package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

/** Drives {@code wearer.walkDist} from horizontal delta on the remote client, where vanilla's
 *  {@code aiStep} update is gated off for non-{@code LocalPlayer} entities. Without it,
 *  walkDist-driven chain math (e.g. anaconda slither) freezes. Server tick is left alone. */
public final class WalkDistSyncHook implements BeforeChainTickHook {

    public static final WalkDistSyncHook INSTANCE = new WalkDistSyncHook();
    private WalkDistSyncHook() {}

    @Override
    public void run(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        if (!wearer.level().isClientSide) return;
        if (wearer.isControlledByLocalInstance()) return;
        double dx = wearer.getX() - wearer.xo;
        double dz = wearer.getZ() - wearer.zo;
        wearer.walkDist += (float) Math.sqrt(dx * dx + dz * dz) * 0.6F;
    }
}
