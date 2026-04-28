package com.ninni.species.server.disguise;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limited logging for the disguise pipeline: logs occurrence #1 per site-key, then every {@link #INTERVAL}.
 * Site keys are caller-provided (e.g. {@code "behavior.preTick"}); ConcurrentHashMap handles client+server in singleplayer.
 */
public final class DisguiseLogging {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Log every Nth occurrence. 1024 ≈ once per ~50s at 20 tps, ~17s at 60 fps. */
    private static final int INTERVAL = 1024;

    private static final ConcurrentHashMap<String, Integer> COUNTERS = new ConcurrentHashMap<>();

    private DisguiseLogging() {}

    /**
     * Log a swallowed exception at the named site, rate-limited.
     * @param siteKey throwing site identifier; same key shares a counter
     * @param t the exception
     */
    public static void rateLimited(String siteKey, Throwable t) {
        int count = COUNTERS.merge(siteKey, 1, Integer::sum);
        if (count == 1) {
            LOGGER.error("[Species] Disguise pipeline swallowed exception at {} (further occurrences will be rate-limited)", siteKey, t);
        } else if (count % INTERVAL == 0) {
            LOGGER.error("[Species] Disguise pipeline swallowed exception at {} (occurrence #{})", siteKey, count, t);
        }
    }
}
