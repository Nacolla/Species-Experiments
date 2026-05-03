package com.ninni.species.server.disguise.util;

import com.ninni.species.server.disguise.DisguiseLogging;

/** Try/catch wrappers around {@code DisguiseBehavior}/{@code DisguiseCosmetics} hook calls,
 *  routing thrown errors through {@link DisguiseLogging#rateLimited}. Centralises the boilerplate
 *  scattered across the tick + render pipelines. */
public final class BehaviorHooks {

    private BehaviorHooks() {}

    /** Invoke {@code action}; on any throwable, log via rate-limited dedup keyed by {@code label}. */
    public static void run(String label, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            DisguiseLogging.rateLimited(label, t);
        }
    }

    /** Invoke {@code action}; swallow any throwable silently. Use only for pure visual hooks
     *  where logging would spam the console for a recoverable failure. */
    public static void runSilently(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {}
    }
}
