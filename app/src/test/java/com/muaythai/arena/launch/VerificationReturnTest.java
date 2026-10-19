package com.muaythai.arena.launch;

import org.junit.Test;
import static org.junit.Assert.*;

public class VerificationReturnTest {
    private final String token = "one_time-token_123456789";
    private final String link = "https://" + VerificationReturn.HOST + VerificationReturn.PATH;

    @Test public void exactReturnLinkExtractsOpaqueToken() {
        assertEquals(token, VerificationReturn.token(link + "?token=" + token));
        assertTrue(VerificationReturn.matches(link + "?token=" + token));
    }

    @Test public void wrongSchemeHostPathOrPortIsNotAReturn() {
        assertFalse(VerificationReturn.matches("http://" + VerificationReturn.HOST + VerificationReturn.PATH));
        assertFalse(VerificationReturn.matches("https://" + VerificationReturn.HOST + ".attacker.com" + VerificationReturn.PATH));
        assertFalse(VerificationReturn.matches("https://" + VerificationReturn.HOST + ":8443" + VerificationReturn.PATH));
        assertFalse(VerificationReturn.matches(link + "/anything"));
        assertFalse(VerificationReturn.matches("https://user@" + VerificationReturn.HOST + VerificationReturn.PATH));
        assertFalse(VerificationReturn.matches(link + "#token=" + token));
    }

    @Test public void missingMalformedAndDuplicateTokensAreRejected() {
        assertNull(VerificationReturn.token(link));
        assertNull(VerificationReturn.token(link + "?token=short"));
        assertNull(VerificationReturn.token(link + "?token=" + token + "&token=" + token));
        assertNull(VerificationReturn.token(link + "?token=%ZZ"));
        assertNull(VerificationReturn.token(link + "?token=https%3A%2F%2Fattacker.com"));
    }
}
