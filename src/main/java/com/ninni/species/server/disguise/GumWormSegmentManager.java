package com.ninni.species.server.disguise;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns client-side {@code GumWormSegmentEntity} chain for a Gum Worm disguise.
 * Wires {@code FRONT_ENTITY_ID}/{@code HEAD_ENTITY_ID} so {@code GumWormSegmentRenderer} picks segments up,
 * tick-repositions mimicking {@code getIdealPosition}. Soft-dep AC; no-op when absent.
 */
public final class GumWormSegmentManager {

    /** Number of segments to spawn — matches AC's natural-spawn count (15-19). */
    private static final int SEGMENT_COUNT = 17;

    /** Resource id of AC's GumWormEntity (for type-checking the disguise). */
    private static final ResourceLocation GUM_WORM_ID = new ResourceLocation("alexscaves", "gum_worm");

    /** Resource id of AC's GumWormSegmentEntity. */
    private static final ResourceLocation GUM_WORM_SEGMENT_ID = new ResourceLocation("alexscaves", "gum_worm_segment");

    /** Per-wearer segment chain (client-side only). */
    private static final java.util.Map<UUID, List<Entity>> SEGMENTS = new ConcurrentHashMap<>();

    static {
        // Self-register with Panacea so the segment chain is included in sub-entity render dispatch.
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.registerGlobalSubEntityProvider(
                (wearer, disguise) -> {
                    if (!isGumWormDisguise(disguise)) return java.util.Collections.emptyList();
                    List<Entity> segs = SEGMENTS.get(wearer.getUUID());
                    return segs != null ? segs : java.util.Collections.emptyList();
                });
    }

    /** Read-only accessor used by the render hook. */
    public static List<Entity> getSegments(UUID wearerUUID) {
        return SEGMENTS.get(wearerUUID);
    }

    private static volatile boolean reflectionInited;
    private static EntityType<?> segmentType;
    private static EntityDataAccessor<Integer> headEntityIdAccessor;
    private static EntityDataAccessor<Integer> frontEntityIdAccessor;
    private static EntityDataAccessor<Integer> backEntityIdAccessor;
    private static Method setIndexMethod;
    private static Method setHeadUuidMethod;
    private static Method setFrontUuidMethod;
    private static Method setBackUuidMethod;

    private GumWormSegmentManager() {}

    public static boolean isGumWormDisguise(LivingEntity disguise) {
        if (disguise == null) return false;
        EntityType<?> type = disguise.getType();
        return type != null && BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(GUM_WORM_ID);
    }

