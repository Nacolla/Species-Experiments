package com.ninni.species.server.disguise.panacea.spec;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntToDoubleFunction;

/** Default chain positioner: parent.position + sideSway + backStretch with speed-limited approach,
 *  optional Y-to-surface clamp. Used by chains without native multipart math. */
public final class IdealApproachPositioner implements SegmentPositioner {

    private final IntToDoubleFunction backStretchFn;
    private final double sideSwayBase;
    private final double sideSwayPerIndex;
    private final double maxDistFromPrev;
    private final boolean clampToSurface;

    public IdealApproachPositioner(IntToDoubleFunction backStretchFn,
                                   double sideSwayBase, double sideSwayPerIndex,
                                   double maxDistFromPrev, boolean clampToSurface) {
        this.backStretchFn = backStretchFn;
        this.sideSwayBase = sideSwayBase;
        this.sideSwayPerIndex = sideSwayPerIndex;
        this.maxDistFromPrev = maxDistFromPrev;
        this.clampToSurface = clampToSurface;
    }

    /** Defaults matching {@code SegmentChainManager}'s base implementation. */
    public static IdealApproachPositioner withDefaults(IntToDoubleFunction backStretchFn, boolean clampToSurface) {
        return new IdealApproachPositioner(backStretchFn, 0.5, 0.05, 2.0, clampToSurface);
    }

    @Override
    public void position(SegmentContext ctx) {
        Entity seg = ctx.seg();
        Entity prev = ctx.prev();
        int index = ctx.index();

        // Armor stand: snap to settled pose, sync prev-frame state for partialTick=0 render.
        if (ctx.wearer() instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            double backStretch = backStretchFn.applyAsDouble(index);
            float prevYaw = prev.getYRot();
            Vec3 offset = new Vec3(0, 0, backStretch).yRot((float) -Math.toRadians(prevYaw));
            Vec3 ideal = prev.position().add(offset);
            seg.setPos(ideal);
            seg.xo = ideal.x; seg.xOld = ideal.x;
            seg.yo = ideal.y; seg.yOld = ideal.y;
            seg.zo = ideal.z; seg.zOld = ideal.z;
            seg.yRotO = seg.getYRot();
            seg.xRotO = seg.getXRot();
            if (seg instanceof net.minecraft.world.entity.LivingEntity living) {
                living.yBodyRot = prevYaw;
                living.yBodyRotO = prevYaw;
                living.yHeadRot = prevYaw;
                living.yHeadRotO = prevYaw;
            }
            return;
        }

        float prevYaw = prev.yRotO;
        float prevPitch = prev.xRotO;
        float sideSwing = (float) ((sideSwayBase + index * sideSwayPerIndex)
                * Math.sin(ctx.tickCount() * 0.2 - index));
        double backStretch = backStretchFn.applyAsDouble(index);
        Vec3 offsetFromParent = new Vec3(sideSwing, 0, backStretch)
                .xRot((float) -Math.toRadians(prevPitch))
                .yRot((float) -Math.toRadians(prevYaw));
        Vec3 ideal = prev.position().add(offsetFromParent);

        Vec3 distVec = ideal.subtract(seg.position());
        double distLen = distVec.length();
        float extraLength = (float) Math.max(distLen - maxDistFromPrev, 0.0F);
        Vec3 step = distLen > 1.0 ? distVec.normalize().scale(1.0F + extraLength) : distVec;
        Vec3 newPos = seg.position().add(step);

        if (clampToSurface) {
            double surfaceY = SegmentChainManager.computeSurfaceY(seg.level(), newPos.x, newPos.y, newPos.z);
            float settleY = (float) Math.min(surfaceY, newPos.y);
            float lerpedY = Mth.approach((float) seg.getY(), settleY, 1.0F);
            newPos = new Vec3(newPos.x, lerpedY, newPos.z);
        }

        SegmentChainManager.savePrevFrame(seg);
        seg.setPos(newPos);
    }
}
