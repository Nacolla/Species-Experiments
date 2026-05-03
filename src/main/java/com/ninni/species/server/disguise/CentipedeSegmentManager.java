package com.ninni.species.server.disguise;

import com.ninni.species.registry.SpeciesConfig;
import com.ninni.species.server.disguise.panacea.spec.BodyXRotSlewRotator;
import com.ninni.species.server.disguise.panacea.spec.CapturePrevFrameHook;
import com.ninni.species.server.disguise.panacea.spec.CentipedeNativePositioner;
import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import com.ninni.species.server.disguise.panacea.spec.ChainSpec;
import com.ninni.species.server.disguise.panacea.spec.ChildUuidServerGuard;
import com.ninni.species.server.disguise.panacea.spec.HystereticGroundClampHook;
import com.ninni.species.server.disguise.panacea.spec.ReflectionPlan;
import com.ninni.species.server.disguise.panacea.spec.RestorePrevFrameHook;
import com.ninni.species.server.disguise.panacea.spec.WalkAnimMirror;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

/** Centipede disguise. Body and tail share {@code EntityCentipedeBody} but use distinct
 *  EntityTypes (last index = tail). Positioning delegates to AM's {@code tickMultipartPosition}
 *  wrapped with prev-frame restore + cliff clamp + BODY_XROT slew. */
public final class CentipedeSegmentManager extends SegmentChainManager {

    private static final ResourceLocation HEAD_ID = new ResourceLocation("alexsmobs", "centipede_head");
    private static final ResourceLocation BODY_ID = new ResourceLocation("alexsmobs", "centipede_body");
    private static final ResourceLocation TAIL_ID = new ResourceLocation("alexsmobs", "centipede_tail");
    /** AM finalizeSpawn picks 5-8 randomly; pin 6 for visual consistency. */
    private static final int SEGMENT_COUNT = 6;
    /** Matches AM's hardcoded multipart radius. */
    private static final float SEGMENT_SPACING = 0.84F;
    private static final double CLIFF_ENTER = 1.5;
    private static final double CLIFF_EXIT = 1.0;
    private static final double CLIFF_FALL_RATE = 0.4;
    /** Tighter than vanilla's 10° cap to smooth per-tick rocking from the discrete 0.2-step terrain probes. */
    private static final float MAX_PITCH_DELTA = 3.0F;

    // Declared AFTER the ID constants: buildSpec() references them in the constructor.
    public static final CentipedeSegmentManager INSTANCE = new CentipedeSegmentManager();

    private CentipedeSegmentManager() { super(buildSpec()); }

    private static ChainSpec buildSpec() {
        ReflectionPlan refl = ReflectionPlan.builder(HEAD_ID, BODY_ID)
                .tailPart(TAIL_ID)
                .publicMethod("setBodyParent", ReflectionPlan.Target.PART, "setParent", "net.minecraft.world.entity.Entity")
                .publicMethod("setBodyParentId", ReflectionPlan.Target.PART, "setParentId", "java.util.UUID")
                .publicMethod("setBodyChildId", ReflectionPlan.Target.PART, "setChildId", "java.util.UUID")
                .publicMethod("setBodyIndex", ReflectionPlan.Target.PART, "setBodyIndex", "int")
                .publicMethod("tickMultipartPosition", ReflectionPlan.Target.PART, "tickMultipartPosition",
                        "int", "float", "net.minecraft.world.phys.Vec3", "float", "float", "boolean")
                .publicMethod("getBackOffset", ReflectionPlan.Target.PART, "getBackOffset")
                .accessor("BODY_XROT", ReflectionPlan.Target.PART, "BODY_XROT")
                .field("prevHeight", ReflectionPlan.Target.PART, "prevHeight")
                .publicMethod("setHeadChildId", ReflectionPlan.Target.HEAD, "setChildId", "java.util.UUID")
                .publicMethod("setSegmentCount", ReflectionPlan.Target.HEAD, "setSegmentCount", "int")
                .declaredMethod("getYawForPart", ReflectionPlan.Target.HEAD, "getYawForPart", "int")
                .build();

        return ChainSpec.builder(HEAD_ID, BODY_ID)
                .tailPart(TAIL_ID)
                .segments(SEGMENT_COUNT)
                .spacing(SEGMENT_SPACING)
                .backStretch(-SEGMENT_SPACING)
                // isOpaqueBlockAt short-circuits when noPhysics=true; height probe needs terrain reads.
                .useNoPhysicsFlag(false)
                // 6 segments × 0.84 ≈ 5 blocks chain; small buffer for head/legs.
                .cameraSizeMinimum(7.0)
                .reflection(refl)
                .preTickHook(CapturePrevFrameHook.INSTANCE)
                .positioner(CentipedeNativePositioner.INSTANCE)
                .postTickHook(new HystereticGroundClampHook(
                        CLIFF_ENTER, CLIFF_EXIT, CLIFF_FALL_RATE,
                        CentipedeSegmentManager::clampFromConfig))
                .postTickHook(RestorePrevFrameHook.withYBodyRotForcedToYHeadRot())
                .rotator(new BodyXRotSlewRotator("BODY_XROT", MAX_PITCH_DELTA))
                .animDriver(WalkAnimMirror.noFloor())
                .serverGuard(new ChildUuidServerGuard("setHeadChildId"))
                .linker((seg, prev, index, wearer, disguise, chain, r) -> {
                    Method setBodyParent = r.method("setBodyParent");
                    Method setBodyParentId = r.method("setBodyParentId");
                    Method setBodyChildId = r.method("setBodyChildId");
                    Method setBodyIndex = r.method("setBodyIndex");
                    Method setHeadChildId = r.method("setHeadChildId");
                    Method setSegmentCount = r.method("setSegmentCount");
                    try {
                        if (setBodyParent != null) setBodyParent.invoke(seg, prev);
                        if (setBodyParentId != null) setBodyParentId.invoke(seg, prev.getUUID());
                        if (setBodyIndex != null) setBodyIndex.invoke(seg, index);
                        if (index == 0) {
                            if (setSegmentCount != null) setSegmentCount.invoke(disguise, SEGMENT_COUNT);
                            if (setHeadChildId != null) setHeadChildId.invoke(disguise, seg.getUUID());
                        } else if (setBodyChildId != null) {
                            // Back-mutation onto the previous segment.
                            setBodyChildId.invoke(chain.get(index - 1), seg.getUUID());
                        }
                    } catch (ReflectiveOperationException ignored) {}
                })
                .build();
    }

    private static boolean clampFromConfig() {
        try { return SpeciesConfig.SEGMENT_CHAIN_GROUND_CLAMP.get(); }
        catch (Throwable t) { return true; }
    }
}
