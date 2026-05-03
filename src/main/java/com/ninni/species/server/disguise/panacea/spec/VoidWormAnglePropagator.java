package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

/** Cascades the WORM_ANGLE wave one segment per tick: each segment saves into
 *  {@code prevWormAngle}, then inherits its parent's previous-frame value (head's for index 0).
 *  Without this, {@code body.rotateAngleZ} stays at default and the chain renders rigid. */
public final class VoidWormAnglePropagator implements StatePropagator {

    public static final VoidWormAnglePropagator INSTANCE = new VoidWormAnglePropagator();
    private VoidWormAnglePropagator() {}

    @Override
    public void propagate(SegmentContext ctx) {
        ReflectionPlan refl = ctx.reflection();
        EntityDataAccessor<Float> wormAngleAcc = refl.accessorTyped("WORM_ANGLE");
        Field partPrevWormAngle = refl.field("partPrevWormAngle");
        if (wormAngleAcc == null || partPrevWormAngle == null) return;

        Entity seg = ctx.seg();
        Entity prev = ctx.prev();
        try {
            partPrevWormAngle.setFloat(seg, seg.getEntityData().get(wormAngleAcc));
            float parentPrev;
            if (ctx.index() == 0) {
                Field headPrevWormAngle = refl.field("headPrevWormAngle");
                parentPrev = (headPrevWormAngle != null) ? headPrevWormAngle.getFloat(ctx.disguise()) : 0F;
            } else {
                parentPrev = partPrevWormAngle.getFloat(prev);
            }
            seg.getEntityData().set(wormAngleAcc, parentPrev);
        } catch (IllegalAccessException ignored) {}
    }
}
