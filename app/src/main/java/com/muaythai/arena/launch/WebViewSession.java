package com.muaythai.arena.launch;

import android.content.Context;
import android.content.SharedPreferences;

/** Durable routing state for users who have entered the verification WebView. */
public final class WebViewSession {
    private final SharedPreferences preferences;

    public WebViewSession(Context context) {
        this(context, "flint_state_t3n8");
    }

    WebViewSession(Context context, String preferenceName) {
        preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
    }


    public boolean hasOpenedWebView() { return preferences.getBoolean("seen_r4j8", false); }
    public String initialEntryUrl() { return preferences.getString("origin_l7c2", null); }
    public String lastWebViewUrl() { return preferences.getString("page_u9b5", null); }
    public boolean lastUrlWorks() { return preferences.getBoolean("healthy_s2e6", false); }

    public void recordFirstRedirect(String entryUrl) {
        if (hasOpenedWebView()) return;
        preferences.edit().putBoolean("seen_r4j8", true)
                .putString("origin_l7c2", entryUrl).putBoolean("healthy_s2e6", false).commit();
    }

    public void recordWorkingPage(String url) {
        preferences.edit().putBoolean("seen_r4j8", true)
                .putString("page_u9b5", url).putBoolean("healthy_s2e6", true).apply();
    }

    public void markLastUrlBroken() { preferences.edit().putBoolean("healthy_s2e6", false).apply(); }

    public boolean wasTokenAccepted(String token) {
        return tokenHash(token).equals(preferences.getString("receipt_c8v3", ""));
    }

    public void recordAcceptedToken(String token) {
        preferences.edit().putString("receipt_c8v3", tokenHash(token)).commit();
    }

    private static String tokenHash(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
