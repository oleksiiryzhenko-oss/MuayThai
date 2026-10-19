package com.muaythai.arena.launch;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerConversionListener;
import com.muaythai.arena.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public final class AppsFlyerHandoff {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Runnable> waiting = new ArrayList<>();
    private static volatile Map<String, String> attribution = java.util.Collections.emptyMap();
    private static volatile RedirectAugmenter augmenter;
    private static boolean configReady, attributionReady, started;
    private AppsFlyerHandoff() {}

    public static void initialize(Application application) {
        if (started) return;
        started = true;
        String key = BuildConfig.APPSFLYER_DEV_KEY.trim();
        if (key.isEmpty()) { configReady = attributionReady = true; return; }
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        config.ensureInitialized().addOnCompleteListener(task -> {
            refreshConfig(config);
            config.fetchAndActivate().addOnCompleteListener(fetch -> {
                refreshConfig(config);
                configReady = true;
                releaseReady();
            });
        });
        AppsFlyerLib sdk = AppsFlyerLib.getInstance();
        sdk.setDebugLog(false);
        sdk.init(key, new AppsFlyerConversionListener() {
            @Override public void onConversionDataSuccess(Map<String, Object> data) {
                Map<String, String> values = new HashMap<>();
                String id = sdk.getAppsFlyerUID(application);
                if (id != null && !id.isEmpty()) values.put("af_id", id);
                if (data != null) {
                    for (String field : new String[]{"af_status", "media_source", "campaign"}) {
                        Object value = data.get(field);
                        if (value != null && !value.toString().trim().isEmpty()) values.put(field, value.toString());
                    }
                    if ("Organic".equals(values.get("af_status"))) values.put("media_source", "organic");
                }
                attribution = java.util.Collections.unmodifiableMap(values);
                MAIN.post(() -> { attributionReady = true; releaseReady(); });
            }
            @Override public void onConversionDataFail(String error) {
                String id = sdk.getAppsFlyerUID(application);
                if (id != null && !id.isEmpty()) attribution = java.util.Collections.singletonMap("af_id", id);
                MAIN.post(() -> { attributionReady = true; releaseReady(); });
            }
            @Override public void onAppOpenAttribution(Map<String, String> data) { }
            @Override public void onAttributionFailure(String error) { }
        }, application);
        sdk.start(application);
    }
    private static void refreshConfig(FirebaseRemoteConfig config) {
        augmenter = RedirectAugmenter.parse(config.getString(BuildConfig.AF_HANDOFF_CONFIG_KEY));
    }

    private static void releaseReady() {
        if (!configReady || !attributionReady) return;
        for (Runnable callback : new ArrayList<>(waiting)) callback.run();
    }
    /** Ready or timeout: each navigation is released exactly once. */
    public static void whenReady(Runnable callback) {
        if (configReady && attributionReady) { callback.run(); return; }
        Runnable once = new Runnable() {
            boolean called;
            @Override public void run() {
                if (called) return;
                called = true;
                waiting.remove(this);
                MAIN.removeCallbacks(this);
                callback.run();
            }
        };
        waiting.add(once);
        MAIN.postDelayed(once, 6000);
    }
    public static String rewrite(String url) {
        if (BuildConfig.APPSFLYER_DEV_KEY.isEmpty()) return url;
        RedirectAugmenter current = augmenter;
        return current == null ? url : current.rewrite(url, attribution);
    }
}
