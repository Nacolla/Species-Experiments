package com.ninni.species.api.disguise;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Wearer state at special-action keybind press. {@link #SWIMMING} is active swim;
 * {@link #UNDERWATER} is the broader still-in-water (wading, treading) case.
 */
public enum ActionContext {
    GROUND,
    FLYING,
    SWIMMING,
    UNDERWATER;

    /** Resolves the wearer's current context. Swimming → underwater → flying → ground. */
    public static ActionContext of(LivingEntity wearer) {
        if (wearer == null) return GROUND;
        if (wearer.isSwimming()) return SWIMMING;
        if (wearer.isInWater()) return UNDERWATER;
        if (wearer.isFallFlying()) return FLYING;
        if (wearer instanceof Player p && p.getAbilities().flying) return FLYING;
        return GROUND;
    }
}
