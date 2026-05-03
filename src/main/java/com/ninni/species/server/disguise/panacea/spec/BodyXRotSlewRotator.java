package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Slew-limits a per-segment X-rotation data accessor and mirrors the smoothed value into
 * {@code seg.xRot}. Caps the per-tick delta at {@code maxPitchDelta}.
 */
public final class BodyXRotSlewRotator implements SegmentRotator {

    private final String accessorKey;
    private final float maxPitchDelta;

    public BodyXRotSlewRotator(String accessorKey, float maxPitchDelta) {
        this.accessorKey = accessorKey;
        this.maxPitchDelta = maxPitchDelta;
    }

    @Override
    public void rotate(SegmentContext ctx) {
        EntityDataAccessor<Float> acc = ctx.reflection().accessorTyped(accessorKey);
        if (acc == null) return;
        Float oldXRot = ctx.scratch(CapturePrevFrameHook.KEY_OLD_X_ROT);
        if (oldXRot == null) return;

        Entity seg = ctx.seg();
        float computed = seg.getEntityData().get(acc);
        float delta = Mth.wrapDegrees(computed - oldXRot);
        if (Math.abs(delta) > maxPitchDelta) delta = Math.signum(delta) * maxPitchDelta;
        float smoothed = Mth.wrapDegrees(oldXRot + delta);

        seg.getEntityData().set(acc, smoothed);
        seg.setXRot(smoothed);
    }
}
