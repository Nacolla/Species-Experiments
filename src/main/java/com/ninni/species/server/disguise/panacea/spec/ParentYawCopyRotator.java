package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Yaw copies {@code parent.yRotO} (the 1-tick delay is the body wobble); pitch approaches
 *  parent at a configurable rate. Living segments also get {@code yBodyRot/yHeadRot}, otherwise
 *  the renderer pins yaw to 0. */
public final class ParentYawCopyRotator implements SegmentRotator {

    private final float pitchApproachRate;

    public ParentYawCopyRotator(float pitchApproachRate) {
        this.pitchApproachRate = pitchApproachRate;
    }

    @Override
    public void rotate(SegmentContext ctx) {
        Entity seg = ctx.seg();
        Entity prev = ctx.prev();

        double dx = prev.getX() - seg.getX();
        double dy = prev.getY() - seg.getY();
        double dz = prev.getZ() - seg.getZ();
        float targetX = -((float) (Mth.atan2(dy, Mth.sqrt((float) (dx * dx + dz * dz))) * Mth.RAD_TO_DEG));
        seg.setXRot(Mth.approachDegrees(seg.getXRot(), targetX, pitchApproachRate));
        seg.setYRot(prev.yRotO);

        LivingEntity ls = ctx.livingSeg();
        if (ls != null) {
            ls.yHeadRot = seg.getYRot();
            ls.yBodyRot = seg.yRotO;
        }
    }
}
