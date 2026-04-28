package com.ninni.species.server.disguise.cosmetic;

import com.ninni.species.api.disguise.DisguiseCosmetics;

/**
 * No-op fallback returned by {@link DisguiseCosmeticRegistry#get} when no cosmetic is
 * registered for the disguise type. Provides a shared non-null instance so callers
 * never need null-checks.
 */
public final class DefaultDisguiseCosmetics implements DisguiseCosmetics {

    public static final DefaultDisguiseCosmetics INSTANCE = new DefaultDisguiseCosmetics();

    private DefaultDisguiseCosmetics() {}
}
