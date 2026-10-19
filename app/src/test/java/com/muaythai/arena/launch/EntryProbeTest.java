package com.muaythai.arena.launch;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public class EntryProbeTest {
    private static final String ENTRY = "https://not-configured.invalid/";

    @Test public void allows200ErrorsAnd304WithoutOpeningWebView() throws Exception {
        for (int status : new int[]{200, 204, 304, 400, 404, 500}) {
            StubConnection connection = new StubConnection(status, "https://other.example/");
            assertEquals(EntryProbe.Decision.ALLOW_FUNCTIONALITY,
                    EntryProbe.check(ENTRY, Map.of(), url -> connection));
            assertTrue(connection.disconnected);
        }
        assertEquals(EntryProbe.Decision.ALLOW_FUNCTIONALITY,
                EntryProbe.check(ENTRY, Map.of(), url -> { throw new IOException("offline"); }));
    }

    @Test public void acceptsAllSpecifiedRedirectsAndRelativeLocations() throws Exception {
        for (int status : new int[]{301, 302, 303, 307, 308}) {
            for (String destination : new String[]{"https://another.example/verify", "/verify", "//other.example/"}) {
                StubConnection connection = new StubConnection(status, destination);
                assertEquals(EntryProbe.Decision.OPEN_WEBVIEW,
                        EntryProbe.check(ENTRY, Map.of(), url -> connection));
            }
        }
    }

    @Test public void invalidLocationsDoNotOpenWebView() throws Exception {
        for (String destination : new String[]{"", " ", "http://other.example/", "https://", "https://bad host/", "javascript:alert(1)"}) {
            StubConnection connection = new StubConnection(302, destination);
            assertEquals(EntryProbe.Decision.ALLOW_FUNCTIONALITY,
                    EntryProbe.check(ENTRY, Map.of(), url -> connection));
        }
    }

    @Test public void probeUsesGetHeadersAndNeverFollowsRedirects() throws Exception {
        EntryAttempt attempt = new EntryAttempt();
        Map<String, String> headers = Map.of("X-App-Package", "com.muaythai.arena", "X-Requested-With", "com.muaythai.arena", "User-Agent", "Android; wv)");
        StubConnection connection = new StubConnection(302, "/verify");
        EntryProbe.check(ENTRY, headers, url -> connection);
        assertFalse(connection.getInstanceFollowRedirects());
        assertEquals("GET", connection.getRequestMethod());
        assertEquals("com.muaythai.arena", connection.getRequestProperty("X-Requested-With"));
        assertEquals("Android; wv)", connection.getRequestProperty("User-Agent"));
        assertEquals("com.muaythai.arena", connection.getRequestProperty("X-App-Package"));
        assertFalse(connection.getUseCaches());
        assertNotEquals(attempt.id, new EntryAttempt().id);
    }

    @Test public void invalidInitialUrlNeverOpensAConnection() {
        for (String url : new String[]{"not-configured.invalid", "http://not-configured.invalid/", "https://bad host/", "https://user@not-configured.invalid/", "https://not-configured.invalid:99999/"}) {
            assertEquals(EntryProbe.Decision.ALLOW_FUNCTIONALITY,
                    EntryProbe.check(url, Map.of(), ignored -> { fail("Must not connect"); return null; }));
        }
    }

    @Test public void redirectPreservesSeparateCookiesAndSendsExistingSession() throws Exception {
        String expiring = "session=one; Expires=Wed, 09 Jun 2027 10:18:14 GMT; Secure; HttpOnly; Path=/";
        String second = "attempt=two; Secure; Path=/";
        StubConnection connection = new StubConnection(302, "/verify") {
            @Override public Map<String, List<String>> getHeaderFields() {
                return Map.of("set-cookie", List.of(expiring, second));
            }
        };
        EntryProbe.Result result = EntryProbe.checkWithCookies(ENTRY,
                Map.of("Cookie", "previous=session"), url -> connection);
        assertEquals(EntryProbe.Decision.OPEN_WEBVIEW, result.decision);
        assertEquals(List.of(expiring, second), result.cookies);
        assertEquals("previous=session", connection.getRequestProperty("Cookie"));
        assertTrue(connection.disconnected);
    }

    @Test public void invalidRedirectDoesNotTransferCookies() throws Exception {
        StubConnection connection = new StubConnection(302, "http://other.example/") {
            @Override public Map<String, List<String>> getHeaderFields() {
                return Map.of("Set-Cookie", List.of("session=one"));
            }
        };
        EntryProbe.Result result = EntryProbe.checkWithCookies(ENTRY, Map.of(), url -> connection);
        assertEquals(EntryProbe.Decision.ALLOW_FUNCTIONALITY, result.decision);
        assertTrue(result.cookies.isEmpty());
    }

    private static class StubConnection extends HttpsURLConnection {
        final int status;
        final String location;
        boolean disconnected;
        StubConnection(int status, String location) throws Exception {
            super(new URL(ENTRY)); this.status = status; this.location = location;
        }
        @Override public int getResponseCode() { return status; }
        @Override public String getHeaderField(String name) { return "Location".equals(name) ? location : null; }
        @Override public Map<String, List<String>> getHeaderFields() { return Map.of(); }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
        @Override public String getCipherSuite() { return "test"; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() { return null; }
    }
}
