package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Default rotator: faces the segment toward {@code prev.position + 3 forward}, with limited
 *  per-tick approach rates. */
public final class IdealApproachRotator implements SegmentRotator {

    private final float yawApproachRate;
    private final float pitchApproachRate;

    public IdealApproachRotator(float yawApproachRate, float pitchApproachRate) {
        this.yawApproachRate = yawApproachRate;
        this.pitchApproachRate = pitchApproachRate;
    }

    public static IdealApproachRotator withDefaults() {
        return new IdealApproachRotator(7.0F, 5.0F);
    }

    @Override
    public void rotate(SegmentContext ctx) {
        Entity seg = ctx.seg();
        Entity prev = ctx.prev();

        // Armor stand wearer: snap segments to the parent's yaw with zero pitch, no approach
        // smoothing. Pairs with IdealApproachPositioner's static-pose path.
        if (ctx.wearer() instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            seg.setYRot(prev.getYRot());
            seg.setXRot(0F);
            return;
        }

        Vec3 frontsBack = prev.position().add(
                new Vec3(0F, 0F, 3F)
                        .xRot((float) -Math.toRadians(prev.getXRot()))
                        .yRot((float) -Math.toRadians(prev.getYRot())));
        double dxr = frontsBack.x - seg.getX();
        double dyr = frontsBack.y - seg.getY();
        double dzr = frontsBack.z - seg.getZ();
        double horiz = Math.sqrt(dxr * dxr + dzr * dzr);
        float targetXRot = Mth.wrapDegrees((float) -(Mth.atan2(dyr, horiz) * (180F / (float) Math.PI)));
        float targetYRot = Mth.wrapDegrees((float) (Mth.atan2(dzr, dxr) * (180F / (float) Math.PI)) - 90F);

        seg.setXRot(Mth.approachDegrees(seg.getXRot(), targetXRot, pitchApproachRate));
        seg.setYRot(Mth.approachDegrees(seg.getYRot(), targetYRot, yawApproachRate));
    }
}
