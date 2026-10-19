package com.muaythai.arena.launch;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.LinkedHashMap;
import org.json.JSONObject;

/** Only an exact HTTPS tracker host/path may receive attribution. */
public final class RedirectAugmenter {
    private final String host, path;
    private final Map<String, String> query;
    private RedirectAugmenter(String host, String path, Map<String, String> query) {
        this.host = host; this.path = path; this.query = query;
    }
    public static RedirectAugmenter parse(String json) {
        try {
            JSONObject data = new JSONObject(json);
            if (!Boolean.TRUE.equals(data.opt("enabled"))) return null;
            String host = data.getString("target_host");
            String path = data.getString("target_path");
            URI target = new URI("https://" + host + path);
            if (host.isEmpty() || !host.equalsIgnoreCase(target.getHost()) || target.getUserInfo() != null
                    || target.getPort() != -1 || !path.startsWith("/")
                    || !path.equals(target.getRawPath()) || target.getRawQuery() != null || target.getRawFragment() != null) return null;
            JSONObject mapping = data.getJSONObject("query");
            Map<String, String> query = new LinkedHashMap<>();
            java.util.Iterator<String> keys = mapping.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String field = mapping.getString(key);
                if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}") || "key".equalsIgnoreCase(key)
                        || !(field.equals("af_id") || field.equals("af_status") || field.equals("media_source") || field.equals("campaign"))) return null;
                query.put(key, field);
            }
            return new RedirectAugmenter(host, path, query);
        } catch (Exception ignored) { return null; }
    }
    public String rewrite(String url, Map<String, String> values) {
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !host.equalsIgnoreCase(uri.getHost())
                    || !path.equals(uri.getRawPath()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) return url;
            String existing = uri.getRawQuery();
            StringBuilder additions = new StringBuilder();
            for (Map.Entry<String, String> entry : query.entrySet()) {
                boolean present = false;
                if (existing != null) for (String item : existing.split("&")) {
                    String key = item.split("=", 2)[0];
                    if (java.net.URLDecoder.decode(key, "UTF-8").equals(entry.getKey())) present = true;
                }
                String value = values.get(entry.getValue());
                if (present || value == null || value.isEmpty()) continue;
                if (additions.length() > 0) additions.append('&');
                additions.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode(value, "UTF-8"));
            }
            if (additions.length() == 0) return url;
            int hash = url.indexOf('#');
            String prefix = hash < 0 ? url : url.substring(0, hash);
            String suffix = hash < 0 ? "" : url.substring(hash);
            String separator = existing == null ? "?" : prefix.endsWith("?") || prefix.endsWith("&") ? "" : "&";
            return prefix + separator + additions + suffix;
        } catch (Exception ignored) { return url; }
    }
}
