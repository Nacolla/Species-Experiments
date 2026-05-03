package com.ninni.species.server.disguise.panacea.spec;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/** AM Anaconda native positioner. Per-segment yaw = {@code slither(phase, i) + yawHist(ring, 4+i*2)},
 *  fed to {@code tickMultipartPosition}. Yaw history and floored phase come from the wearer
 *  (disguise body's values stay flat without aiStep; raw walkDist crawls when crouching). */
public final class AnacondaPositioner implements SegmentPositioner {

    private final StateSlot<UUID, YawRingBuffer> ringSlot;
    private final StateSlot<UUID, float[]> phaseSlot;
    private final float slitherAmplitude;
    private final int sampleBaseOffset;
    private final int sampleStride;

    public AnacondaPositioner(StateSlot<UUID, YawRingBuffer> ringSlot,
                              StateSlot<UUID, float[]> phaseSlot,
                              float slitherAmplitude,
                              int sampleBaseOffset, int sampleStride) {
        this.ringSlot = ringSlot;
        this.phaseSlot = phaseSlot;
        this.slitherAmplitude = slitherAmplitude;
        this.sampleBaseOffset = sampleBaseOffset;
        this.sampleStride = sampleStride;
    }

    @Override
    public void position(SegmentContext ctx) {
        Method tickMultipart = ctx.reflection().method("tickMultipartPosition");
        if (tickMultipart == null) return;

        Entity seg = ctx.seg();
        Entity prev = ctx.prev();
        LivingEntity wearer = ctx.wearer();
        LivingEntity disguise = ctx.disguise();
        int index = ctx.index();

        Object parentIndex;
        if (index == 0) {
            parentIndex = AnacondaPartIndexResolver.head();
        } else {
            Method getPartType = ctx.reflection().method("getPartType");
            try {
                parentIndex = (getPartType != null) ? getPartType.invoke(prev) : AnacondaPartIndexResolver.head();
            } catch (ReflectiveOperationException e) {
                parentIndex = AnacondaPartIndexResolver.head();
            }
        }
        if (parentIndex == null) return;

        YawRingBuffer ring = ringSlot.get(wearer.getUUID());
        float yawHist = (ring != null) ? ring.sample(sampleBaseOffset + index * sampleStride) : wearer.getYRot();
        float[] phaseHolder = phaseSlot.get(wearer.getUUID());
        float phase = (phaseHolder != null) ? phaseHolder[0] : 0F;
        float prevReqRot = slither(phase, index) + yawHist;
        float reqRot = slither(phase, index + 1) + yawHist;

        try {
            tickMultipart.invoke(seg, disguise.getId(), parentIndex, prev.position(),
                    prev.getXRot(), prevReqRot, reqRot, true);
        } catch (ReflectiveOperationException ignored) { return; }

        SegmentChainManager.savePrevFrame(seg);
    }

    private float slither(float phase, int i) {
        return (float) (slitherAmplitude * -Math.sin(phase - i));
    }
}
