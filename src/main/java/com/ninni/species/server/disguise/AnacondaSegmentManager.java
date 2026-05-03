package com.ninni.species.server.disguise;

import com.ninni.species.server.disguise.panacea.spec.AnacondaPartIndexResolver;
import com.ninni.species.server.disguise.panacea.spec.AnacondaPositioner;
import com.ninni.species.server.disguise.panacea.spec.AnacondaSlitherPhaseHook;
import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import com.ninni.species.server.disguise.panacea.spec.ChainSpec;
import com.ninni.species.server.disguise.panacea.spec.NoOpRotator;
import com.ninni.species.server.disguise.panacea.spec.ReflectionPlan;
import com.ninni.species.server.disguise.panacea.spec.StateSlot;
import com.ninni.species.server.disguise.panacea.spec.WalkDistSyncHook;
import com.ninni.species.server.disguise.panacea.spec.YawRingAdvanceHook;
import com.ninni.species.server.disguise.panacea.spec.YawRingBuffer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.UUID;

/** Anaconda disguise. Replicates AM's S-wave math locally (head's private ringBuffer/walkDist
 *  don't update reliably without level-ticking). Per-segment yaw is local-ring sample +
 *  sin-based slither term, both seeded from the wearer. */
public final class AnacondaSegmentManager extends SegmentChainManager {

    private static final ResourceLocation HEAD_ID = new ResourceLocation("alexsmobs", "anaconda");
    private static final ResourceLocation PART_ID = new ResourceLocation("alexsmobs", "anaconda_part");
    private static final int SEGMENT_COUNT = 7;
    private static final int RING_BUFFER_SIZE = 64;
    private static final float SLITHER_AMPLITUDE = 40F;
    /** Multiplier applied to per-tick {@code walkDist} delta when advancing the slither phase. */
    private static final float SLITHER_WALKDIST_MULTIPLIER = 3F;
    /** Floor on slither phase advance while moving — engages when {@code walkDist} growth stalls
     *  (e.g. crouched). Set to AM's {@code MOVEMENT_SPEED} (0.15F) so cadence reads as the
     *  native gait, not the player's. */
    private static final float SLITHER_FLOOR_WHEN_MOVING = 0.15F;
    private static final int SAMPLE_BASE_OFFSET = 4;
    private static final int SAMPLE_STRIDE = 2;

    private static final StateSlot<UUID, YawRingBuffer> RING_SLOT =
            StateSlot.perWearer(() -> new YawRingBuffer(RING_BUFFER_SIZE));
    private static final StateSlot<UUID, float[]> SLITHER_PHASE_SLOT =
            StateSlot.perWearer(() -> new float[1]);

    // Declared AFTER the ID constants and slot fields: buildSpec() references them in the constructor.
    public static final AnacondaSegmentManager INSTANCE = new AnacondaSegmentManager();

    private AnacondaSegmentManager() { super(buildSpec()); }

