package com.muaythai.arena.launch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared probe/WebView query ID, never saved in routing history. */
public final class EntryAttempt {
    public static final String PARAMETER = "rk7p4";
    public final String id = UUID.randomUUID().toString().replace("-", "");

    public String url(String value) {
        String clean = strip(value);
        int fragment = clean.indexOf('#');
        String base = fragment < 0 ? clean : clean.substring(0, fragment);
        String suffix = fragment < 0 ? "" : clean.substring(fragment);
        return base + (base.contains("?") ? "&" : "?") + PARAMETER + "=" + id + suffix;
    }

    public static String strip(String value) {
        if (value == null) return null;
        try {
            URI uri = new URI(value);
            String query = uri.getRawQuery();
            if (query == null) return value;
            List<String> kept = new ArrayList<>();
            boolean found = false;
            for (String pair : query.split("&", -1)) {
                String key = pair.split("=", 2)[0];
                if (PARAMETER.equals(java.net.URLDecoder.decode(key, "UTF-8"))) found = true;
                else kept.add(pair);
            }
            if (!found) return value;
            int start = value.indexOf('?');
            int fragment = value.indexOf('#', start);
            return value.substring(0, start) + (kept.isEmpty() ? "" : "?" + String.join("&", kept))
                    + (fragment < 0 ? "" : value.substring(fragment));
        } catch (Exception ignored) { return value; }
    }
}
