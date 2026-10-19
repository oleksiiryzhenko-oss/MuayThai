package com.muaythai.arena.launch;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;

public class RedirectAugmenterTest {
    private final String config = "{\"enabled\":true,\"target_host\":\"tracker.example.com\",\"target_path\":\"/track\",\"query\":{\"af_uid\":\"af_id\",\"campaign\":\"campaign\"}}";
    private final Map<String, String> values = Map.of("af_id", "install-id", "campaign", "Summer & sport");
    @Test public void preservesOriginalQueryAndFragment() {
        String out = RedirectAugmenter.parse(config).rewrite("https://tracker.example.com/track?key=a%2Bb&token=42#section", values);
        assertTrue(out.startsWith("https://tracker.example.com/track?key=a%2Bb&token=42&"));
        assertTrue(out.contains("af_uid=install-id"));
        assertTrue(out.contains("campaign=Summer+%26+sport"));
        assertTrue(out.endsWith("#section"));
    }
    @Test public void isIdempotentAndKeepsExistingValues() {
        RedirectAugmenter a = RedirectAugmenter.parse(config);
        String out = a.rewrite("https://tracker.example.com/track?key=x&af_uid=original", values);
        assertTrue(out.contains("af_uid=original"));
        assertFalse(out.contains("af_uid=install-id"));
        assertEquals(out, a.rewrite(out, values));
    }
    @Test public void neverTouchesOtherDestinations() {
        RedirectAugmenter a = RedirectAugmenter.parse(config);
        for (String url : new String[]{"http://tracker.example.com/track", "https://tracker.example.com.evil/track", "https://tracker.example.com/track/other", "https://user@tracker.example.com/track", "https://tracker.example.com:8443/track"}) assertEquals(url, a.rewrite(url, values));
    }
    @Test public void missingDataDoesNotInventCampaign() {
        String out = RedirectAugmenter.parse(config).rewrite("https://tracker.example.com/track?key=x", Map.of("af_id", "id"));
        assertEquals("https://tracker.example.com/track?key=x&af_uid=id", out);
    }
    @Test public void disabledOrMalformedConfigIsRejected() {
        assertNull(RedirectAugmenter.parse(config.replace("true", "false")));
        assertNull(RedirectAugmenter.parse("not-json"));
        assertNull(RedirectAugmenter.parse(config.replace("tracker.example.com", "")));
        assertNull(RedirectAugmenter.parse(config.replace("/track", "/track?key=x")));
        assertNull(RedirectAugmenter.parse(config.replace("af_uid", "key")));
    }
    @Test public void encodedExistingKeysAreNotDuplicated() {
        String out = RedirectAugmenter.parse(config).rewrite("https://tracker.example.com/track?af%5Fuid=original", Map.of("af_id", "id"));
        assertEquals("https://tracker.example.com/track?af%5Fuid=original", out);
    }
}