    private static ChainSpec buildSpec() {
        ReflectionPlan refl = ReflectionPlan.builder(HEAD_ID, PART_ID)
                .auxClass(AnacondaPartIndexResolver.FQN)
                .publicMethod("setParent", ReflectionPlan.Target.PART, "setParent", "net.minecraft.world.entity.Entity")
                .publicMethod("setParentId", ReflectionPlan.Target.PART, "setParentId", "java.util.UUID")
                .publicMethod("setChildId", ReflectionPlan.Target.PART, "setChildId", "java.util.UUID")
                .publicMethod("setBodyIndex", ReflectionPlan.Target.PART, "setBodyIndex", "int")
                .publicMethod("setPartType", ReflectionPlan.Target.PART, "setPartType",
                        AnacondaPartIndexResolver.FQN)
                .publicMethod("getPartType", ReflectionPlan.Target.PART, "getPartType")
                .publicMethod("tickMultipartPosition", ReflectionPlan.Target.PART, "tickMultipartPosition",
                        "int", AnacondaPartIndexResolver.FQN, "net.minecraft.world.phys.Vec3",
                        "float", "float", "float", "boolean")
                .publicMethod("setHeadChildId", ReflectionPlan.Target.HEAD, "setChildId", "java.util.UUID")
                // Private parts array on the head; pre-allocated server-side so the head's tick
                // doesn't NPE (it iterates {@code this.parts[i]} unconditionally after the spawn
                // gate, which our CHILD_UUID pin closes).
                .field("partsArray", ReflectionPlan.Target.HEAD, "parts")
                .build();

        return ChainSpec.builder(HEAD_ID, PART_ID)
                .segments(SEGMENT_COUNT)
                .spacing(1.5F)
                .backStretch(-1.5F)
                // EntityAnacondaPart probes blocks via isOpaqueBlockAt; noPhysics=true would
                // force-return false and sink segments.
                .useNoPhysicsFlag(false)
                // 7 segments × 1.5 spacing ≈ 10 blocks; buffer for head + slither curl.
                .cameraSizeMinimum(13.0)
                .reflection(refl)
                .stateSlot(RING_SLOT)
                .stateSlot(SLITHER_PHASE_SLOT)
                .beforeChainTickHook(new YawRingAdvanceHook(RING_SLOT))
                // wearer.walkDist freezes on the client for non-LocalPlayer wearers; without this
                // hook the slither term reads a constant value and the chain stops oscillating.
                .beforeChainTickHook(WalkDistSyncHook.INSTANCE)
                // Crouching collapses raw walkDist growth to near-zero — the floored phase keeps
                // the wave at vanilla pace while normal walking still rides the natural delta.
                .beforeChainTickHook(new AnacondaSlitherPhaseHook(
                        SLITHER_PHASE_SLOT, SLITHER_WALKDIST_MULTIPLIER, SLITHER_FLOOR_WHEN_MOVING))
                .positioner(new AnacondaPositioner(
                        RING_SLOT, SLITHER_PHASE_SLOT, SLITHER_AMPLITUDE,
                        SAMPLE_BASE_OFFSET, SAMPLE_STRIDE))
                .rotator(NoOpRotator.INSTANCE)
                .serverGuard((wearer, disguise, r) -> {
                    // Pin CHILD_UUID so getChild() resolves and the head's first-tick spawn-loop is skipped.
                    Method setChildId = r.method("setHeadChildId");
                    if (setChildId != null) {
                        try { setChildId.invoke(disguise, wearer.getUUID()); }
                        catch (ReflectiveOperationException ignored) {}
                    }
                    // Pre-allocate the head's private parts array — the tick loop reads
                    // this.parts[i] unconditionally and would NPE without it.
                    java.lang.reflect.Field partsField = r.field("partsArray");
                    if (partsField != null) {
                        Class<?> componentType = partsField.getType().getComponentType();
                        if (componentType != null) {
                            try {
                                Object array = java.lang.reflect.Array.newInstance(componentType, SEGMENT_COUNT);
                                partsField.set(disguise, array);
                            } catch (ReflectiveOperationException ignored) {}
                        }
                    }
                })
                .linker((seg, prev, index, wearer, disguise, chain, r) -> {
                    Method setParent = r.method("setParent");
                    Method setParentId = r.method("setParentId");
                    Method setChildId = r.method("setChildId");
                    Method setBodyIndex = r.method("setBodyIndex");
                    Method setPartType = r.method("setPartType");
                    Method setHeadChildId = r.method("setHeadChildId");
                    try {
                        if (setParent != null) setParent.invoke(seg, prev);
                        if (setParentId != null) setParentId.invoke(seg, prev.getUUID());
                        if (setBodyIndex != null) setBodyIndex.invoke(seg, index);
                        // sizeAt(1+i) maps body index to NECK/BODY/TAIL — matches EntityAnaconda.aiStep.
                        if (setPartType != null) {
                            Object partTypeVal = AnacondaPartIndexResolver.sizeAt(1 + index);
                            if (partTypeVal != null) setPartType.invoke(seg, partTypeVal);
                        }
                        if (index == 0) {
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
