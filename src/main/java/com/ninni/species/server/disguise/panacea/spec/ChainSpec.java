package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Immutable chain disguise spec (head + N segments) built via the fluent {@link Builder}. */
public final class ChainSpec {

    private final ResourceLocation headId;
    private final ResourceLocation partId;
    private final ResourceLocation tailPartId;
    private final int segmentCount;
    private final float initialSpacing;
    private final float backStretch;
    private final boolean useNoPhysicsFlag;
    private final boolean clampToSurface;
    private final double cameraSizeMinimum;

    private final ReflectionPlan reflectionPlan;

    private final List<BeforeChainTickHook> beforeChainTickHooks;
    private final Optional<TickGuard> tickGuard;
    private final List<TickHook> preTickHooks;
    private final SegmentPositioner positioner;
    private final List<TickHook> postTickHooks;
    private final SegmentRotator rotator;
    private final Optional<StatePropagator> propagator;
    private final Optional<AnimDriver> animDriver;

    private final ServerGuard serverGuard;
    private final SegmentLinker linker;

    private final List<StateSlot<?, ?>> stateSlots;

    private ChainSpec(Builder b) {
        this.headId = b.headId;
        this.partId = b.partId;
        this.tailPartId = b.tailPartId;
        this.segmentCount = b.segmentCount;
        this.initialSpacing = b.initialSpacing;
        this.backStretch = b.backStretch;
        this.useNoPhysicsFlag = b.useNoPhysicsFlag;
        this.clampToSurface = b.clampToSurface;
        this.cameraSizeMinimum = b.cameraSizeMinimum;
        this.reflectionPlan = b.reflectionPlan;
        this.beforeChainTickHooks = List.copyOf(b.beforeChainTickHooks);
        this.tickGuard = Optional.ofNullable(b.tickGuard);
        this.preTickHooks = List.copyOf(b.preTickHooks);
        this.positioner = b.positioner;
        this.postTickHooks = List.copyOf(b.postTickHooks);
        this.rotator = b.rotator;
        this.propagator = Optional.ofNullable(b.propagator);
        this.animDriver = Optional.ofNullable(b.animDriver);
        this.serverGuard = b.serverGuard;
        this.linker = b.linker;
        this.stateSlots = List.copyOf(b.stateSlots);
    }

    public ResourceLocation headId() { return headId; }
    public ResourceLocation partId() { return partId; }
    public ResourceLocation tailPartId() { return tailPartId; }
    public int segmentCount() { return segmentCount; }
    public float initialSpacing() { return initialSpacing; }
    public float backStretch() { return backStretch; }
    public boolean useNoPhysicsFlag() { return useNoPhysicsFlag; }
    public boolean clampToSurface() { return clampToSurface; }
    /** Floor for the third-person camera distance scaler. {@code 0.0} = no floor (use intrinsic AABB only). */
    public double cameraSizeMinimum() { return cameraSizeMinimum; }
    public ReflectionPlan reflectionPlan() { return reflectionPlan; }
    public List<BeforeChainTickHook> beforeChainTickHooks() { return beforeChainTickHooks; }
    public Optional<TickGuard> tickGuard() { return tickGuard; }
    public List<TickHook> preTickHooks() { return preTickHooks; }
    public SegmentPositioner positioner() { return positioner; }
    public List<TickHook> postTickHooks() { return postTickHooks; }
    public SegmentRotator rotator() { return rotator; }
    public Optional<StatePropagator> propagator() { return propagator; }
    public Optional<AnimDriver> animDriver() { return animDriver; }
    public ServerGuard serverGuard() { return serverGuard; }
    public SegmentLinker linker() { return linker; }
    public List<StateSlot<?, ?>> stateSlots() { return stateSlots; }

    public static Builder builder(ResourceLocation headId, ResourceLocation partId) {
        return new Builder(headId, partId);
    }

    public static final class Builder {
        private final ResourceLocation headId;
        private final ResourceLocation partId;
        private ResourceLocation tailPartId;
        private int segmentCount = 1;
        private float initialSpacing = 1.0F;
        private float backStretch = -1.0F;
        private boolean useNoPhysicsFlag = true;
        private boolean clampToSurface = false;
        private double cameraSizeMinimum = 0.0;

        private ReflectionPlan reflectionPlan;
        private final List<BeforeChainTickHook> beforeChainTickHooks = new ArrayList<>();
        private TickGuard tickGuard;
        private final List<TickHook> preTickHooks = new ArrayList<>();
        private SegmentPositioner positioner;
        private final List<TickHook> postTickHooks = new ArrayList<>();
        private SegmentRotator rotator;
        private StatePropagator propagator;
        private AnimDriver animDriver;
        private ServerGuard serverGuard;
        private SegmentLinker linker;
        private final List<StateSlot<?, ?>> stateSlots = new ArrayList<>();

        Builder(ResourceLocation headId, ResourceLocation partId) {
            this.headId = headId;
            this.partId = partId;
        }

        public Builder tailPart(ResourceLocation id) { this.tailPartId = id; return this; }
        public Builder segments(int count) { this.segmentCount = count; return this; }
        public Builder spacing(float spacing) { this.initialSpacing = spacing; return this; }
        public Builder backStretch(float stretch) { this.backStretch = stretch; return this; }
        public Builder useNoPhysicsFlag(boolean v) { this.useNoPhysicsFlag = v; return this; }
        public Builder clampToSurface(boolean v) { this.clampToSurface = v; return this; }
        /**
         * Pin the third-person camera floor for chains whose silhouette extends beyond the head's
         * AABB (segments aren't part of {@code disguise.getBbWidth()}). {@code 0.0} skips the pin.
         */
        public Builder cameraSizeMinimum(double v) { this.cameraSizeMinimum = v; return this; }

        public Builder reflection(ReflectionPlan plan) { this.reflectionPlan = plan; return this; }
        public Builder beforeChainTickHook(BeforeChainTickHook hook) { beforeChainTickHooks.add(hook); return this; }
        public Builder tickGuard(TickGuard guard) { this.tickGuard = guard; return this; }
        public Builder preTickHook(TickHook hook) { preTickHooks.add(hook); return this; }
        public Builder positioner(SegmentPositioner p) { this.positioner = p; return this; }
        public Builder postTickHook(TickHook hook) { postTickHooks.add(hook); return this; }
        public Builder rotator(SegmentRotator r) { this.rotator = r; return this; }
        public Builder propagator(StatePropagator p) { this.propagator = p; return this; }
        public Builder animDriver(AnimDriver a) { this.animDriver = a; return this; }
        public Builder serverGuard(ServerGuard g) { this.serverGuard = g; return this; }
        public Builder linker(SegmentLinker l) { this.linker = l; return this; }

        public Builder stateSlot(StateSlot<?, ?> slot) { stateSlots.add(slot); return this; }

        public ChainSpec build() {
            if (headId == null || partId == null) {
                throw new IllegalStateException("ChainSpec headId/partId must be non-null (check static field init order)");
            }
            if (reflectionPlan == null) reflectionPlan = ReflectionPlan.builder(headId, partId).build();
            if (positioner == null || rotator == null || serverGuard == null || linker == null) {
                throw new IllegalStateException("ChainSpec requires positioner, rotator, serverGuard, linker");
            }
            return new ChainSpec(this);
        }
    }
}
