package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/** Per-tick context passed to every hook/strategy in a {@link ChainSpec} pipeline. Carries
 *  entity refs, frame info, reflection bundle, and a scratch map for hook-to-hook communication
 *  within a tick. */
public final class SegmentContext {

    private final Entity seg;
    private final Entity prev;
    private final int index;
    private final LivingEntity wearer;
    private final LivingEntity disguise;
    private final int tickCount;
    private final ChainSpec spec;

    /** Scratch slot for hooks to pass values between phases of the SAME tick. Keys must be agreed by the hooks involved. */
    private final Map<String, Object> scratch = new HashMap<>();

    public SegmentContext(Entity seg, Entity prev, int index, LivingEntity wearer,
                          LivingEntity disguise, int tickCount, ChainSpec spec) {
        this.seg = seg;
        this.prev = prev;
        this.index = index;
        this.wearer = wearer;
        this.disguise = disguise;
        this.tickCount = tickCount;
        this.spec = spec;
    }

    public Entity seg() { return seg; }
    public Entity prev() { return prev; }
    public int index() { return index; }
    public LivingEntity wearer() { return wearer; }
    public LivingEntity disguise() { return disguise; }
    public int tickCount() { return tickCount; }
    public ChainSpec spec() { return spec; }
    public ReflectionPlan reflection() { return spec.reflectionPlan(); }

    public void putScratch(String key, Object value) { scratch.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> T scratch(String key) { return (T) scratch.get(key); }
    public boolean hasScratch(String key) { return scratch.containsKey(key); }

    /** True if this segment is the LivingEntity flavor (most chain parts). */
    public boolean isLivingSeg() { return seg instanceof LivingEntity; }
    public LivingEntity livingSeg() { return seg instanceof LivingEntity le ? le : null; }
}
