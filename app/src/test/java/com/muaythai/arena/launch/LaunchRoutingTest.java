package com.muaythai.arena.launch;

import org.junit.Test;
import static org.junit.Assert.*;

public class LaunchRoutingTest {
    @Test public void brokenReplacementCannotStartAnotherFallback() {
        LaunchRouting route = new LaunchRouting();
        route.moveTo(LaunchRouting.Stage.REMOTE);
        assertTrue(route.beginFallback());
        route.moveTo(LaunchRouting.Stage.REMOTE);
        assertFalse(route.beginFallback());
    }

    @Test public void recreationKeepsBudgetButNewLaunchCanRetry() {
        LaunchRouting recreated = new LaunchRouting();
        recreated.restoreFallback(true);
        assertFalse(recreated.beginFallback());
        assertTrue(new LaunchRouting().beginFallback());
    }

    @Test public void returnLinkInvalidatesPendingProbe() {
        LaunchRouting route = new LaunchRouting();
        int pendingProbe = route.revision();
        route.moveTo(LaunchRouting.Stage.VERIFYING);
        assertFalse(route.accepts(pendingProbe));
        assertFalse(route.beginFallback());
        route.moveTo(LaunchRouting.Stage.FUNCTIONAL);
        assertFalse(route.beginFallback());
    }
}
