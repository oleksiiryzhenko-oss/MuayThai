package com.muaythai.arena.launch;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.WebSettings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import androidx.core.os.LocaleListCompat;

/** Probe identification headers; WebView uses a separately sanitized copy. */
public final class DeviceHeaders {
    private DeviceHeaders() {}

    public static Map<String, String> collect(Context context) {
        return collect(context, null);
    }

    public static Map<String, String> collect(Context context, String cookieHeader) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", WebSettings.getDefaultUserAgent(context));
        headers.put("X-Requested-With", context.getPackageName());
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        // The probe inspects status/headers only and never renders the compressed body.
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Pragma", "no-cache");
        List<Locale> preferred = new ArrayList<>();
        LocaleListCompat locales = LocaleListCompat.getDefault();
        for (int i = 0; i < locales.size(); i++) {
            if (locales.get(i) != null) preferred.add(locales.get(i));
        }
        headers.put("Accept-Language", acceptLanguage(preferred));
        headers.put("X-Device-Model", Build.MODEL);
        headers.put("X-Device-Brand", Build.BRAND);
        headers.put("X-Device-Manufacturer", Build.MANUFACTURER);
        headers.put("X-Device-Product", Build.PRODUCT);
        headers.put("X-Device-Hardware", Build.HARDWARE);
        headers.put("X-Device-Language", Locale.getDefault().toLanguageTag());
        headers.put("X-Device-Fingerprint", Build.FINGERPRINT);
        headers.put("X-OS-Version", Build.VERSION.RELEASE);
        headers.put("X-SDK-Version", Integer.toString(Build.VERSION.SDK_INT));
        headers.put("X-Timezone", TimeZone.getDefault().getID());
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRealMetrics(metrics);
        headers.put("X-Screen-Density", Integer.toString(metrics.densityDpi));
        headers.put("X-Screen-Resolution", metrics.widthPixels + "x" + metrics.heightPixels);
        headers.put("X-App-Package", context.getPackageName());
        if (cookieHeader != null && !cookieHeader.trim().isEmpty()) headers.put("Cookie", cookieHeader);
        return headers;
    }

    public static Map<String, String> collectForWebView(Context context) {
        return forWebView(collect(context));
    }

    static Map<String, String> forWebView(Map<String, String> probeHeaders) {
        Map<String, String> headers = new LinkedHashMap<>(probeHeaders);
        java.util.Iterator<String> keys = headers.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase("X-Requested-With") || key.equalsIgnoreCase("Accept-Encoding")
                    || key.equalsIgnoreCase("Cookie") || key.equalsIgnoreCase("Accept")
                    || key.equalsIgnoreCase("Pragma")) keys.remove();
        }
        String userAgent = headers.get("User-Agent");
        if (userAgent != null) headers.put("User-Agent", userAgent.replace("; wv)", ")"));
        return headers;
    }

    static String acceptLanguage(List<Locale> locales) {
        if (locales.isEmpty()) return Locale.getDefault().toLanguageTag();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < locales.size(); i++) {
            if (i > 0) value.append(", ");
            value.append(locales.get(i).toLanguageTag());
            if (i > 0) value.append(";q=0.").append(Math.max(1, 10 - i));
        }
        return value.toString();
    }
}
