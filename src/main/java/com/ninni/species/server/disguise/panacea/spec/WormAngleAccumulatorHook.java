package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Drives the head's {@code WORM_ANGLE} from wearer {@code yRotO − yRot} (disguise's own delta
 *  is wiped by aiStep). Snapshots {@code prevWormAngle} so the propagator can read it. */
public final class WormAngleAccumulatorHook implements BeforeChainTickHook {

    private final String getWormAngleKey;
    private final String setWormAngleKey;
    private final String prevWormAngleFieldKey;
    private final float deltaPerTick;
    private final float decayPerTick;
    private final float turnThreshold;
    private final float clamp;

    public WormAngleAccumulatorHook(String getWormAngleKey, String setWormAngleKey,
                                    String prevWormAngleFieldKey,
                                    float deltaPerTick, float decayPerTick,
                                    float turnThreshold, float clamp) {
        this.getWormAngleKey = getWormAngleKey;
        this.setWormAngleKey = setWormAngleKey;
        this.prevWormAngleFieldKey = prevWormAngleFieldKey;
        this.deltaPerTick = deltaPerTick;
        this.decayPerTick = decayPerTick;
        this.turnThreshold = turnThreshold;
        this.clamp = clamp;
    }

    @Override
    public void run(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        Method getWormAngle = refl.method(getWormAngleKey);
        Method setWormAngle = refl.method(setWormAngleKey);
        Field prevWormAngle = refl.field(prevWormAngleFieldKey);
        if (getWormAngle == null || setWormAngle == null || prevWormAngle == null) return;

        try {
            float current = (float) getWormAngle.invoke(disguise);
            prevWormAngle.setFloat(disguise, current);
            float yawDelta = Mth.wrapDegrees(wearer.yRotO - wearer.getYRot());
            float next;
            if (yawDelta > turnThreshold)        next = current + deltaPerTick;
            else if (yawDelta < -turnThreshold)  next = current - deltaPerTick;
            else if (current > 0)                next = Math.max(current - decayPerTick, 0F);
            else if (current < 0)                next = Math.min(current + decayPerTick, 0F);
            else                                 next = current;
            // Cap below ±180° so the model's body.rotateAngleZ doesn't flip past the seam.
            next = Mth.clamp(next, -clamp, clamp);
            setWormAngle.invoke(disguise, next);
        } catch (ReflectiveOperationException ignored) {}
    }
}
