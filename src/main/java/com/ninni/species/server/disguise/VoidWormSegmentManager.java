package com.ninni.species.server.disguise;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import com.ninni.species.server.disguise.panacea.spec.ChainSpec;
import com.ninni.species.server.disguise.panacea.spec.NoOpRotator;
import com.ninni.species.server.disguise.panacea.spec.ReflectionPlan;
import com.ninni.species.server.disguise.panacea.spec.VoidWormAnglePropagator;
import com.ninni.species.server.disguise.panacea.spec.VoidWormPositioner;
import com.ninni.species.server.disguise.panacea.spec.VoidWormServerGuard;
import com.ninni.species.server.disguise.panacea.spec.WormAngleAccumulatorHook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Void Worm disguise. Orbital positioning around the parent's lagged frame; per-segment yaw is
 *  branch-unwrapped against prev WORM_YAW so the renderer's raw lerp doesn't flip past ±π. Body
 *  curve is a 1-tick-delayed WORM_ANGLE wave from the head. Pinned to 25 segments. */
public final class VoidWormSegmentManager extends SegmentChainManager {

    private static final ResourceLocation HEAD_ID = new ResourceLocation("alexsmobs", "void_worm");
    private static final ResourceLocation PART_ID = new ResourceLocation("alexsmobs", "void_worm_part");
    private static final int SEGMENT_COUNT = 25;
    /** Last N indices spawn as tail parts (matches AM head-side tailstart logic). */
    private static final int TAIL_COUNT = 4;

    private static final float WORM_ANGLE_DELTA = 15F;
    private static final float WORM_ANGLE_DECAY = 20F;
    private static final float WORM_ANGLE_TURN_THRESHOLD = 0.05F;
    private static final float WORM_ANGLE_CLAMP = 60F;

    private static final float ANGLE_YAW_CONSTANT = (180.0F + 90.0F) * Mth.DEG_TO_RAD;

    // Declared AFTER the constants: buildSpec() references them in the constructor.
    public static final VoidWormSegmentManager INSTANCE = new VoidWormSegmentManager();

    private VoidWormSegmentManager() { super(buildSpec()); }

