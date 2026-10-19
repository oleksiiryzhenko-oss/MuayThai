package com.muaythai.arena.launch;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.annotation.RequiresApi;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** WebView client that adds device headers only to the entry site's traffic. */
public final class ManagedWebViewClient extends WebViewClient {
    private final EntryActivity activity;
    private final WebViewSession session;
    private final Map<String, String> deviceHeaders;
    private String mainPageUrl;
    private final java.util.Set<String> failedPages = new java.util.HashSet<>();

    ManagedWebViewClient(EntryActivity activity, WebViewSession session,
                         Map<String, String> deviceHeaders) {
        this.activity = activity;
        this.session = session;
        this.deviceHeaders = deviceHeaders;
    }

    private boolean isRemotePage(String url) { return EntryProbe.isValidHttpsUrl(url); }

    private String entryHost() {
        try { return new URL(session.initialEntryUrl()).getHost(); }
        catch (Exception ignored) { return null; }
    }

    private boolean isEntryHost(Uri uri) {
        String host = entryHost();
        return host != null && uri.getHost() != null && host.equalsIgnoreCase(uri.getHost());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (VerificationReturn.matches(uri.toString())) {
            if (request.isForMainFrame()) activity.handleReturnLink(uri);
            return true;
        }
        if (request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
            String rewritten = AppsFlyerHandoff.rewrite(uri.toString());
            if (!rewritten.equals(uri.toString())) {
                activity.loadRemotePage(rewritten);
                return true;
            }
        }
        if ("https".equalsIgnoreCase(uri.getScheme()) && isEntryHost(uri)
                && request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
            activity.loadRemotePage(uri.toString());
            return true;
        }
        if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (VerificationReturn.matches(url)) { activity.handleReturnLink(Uri.parse(url)); return true; }
        if ("https".equalsIgnoreCase(Uri.parse(url).getScheme()) && isEntryHost(Uri.parse(url))) {
            activity.loadRemotePage(url);
            return true;
        }
        if (url.startsWith("https://") || url.startsWith("http://")) return false;
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mainPageUrl = url;
        failedPages.remove(url);
        activity.onRemotePageStarted();
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (!isRemotePage(uri.toString())) return null;
        if (request.isForMainFrame()) return null;
        return interceptEntryResource(request);
    }

    /** Shared with ServiceWorkerClient; never substitutes a main document or a POST body. */
    WebResourceResponse interceptEntryResource(WebResourceRequest request) {
        if (request.isForMainFrame()) return null;
        Uri uri = request.getUrl();
        if (!"https".equalsIgnoreCase(uri.getScheme())) return null;
        if (!isEntryHost(uri)) return null;
        String method = request.getMethod();
        if (method == null || !(method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD"))) return null;

        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(uri.toString()).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            for (Map.Entry<String, String> header : request.getRequestHeaders().entrySet()) {
                if (!header.getKey().equalsIgnoreCase("Accept-Encoding")
) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            // Device values take precedence over WebView's original header values.
            for (Map.Entry<String, String> header : activity.headersFor(uri.toString()).entrySet()) {
                if (!header.getKey().equalsIgnoreCase("Accept-Encoding")) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            String cookie = CookieManager.getInstance().getCookie(uri.toString());
            if (cookie != null) connection.setRequestProperty("Cookie", cookie);

            int status = connection.getResponseCode();
            // WebResourceResponse cannot represent redirects. Let Chromium handle them.
            if (status >= 300 && status < 400) { connection.disconnect(); return null; }
            String contentEncoding = connection.getContentEncoding();
            if (contentEncoding != null && !contentEncoding.trim().isEmpty()
                    && !"identity".equalsIgnoreCase(contentEncoding)) {
                connection.disconnect();
                return null;
            }
            Map<String, String> responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> item : connection.getHeaderFields().entrySet()) {
                if (item.getKey() != null && item.getValue() != null && !item.getValue().isEmpty()) {
                    String value = android.text.TextUtils.join(",", item.getValue());
                    responseHeaders.put(item.getKey(), value);
                    if (item.getKey().equalsIgnoreCase("Set-Cookie")) {
                        for (String setCookie : item.getValue()) CookieManager.getInstance().setCookie(uri.toString(), setCookie);
                    }
                }
            }
            String contentType = connection.getContentType();
            String mime = "text/plain";
            String charset = "UTF-8";
            if (contentType != null) {
                String[] parts = contentType.split(";", 2);
                mime = parts[0].trim();
                if (parts.length == 2 && parts[1].toLowerCase(Locale.ROOT).contains("charset=")) {
                    charset = parts[1].substring(parts[1].toLowerCase(Locale.ROOT).indexOf("charset=") + 8).trim();
                }
            }
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) input = new ByteArrayInputStream(new byte[0]);
            final HttpsURLConnection responseConnection = connection;
            InputStream stream = new FilterInputStream(input) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { responseConnection.disconnect(); }
                }
            };
            String reason = connection.getResponseMessage();
            if (reason == null || reason.trim().isEmpty()) reason = "HTTP response";
            return new WebResourceResponse(mime, charset, status, reason, responseHeaders, stream);
        } catch (Exception ignored) {
            if (connection != null) connection.disconnect();
            return null;
        }
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        activity.onRemotePageVisible();
        saveWorkingPage(url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        saveWorkingPage(url);
    }

    private void saveWorkingPage(String url) {
        if (activity.isShowingRemotePage() && session.hasOpenedWebView()
                && isRemotePage(url) && !VerificationReturn.matches(url) && !failedPages.contains(url)
                && java.util.Objects.equals(url, mainPageUrl)) {
            session.recordWorkingPage(EntryAttempt.strip(url));
            CookieManager.getInstance().flush();
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        if (request.isForMainFrame() && isRemotePage(request.getUrl().toString())) {
            failMainPage(request.getUrl().toString());
        }
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
        super.onReceivedHttpError(view, request, response);
        if (request.isForMainFrame() && isRemotePage(request.getUrl().toString())
                && response.getStatusCode() >= 400) failMainPage(request.getUrl().toString());
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
        handler.cancel();
        // TLS callbacks have no isForMainFrame; only fail the route for the active document.
        if (isRemotePage(error.getUrl()) && (java.util.Objects.equals(error.getUrl(), mainPageUrl)
                || java.util.Objects.equals(error.getUrl(), view.getUrl()))) failMainPage(error.getUrl());
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.O)
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        if (detail.didCrash()) {
            activity.restartAfterRendererCrash(view);
        } else {
            activity.finish();
        }
        return true;
    }

    private void failMainPage(String url) {
        failedPages.add(url);
        if (java.util.Objects.equals(url, mainPageUrl)
                || java.util.Objects.equals(url, activity.currentWebUrl())) {
            activity.onRemotePageFailed();
        }
    }
}
