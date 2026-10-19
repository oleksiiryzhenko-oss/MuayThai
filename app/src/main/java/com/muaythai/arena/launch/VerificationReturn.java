package com.muaythai.arena.launch;

import com.muaythai.arena.BuildConfig;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Proposed backend contract; endpoint and App Link are fixed, never supplied by a link. */
public final class VerificationReturn {
    public static final String HOST = BuildConfig.RETURN_HOST;
    public static final String PATH = "/flint-return-t3n8";
    public static final String ENDPOINT = "https://" + HOST + "/api/flint-check-k9v2";
    private VerificationReturn() {}

    public static boolean matches(String value) {
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                    && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null
                    && PATH.equals(uri.getPath()) && uri.getFragment() == null;
        } catch (Exception ignored) { return false; }
    }

    public static String token(String value) {
        if (!matches(value)) return null;
        try {
            String query = new URI(value).getRawQuery();
            if (query == null) return null;
            String token = null;
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                if (!"token".equals(URLDecoder.decode(parts[0], "UTF-8"))) continue;
                if (token != null || parts.length != 2) return null;
                token = URLDecoder.decode(parts[1], "UTF-8");
            }
            // Tokens are opaque URL-safe ASCII, never URLs or scripts.
            return token != null && token.matches("[A-Za-z0-9._~-]{16,2048}") ? token : null;
        } catch (Exception ignored) { return null; }
    }
}
