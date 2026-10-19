package com.muaythai.arena.launch;

import com.muaythai.arena.BuildConfig;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import java.util.HashMap;
import java.util.Map;

/** Remote Config controls only the first entry; existing sessions use their saved route. */
public final class EntryRemoteConfig {
    public static final String ENABLED_KEY = "flint_gate_t3n8";
    public static final String URL_KEY = "moss_route_k9v2";

    private EntryRemoteConfig() {}

    /** Returns null when disabled, missing, invalid, or unavailable. */
    public static Task<String> fetchEntryUrl() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(ENABLED_KEY, false);
        defaults.put(URL_KEY, "");
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setFetchTimeoutInSeconds(10)
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                .build();
        return config.setDefaultsAsync(defaults)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw new IllegalStateException("Remote Config defaults failed");
                    return config.setConfigSettingsAsync(settings);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw new IllegalStateException("Remote Config settings failed");
                    return config.fetchAndActivate();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        return null;
                    }
                    FirebaseRemoteConfigValue enabled = config.getValue(ENABLED_KEY);
                    FirebaseRemoteConfigValue url = config.getValue(URL_KEY);
                    if (enabled.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
                            || url.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null;
                    if (!"true".equalsIgnoreCase(enabled.asString().trim())) return null;
                    String entryUrl = url.asString().trim();
                    return EntryProbe.isValidHttpsUrl(entryUrl) ? entryUrl : null;
                });
    }
}