    /** Called when a Gum Worm disguise is freshly created. Client-only. */
    public static void onDisguiseCreated(LivingEntity wearer, LivingEntity disguise) {
        if (wearer == null || disguise == null) return;
        if (!wearer.level().isClientSide) return;
        if (!isGumWormDisguise(disguise)) return;
        initReflection(disguise);
        if (segmentType == null) return;

        // Defensive cleanup of any stale chain.
        removeSegmentsByUUID(wearer.getUUID());

        Level level = wearer.level();
        List<Entity> chain = new ArrayList<>(SEGMENT_COUNT);
        Entity prev = wearer;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            Entity seg = segmentType.create(level);
            if (seg == null) continue;

            // Anchor the segment so its stock tick can't push or fall.
            seg.noPhysics = true;
            seg.setNoGravity(true);

            // Initial position — straight line behind the wearer; tickSegments
            // will refine each frame using the swimming/swaying offset.
            Vec3 initial = wearer.position().add(
                    new Vec3(0, 0, -2.5 * (i + 1)).yRot((float) -Math.toRadians(wearer.getYRot())));
            seg.setPos(initial);
            seg.xo = initial.x; seg.yo = initial.y; seg.zo = initial.z;
            seg.xOld = initial.x; seg.yOld = initial.y; seg.zOld = initial.z;
            seg.setYRot(wearer.getYRot());
            seg.yRotO = wearer.getYRot();

            // Do NOT call level.addFreshEntity — ClientLevel silently ignores it
            // (entities arrive via server spawn packets). Keep segments in the static
            // map and render manually from ForgeClientEvents.livingEntityRenderer.
            // Segment 0's FRONT_ENTITY_ID = wearer's id (exists in client level) so
            // GumWormSegmentRenderer draws the front connector; deeper segments skip
            // it because their previous segment isn't in the level.
            try {
                if (headEntityIdAccessor != null) seg.getEntityData().set(headEntityIdAccessor, wearer.getId());
                if (frontEntityIdAccessor != null) seg.getEntityData().set(frontEntityIdAccessor, prev.getId());
                if (setHeadUuidMethod != null) setHeadUuidMethod.invoke(seg, wearer.getUUID());
                if (setFrontUuidMethod != null) setFrontUuidMethod.invoke(seg, prev.getUUID());
                if (setIndexMethod != null) setIndexMethod.invoke(seg, i);
                // Update the previous segment's BACK pointer so the chain is doubly linked.
                if (i > 0) {
                    Entity prevSeg = chain.get(i - 1);
                    if (backEntityIdAccessor != null) prevSeg.getEntityData().set(backEntityIdAccessor, seg.getId());
                    if (setBackUuidMethod != null) setBackUuidMethod.invoke(prevSeg, seg.getUUID());
                }
            } catch (ReflectiveOperationException ignored) {}

            chain.add(seg);
            prev = seg;
        }
        SEGMENTS.put(wearer.getUUID(), chain);
    }

    /** Per-tick segment positioning mirroring {@code GumWormSegmentEntity.tick}: parent-yRotO ideal, speed-limited approach, snake yaw/pitch, terrain Y probe. */
    public static void tickSegments(LivingEntity wearer) {
        if (wearer == null) return;
        if (!wearer.level().isClientSide) return;
        List<Entity> chain = SEGMENTS.get(wearer.getUUID());
        if (chain == null || chain.isEmpty()) return;

        Entity prev = wearer;
        int tickCount = wearer.tickCount;
        final float maxDistFromFront = 2.0F;
        final float yawApproachDegPerTick = 7.0F;
        final float pitchApproachDegPerTick = 5.0F;

        for (int i = 0; i < chain.size(); i++) {
            Entity seg = chain.get(i);
            if (seg == null || seg.isRemoved()) continue;

            // (1) Ideal position using parent's previous-tick rotation — source of chain lag.
            float prevYaw = prev.yRotO;
            float prevPitch = prev.xRotO;
            float sideSwing = (0.5F + i * 0.05F) * (float) Math.sin(tickCount * 0.2F - i);
            // Mirrors getIdealPosition:228 — segment 0 gets an extended back-stretch.
            float backStretch = (i == 0) ? (-2.5F - 1.2F) : -2.5F;
            Vec3 offsetFromParent = new Vec3(sideSwing, 0, backStretch)
                    .xRot((float) -Math.toRadians(prevPitch))
                    .yRot((float) -Math.toRadians(prevYaw));
            Vec3 ideal = prev.position().add(offsetFromParent);

            // (2) Speed-limited approach: full move if dist ≤ 1, else normalize × (1 + overshoot).
            net.minecraft.world.phys.Vec3 distVec = ideal.subtract(seg.position());
            double distLen = distVec.length();
            float extraLength = (float) Math.max(distLen - maxDistFromFront, 0.0F);
            net.minecraft.world.phys.Vec3 step =
                    distLen > 1.0 ? distVec.normalize().scale(1.0F + extraLength) : distVec;
            net.minecraft.world.phys.Vec3 newPos = seg.position().add(step);

            // (2b) Y terrain probe — mirrors Mth.approach(getY(), min(surfaceY, idealY), 1).
            double surfaceY = computeSurfaceY(seg.level(), newPos.x, newPos.y, newPos.z);
            float settleY = (float) Math.min(surfaceY, newPos.y);
            float lerpedY = net.minecraft.util.Mth.approach((float) seg.getY(), settleY, 1.0F);
            newPos = new net.minecraft.world.phys.Vec3(newPos.x, lerpedY, newPos.z);

            // (3) Target rotation: yaw/pitch toward frontsBack = front.pos + Vec3(0,0,3).xRot(-xRot).yRot(-yRot).
            net.minecraft.world.phys.Vec3 frontsBack = prev.position().add(
                    new net.minecraft.world.phys.Vec3(0F, 0F, 3F)
                            .xRot((float) -Math.toRadians(prev.getXRot()))
                            .yRot((float) -Math.toRadians(prev.getYRot())));
            double dxr = frontsBack.x - newPos.x;
            double dyr = frontsBack.y - newPos.y;
            double dzr = frontsBack.z - newPos.z;
            double horiz = Math.sqrt(dxr * dxr + dzr * dzr);
            float targetXRot = net.minecraft.util.Mth.wrapDegrees(
                    (float) -(net.minecraft.util.Mth.atan2(dyr, horiz) * (180F / (float) Math.PI)));
            float targetYRot = net.minecraft.util.Mth.wrapDegrees(
                    (float) (net.minecraft.util.Mth.atan2(dzr, dxr) * (180F / (float) Math.PI)) - 90F);

            // Approach at limited rate — produces snake-like turning lag.
            float newXRot = net.minecraft.util.Mth.approachDegrees(seg.getXRot(), targetXRot, pitchApproachDegPerTick);
            float newYRot = net.minecraft.util.Mth.approachDegrees(seg.getYRot(), targetYRot, yawApproachDegPerTick);

            // Save prev-frame state for renderer partialTick lerp.
            seg.xo = seg.getX();
            seg.yo = seg.getY();
            seg.zo = seg.getZ();
            seg.xOld = seg.getX();
            seg.yOld = seg.getY();
            seg.zOld = seg.getZ();
            seg.yRotO = seg.getYRot();
            seg.xRotO = seg.getXRot();

            seg.setPos(newPos);
            seg.setYRot(newYRot);
            seg.setXRot(newXRot);
            seg.tickCount = tickCount;

            prev = seg;
        }
    }

    /**
     * Surface-Y probe: scan downward from y+2 for the first suffocating block, return its top.
     * Mirrors {@code GumWormSegmentEntity.calculateSurfaceY} without memoization
     * (at most 17 segments per disguise, recomputing every tick is acceptable).
     */
    private static double computeSurfaceY(net.minecraft.world.level.Level level, double x, double y, double z) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        int top = (int) Math.round(y) + 2;
        pos.set((int) Math.round(x), top, (int) Math.round(z));
        int min = level.getMinBuildHeight();
        while (pos.getY() > min && !level.getBlockState(pos).isSuffocating(level, pos)) {
            pos.move(0, -1, 0);
        }
        return 1.0 + pos.getY();
    }

    /** Discards client-side segments for the wearer; client-side gate is required because singleplayer shares the static map. */
    public static void removeSegments(LivingEntity wearer) {
        if (wearer == null) return;
        if (!wearer.level().isClientSide) return;
        removeSegmentsByUUID(wearer.getUUID());
    }

    /** Internal — assumes the caller already verified client-side context. */
    private static void removeSegmentsByUUID(UUID wearerUUID) {
        List<Entity> chain = SEGMENTS.remove(wearerUUID);
        if (chain == null) return;
        for (Entity seg : chain) {
            if (seg != null && !seg.isRemoved()) {
                seg.discard();
            }
        }
    }

    private static void initReflection(LivingEntity sample) {
        if (reflectionInited) return;
        synchronized (GumWormSegmentManager.class) {
            if (reflectionInited) return;
            // Set reflectionInited LAST so a transient failure doesn't permanently lock out retry.
            segmentType = BuiltInRegistries.ENTITY_TYPE.getOptional(GUM_WORM_SEGMENT_ID).orElse(null);
            if (segmentType == null) {
                reflectionInited = true;
                return;
            }

            // Walk the segment class hierarchy for static EntityDataAccessor fields.
            // Use a segment probe, not the sample (which is GumWormEntity, the head).
            Entity probe = segmentType.create(sample.level());
            if (probe == null) return;
            Class<?> c = probe.getClass();
            while (c != null && c != Object.class) {
                if (headEntityIdAccessor == null) headEntityIdAccessor = grabAccessor(c, "HEAD_ENTITY_ID");
                if (frontEntityIdAccessor == null) frontEntityIdAccessor = grabAccessor(c, "FRONT_ENTITY_ID");
                if (backEntityIdAccessor == null) backEntityIdAccessor = grabAccessor(c, "BACK_ENTITY_ID");
                c = c.getSuperclass();
            }
            // getMethod walks the hierarchy automatically for public methods.
            try {
                setIndexMethod = probe.getClass().getMethod("setIndex", int.class);
            } catch (ReflectiveOperationException ignored) {}
            try {
                setHeadUuidMethod = probe.getClass().getMethod("setHeadUUID", UUID.class);
            } catch (ReflectiveOperationException ignored) {}
            try {
                setFrontUuidMethod = probe.getClass().getMethod("setFrontEntityUUID", UUID.class);
            } catch (ReflectiveOperationException ignored) {}
            try {
                setBackUuidMethod = probe.getClass().getMethod("setBackEntityUUID", UUID.class);
            } catch (ReflectiveOperationException ignored) {}
            probe.discard();
            reflectionInited = true;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityDataAccessor<Integer> grabAccessor(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            Object value = f.get(null);
            if (value instanceof EntityDataAccessor<?>) {
                return (EntityDataAccessor<Integer>) value;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        return null;
    }
}