    private static ChainSpec buildSpec() {
        ReflectionPlan refl = ReflectionPlan.builder(HEAD_ID, PART_ID)
                .publicMethod("setParent", ReflectionPlan.Target.PART, "setParent", "net.minecraft.world.entity.Entity")
                .publicMethod("setParentId", ReflectionPlan.Target.PART, "setParentId", "java.util.UUID")
                .publicMethod("setChildId", ReflectionPlan.Target.PART, "setChildId", "java.util.UUID")
                .publicMethod("setBodyIndex", ReflectionPlan.Target.PART, "setBodyIndex", "int")
                .publicMethod("setTail", ReflectionPlan.Target.PART, "setTail", "boolean")
                .publicMethod("setWormScale", ReflectionPlan.Target.PART, "setWormScale", "float")
                .publicMethod("getWormScale", ReflectionPlan.Target.PART, "getWormScale")
                .publicMethod("getPortalTicks", ReflectionPlan.Target.PART, "getPortalTicks")
                .publicMethod("isTail", ReflectionPlan.Target.PART, "isTail")
                .field("radius", ReflectionPlan.Target.PART, "radius")
                .field("angleYaw", ReflectionPlan.Target.PART, "angleYaw")
                .field("offsetY", ReflectionPlan.Target.PART, "offsetY")
                .field("prevWormYaw", ReflectionPlan.Target.PART, "prevWormYaw")
                .field("partPrevWormAngle", ReflectionPlan.Target.PART, "prevWormAngle")
                .accessor("WORM_YAW", ReflectionPlan.Target.PART, "WORM_YAW")
                .accessor("WORM_ANGLE", ReflectionPlan.Target.PART, "WORM_ANGLE")
                .publicMethod("setHeadChildId", ReflectionPlan.Target.HEAD, "setChildId", "java.util.UUID")
                .publicMethod("setSegmentCount", ReflectionPlan.Target.HEAD, "setSegmentCount", "int")
                .publicMethod("getWormAngle", ReflectionPlan.Target.HEAD, "getWormAngle")
                .publicMethod("setWormAngle", ReflectionPlan.Target.HEAD, "setWormAngle", "float")
                .field("headPrevWormAngle", ReflectionPlan.Target.HEAD, "prevWormAngle")
                .field("makePortalCooldown", ReflectionPlan.Target.HEAD, "makePortalCooldown")
                .field("makeIdlePortalCooldown", ReflectionPlan.Target.HEAD, "makeIdlePortalCooldown")
                .build();

        return ChainSpec.builder(HEAD_ID, PART_ID)
                .segments(SEGMENT_COUNT)
                .spacing(1.2F)
                .backStretch(-1.2F)
                // 25 segments × 1.2 spacing = 30 blocks; add buffer for head + in-flight curl.
                .cameraSizeMinimum(35.0)
                .reflection(refl)
                .beforeChainTickHook(new WormAngleAccumulatorHook(
                        "getWormAngle", "setWormAngle", "headPrevWormAngle",
                        WORM_ANGLE_DELTA, WORM_ANGLE_DECAY, WORM_ANGLE_TURN_THRESHOLD, WORM_ANGLE_CLAMP))
                .positioner(VoidWormPositioner.INSTANCE)
                .rotator(NoOpRotator.INSTANCE)
                .propagator(VoidWormAnglePropagator.INSTANCE)
                .serverGuard(VoidWormServerGuard.INSTANCE)
                .linker((seg, prev, index, wearer, disguise, chain, r) -> {
                    Method setParent = r.method("setParent");
                    Method setParentId = r.method("setParentId");
                    Method setChildId = r.method("setChildId");
                    Method setBodyIndex = r.method("setBodyIndex");
                    Method setTail = r.method("setTail");
                    Method setWormScale = r.method("setWormScale");
                    Method setHeadChildId = r.method("setHeadChildId");
                    Method setSegmentCount = r.method("setSegmentCount");
                    Field radiusF = r.field("radius");
                    Field angleYawF = r.field("angleYaw");
                    Field offsetYF = r.field("offsetY");
                    try {
                        if (setParent != null) setParent.invoke(seg, prev);
                        if (setParentId != null) setParentId.invoke(seg, prev.getUUID());
                        if (setBodyIndex != null) setBodyIndex.invoke(seg, index);

                        int total = SEGMENT_COUNT;
                        int tailStart = total - TAIL_COUNT;
                        boolean tail = index >= tailStart;
                        if (setTail != null) setTail.invoke(seg, tail);

                        // Worm scale: 1F + (i/total)·0.5 (matches EntityVoidWorm.aiStep), tail damped.
                        float scale = 1.0F + ((float) index / (float) total) * 0.5F;
                        if (tail) scale *= 0.85F;
                        if (setWormScale != null) setWormScale.invoke(seg, scale);

                        // Native ctor takes radius/angleYaw/offsetY; we use the 2-arg ctor and write here.
                        float radius = 1.0F + (scale * (tail ? 0.65F : 0.3F)) + (index == 0 ? 0.8F : 0.0F);
                        float offsetY = (index == 0) ? 0.0F : (index == tailStart ? -0.3F : 0.0F);
                        if (radiusF != null) radiusF.setFloat(seg, radius);
                        if (angleYawF != null) angleYawF.setFloat(seg, ANGLE_YAW_CONSTANT);
                        if (offsetYF != null) offsetYF.setFloat(seg, offsetY);

                        if (index == 0) {
                            if (setSegmentCount != null) setSegmentCount.invoke(disguise, total);
                            if (setHeadChildId != null) setHeadChildId.invoke(disguise, seg.getUUID());
                        } else if (setChildId != null) {
                            // Back-mutation onto the previous segment.
                            setChildId.invoke(chain.get(index - 1), seg.getUUID());
                        }
                    } catch (ReflectiveOperationException ignored) {}
                })
                .build();
    }
}
