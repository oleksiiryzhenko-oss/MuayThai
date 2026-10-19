package com.muaythai.arena.launch;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.net.ssl.HttpsURLConnection;

/** Performs a non-following HTTPS GET before deciding whether to enter the WebView. */
public final class EntryProbe {
    public enum Decision { ALLOW_FUNCTIONALITY, OPEN_WEBVIEW }
    public static final class Result {
        public final Decision decision;
        public final List<String> cookies;

        private Result(Decision decision, List<String> cookies) {
            this.decision = decision;
            this.cookies = Collections.unmodifiableList(new ArrayList<>(cookies));
        }
    }
    private EntryProbe() {}



    public static boolean isValidHttpsUrl(String value) {
        try {
            java.net.URI uri = new java.net.URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && !uri.getHost().isEmpty() && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535));
        } catch (Exception ignored) { return false; }
    }

    public static Decision check(String value, Map<String, String> headers) {
        return check(value, headers, url -> (HttpsURLConnection) url.openConnection());
    }

    public static Result checkWithCookies(String value, Map<String, String> headers) {
        return checkWithCookies(value, headers, url -> (HttpsURLConnection) url.openConnection());
    }

    interface ConnectionFactory {
        HttpsURLConnection open(URL url) throws java.io.IOException;
    }

    static Decision check(String value, Map<String, String> headers, ConnectionFactory factory) {
        return checkWithCookies(value, headers, factory).decision;
    }

    static Result checkWithCookies(String value, Map<String, String> headers, ConnectionFactory factory) {
        List<String> cookies = new ArrayList<>();
        Decision decision = checkResponse(value, headers, factory, cookies);
        return new Result(decision, cookies);
    }

    private static Decision checkResponse(String value, Map<String, String> headers,
                                          ConnectionFactory factory, List<String> cookies) {
        if (!isValidHttpsUrl(value)) return Decision.ALLOW_FUNCTIONALITY;
        HttpsURLConnection connection = null;
        try {
            connection = factory.open(new URL(value));
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) return Decision.ALLOW_FUNCTIONALITY;
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                if (location == null || location.trim().isEmpty()) return Decision.ALLOW_FUNCTIONALITY;
                URL destination = new URL(new URL(value), location);
                if (isValidHttpsUrl(destination.toString())) {
                    // Keep each Set-Cookie separate: Expires attributes contain commas.
                    for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
                        if (field.getKey() != null && field.getKey().equalsIgnoreCase("Set-Cookie")
                                && field.getValue() != null) {
                            for (String cookie : field.getValue()) {
                                if (cookie != null && !cookie.trim().isEmpty()) cookies.add(cookie);
                            }
                        }
                    }
                    return Decision.OPEN_WEBVIEW;
                }
            }
        } catch (Exception ignored) {
            // Site and network errors use the explicit fail-open rule in the spec.
        } finally {
            if (connection != null) connection.disconnect();
        }
        return Decision.ALLOW_FUNCTIONALITY;
    }
}
