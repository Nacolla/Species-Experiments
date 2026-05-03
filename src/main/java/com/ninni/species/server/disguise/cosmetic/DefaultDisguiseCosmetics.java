package com.ninni.species.server.disguise.cosmetic;

import com.ninni.species.api.disguise.DisguiseCosmetics;

/** No-op fallback for {@link DisguiseCosmeticRegistry#get} so callers never null-check. */
public final class DefaultDisguiseCosmetics implements DisguiseCosmetics {

    public static final DefaultDisguiseCosmetics INSTANCE = new DefaultDisguiseCosmetics();

    private DefaultDisguiseCosmetics() {}
}
