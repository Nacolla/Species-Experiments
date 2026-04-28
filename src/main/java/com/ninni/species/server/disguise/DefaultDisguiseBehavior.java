package com.ninni.species.server.disguise;

import com.ninni.species.api.disguise.DisguiseBehavior;

/**
 * Conservative fallback behavior for disguise types that don't have explicit
 * overrides. All hooks are no-ops; rotation/xRot rules use the interface defaults.
 */
public final class DefaultDisguiseBehavior implements DisguiseBehavior {
    public static final DefaultDisguiseBehavior INSTANCE = new DefaultDisguiseBehavior();

    private DefaultDisguiseBehavior() {}
}
