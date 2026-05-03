package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Pins CHILD_UUID and both portal cooldowns ({@code makePortalCooldown},
 * {@code makeIdlePortalCooldown}) to suppress segment-spawn and portal-spawn loops.
 */
public final class VoidWormServerGuard implements ServerGuard {

    public static final VoidWormServerGuard INSTANCE = new VoidWormServerGuard();
    private VoidWormServerGuard() {}

    @Override
    public void apply(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        Method setHeadChildId = refl.method("setHeadChildId");
        if (setHeadChildId != null) {
            try { setHeadChildId.invoke(disguise, wearer.getUUID()); } catch (ReflectiveOperationException ignored) {}
        }
        try {
            Field stuck = refl.field("makePortalCooldown");
            if (stuck != null) stuck.setInt(disguise, Integer.MAX_VALUE);
            Field idle = refl.field("makeIdlePortalCooldown");
            if (idle != null) idle.setInt(disguise, Integer.MAX_VALUE);
        } catch (IllegalAccessException ignored) {}
    }
}
