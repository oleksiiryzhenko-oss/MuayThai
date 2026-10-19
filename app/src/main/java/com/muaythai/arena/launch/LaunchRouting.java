package com.muaythai.arena.launch;

/** Per-Activity routing state, also restored during Activity recreation. */
public final class LaunchRouting {
    public enum Stage { CHECKING, REMOTE, FUNCTIONAL, VERIFYING }
    private volatile Stage stage = Stage.CHECKING;
    private boolean fallbackUsed;
    private int revision;

    public Stage stage() { return stage; }
    public int revision() { return revision; }
    public boolean fallbackUsed() { return fallbackUsed; }
    public void restoreFallback(boolean used) { fallbackUsed = used; }
    public void moveTo(Stage next) { stage = next; revision++; }
    public boolean accepts(int expectedRevision) { return revision == expectedRevision; }

    public boolean beginFallback() {
        if (fallbackUsed || stage == Stage.FUNCTIONAL || stage == Stage.VERIFYING) return false;
        fallbackUsed = true;
        moveTo(Stage.CHECKING);
        return true;
    }
}
