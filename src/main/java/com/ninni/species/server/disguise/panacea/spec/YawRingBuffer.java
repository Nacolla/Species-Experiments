package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.util.Mth;

/** Power-of-2 rolling buffer of yaw history; {@link #sample(int)} returns the value N ticks ago. */
public final class YawRingBuffer {

    private final float[] data;
    private final int mask;
    private int idx;

    public YawRingBuffer(int sizePow2) {
        if (Integer.bitCount(sizePow2) != 1) throw new IllegalArgumentException("size must be a power of 2");
        this.data = new float[sizePow2];
        this.mask = sizePow2 - 1;
    }

    public void advance(float yaw) {
        idx = (idx + 1) & mask;
        data[idx] = yaw;
    }

    public float sample(int ticksBack) {
        return Mth.wrapDegrees(data[(idx - ticksBack) & mask]);
    }
}
