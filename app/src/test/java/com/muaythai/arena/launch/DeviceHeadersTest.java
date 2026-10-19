package com.muaythai.arena.launch;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceHeadersTest {
    @Test public void webViewSanitizesProbeIdentificationWithoutChangingProbeHeaders() {
        Map<String, String> probe = Map.of(
                "User-Agent", "Mozilla/5.0 (Linux; Android 16; wv) Chrome/140",
                "x-requested-with", "com.muaythai.arena",
                "Accept-Encoding", "gzip, deflate, br",
                "Cookie", "session=one",
                "Accept", "text/html",
                "X-Device-Model", "phone");
        Map<String, String> web = DeviceHeaders.forWebView(probe);
        assertEquals("Mozilla/5.0 (Linux; Android 16) Chrome/140", web.get("User-Agent"));
        assertFalse(web.containsKey("x-requested-with"));
        assertFalse(web.containsKey("Accept-Encoding"));
        assertFalse(web.containsKey("Cookie"));
        assertFalse(web.containsKey("Accept"));
        assertEquals("phone", web.get("X-Device-Model"));
        assertTrue(probe.get("User-Agent").contains("; wv)"));
        assertEquals("session=one", probe.get("Cookie"));
    }

    @Test public void languagePreferencesUseBcp47AndDecreasingQuality() {
        assertEquals("uk-UA, en-US;q=0.9, pt-PT;q=0.8", DeviceHeaders.acceptLanguage(List.of(
                Locale.forLanguageTag("uk-UA"), Locale.US, Locale.forLanguageTag("pt-PT"))));
        assertEquals("en-US", DeviceHeaders.acceptLanguage(List.of(Locale.US)));
    }
}
