package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** Per-wearer slither phase advance with a floor on {@code walkDist} delta — without it, crouched
 *  wearers stall the S-wave. Frozen when not horizontally moving. */
public final class AnacondaSlitherPhaseHook implements BeforeChainTickHook {

    private static final float MOVING_EPSILON = 1.0E-4F;
    private static final float WALKDIST_SCALE = 0.6F;

    private final StateSlot<UUID, float[]> slot;
    private final float walkDistMultiplier;
    private final float floorWhenMoving;

    public AnacondaSlitherPhaseHook(StateSlot<UUID, float[]> slot,
                                    float walkDistMultiplier, float floorWhenMoving) {
        this.slot = slot;
        this.walkDistMultiplier = walkDistMultiplier;
        this.floorWhenMoving = floorWhenMoving;
    }

    @Override
    public void run(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        double dx = wearer.getX() - wearer.xo;
        double dz = wearer.getZ() - wearer.zo;
        float walkDistDelta = (float) Math.sqrt(dx * dx + dz * dz) * WALKDIST_SCALE;

        float[] phase = slot.getOrCreate(wearer.getUUID());
        if (walkDistDelta > MOVING_EPSILON) {
            phase[0] += Math.max(walkDistDelta * walkDistMultiplier, floorWhenMoving);
        }
    }
}
