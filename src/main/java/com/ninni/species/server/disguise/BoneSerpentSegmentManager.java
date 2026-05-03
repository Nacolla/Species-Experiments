package com.ninni.species.server.disguise;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import com.ninni.species.server.disguise.panacea.spec.ChainSpec;
import com.ninni.species.server.disguise.panacea.spec.ChildUuidServerGuard;
import com.ninni.species.server.disguise.panacea.spec.OrbitalPositioner;
import com.ninni.species.server.disguise.panacea.spec.ParentYawCopyRotator;
import com.ninni.species.server.disguise.panacea.spec.ReflectionPlan;
import com.ninni.species.server.disguise.panacea.spec.WalkAnimMirror;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Bone Serpent disguise. Segments orbit the parent's lagged frame on a fixed-radius circle;
 *  body wave lives in the model's per-bone offsets, not segment positions. Pinned to 10. */
public final class BoneSerpentSegmentManager extends SegmentChainManager {

    private static final ResourceLocation HEAD_ID = new ResourceLocation("alexsmobs", "bone_serpent");
    private static final ResourceLocation PART_ID = new ResourceLocation("alexsmobs", "bone_serpent_part");
    private static final int SEGMENT_COUNT = 10;
    private static final float DEFAULT_RADIUS = 0.9F;
    private static final float DEFAULT_ANGLE_YAW = (180.0F + 90.0F) * Mth.DEG_TO_RAD;
    private static final float DEFAULT_OFFSET_Y = 0.0F;
    private static final float PITCH_APPROACH_RATE = 5.0F;
    /** Idle floor keeps the model's swim wave firing while the wearer is stationary. */
    private static final float WALK_IDLE_FLOOR = 0.15F;

    // Declared AFTER the ID constants: buildSpec() references them in the constructor.
    public static final BoneSerpentSegmentManager INSTANCE = new BoneSerpentSegmentManager();

    private BoneSerpentSegmentManager() { super(buildSpec()); }

    private static ChainSpec buildSpec() {
        ReflectionPlan refl = ReflectionPlan.builder(HEAD_ID, PART_ID)
                .publicMethod("setParent", ReflectionPlan.Target.PART, "setParent", "net.minecraft.world.entity.Entity")
                .publicMethod("setParentId", ReflectionPlan.Target.PART, "setParentId", "java.util.UUID")
                .publicMethod("setBodyIndex", ReflectionPlan.Target.PART, "setBodyIndex", "int")
                .publicMethod("setTail", ReflectionPlan.Target.PART, "setTail", "boolean")
                .field("radius", ReflectionPlan.Target.PART, "radius")
                .field("angleYaw", ReflectionPlan.Target.PART, "angleYaw")
                .field("offsetY", ReflectionPlan.Target.PART, "offsetY")
                .publicMethod("setChildId", ReflectionPlan.Target.HEAD, "setChildId", "java.util.UUID")
                .build();

        return ChainSpec.builder(HEAD_ID, PART_ID)
                .segments(SEGMENT_COUNT)
                .backStretch(-1.0F)
                // 10 segments × 1.0 spacing = 10 blocks orbital ring; buffer for head + sway.
                .cameraSizeMinimum(12.0)
                .reflection(refl)
                .positioner(new OrbitalPositioner(
                        DEFAULT_RADIUS, DEFAULT_ANGLE_YAW, DEFAULT_OFFSET_Y,
                        "radius", "angleYaw", "offsetY"))
                .rotator(new ParentYawCopyRotator(PITCH_APPROACH_RATE))
                .animDriver(new WalkAnimMirror(WALK_IDLE_FLOOR))
                .serverGuard(new ChildUuidServerGuard("setChildId"))
                .linker((seg, prev, index, wearer, disguise, chain, r) -> {
                    Method setParent = r.method("setParent");
                    Method setParentId = r.method("setParentId");
                    Method setBodyIndex = r.method("setBodyIndex");
                    Method setTail = r.method("setTail");
                    Method setChildId = r.method("setChildId");
                    Field radiusF = r.field("radius");
                    Field angleYawF = r.field("angleYaw");
                    Field offsetYF = r.field("offsetY");
                    try {
                        if (setParent != null) setParent.invoke(seg, prev);
                        if (setParentId != null) setParentId.invoke(seg, prev.getUUID());
                        if (setBodyIndex != null) setBodyIndex.invoke(seg, index);
                        if (index == SEGMENT_COUNT - 1 && setTail != null) setTail.invoke(seg, true);
                        // AM's natural ctor takes radius/angleYaw/offsetY; we use the 2-arg ctor
                        // and write them here.
                        if (radiusF != null) radiusF.setFloat(seg, DEFAULT_RADIUS);
                        if (angleYawF != null) angleYawF.setFloat(seg, DEFAULT_ANGLE_YAW);
                        if (offsetYF != null) offsetYF.setFloat(seg, DEFAULT_OFFSET_Y);
                        if (index == 0 && setChildId != null) setChildId.invoke(disguise, seg.getUUID());
                    } catch (ReflectiveOperationException ignored) {}
                })
                .build();
    }
}
