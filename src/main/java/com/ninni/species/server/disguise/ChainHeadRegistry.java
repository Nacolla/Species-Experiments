package com.ninni.species.server.disguise;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Maps chain segment {@link EntityType}s to their head so a Wicked Mask imprint of a segment
 *  redirects to the full chain. Populated by {@code SegmentChainManager.register()}; third-party
 *  hardcoded chains use {@link com.ninni.species.api.disguise.SpeciesAPI#registerChainHeadMapping}. */
public final class ChainHeadRegistry {

    private static final Map<EntityType<?>, EntityType<?>> SEGMENT_TO_HEAD = new ConcurrentHashMap<>();

    private ChainHeadRegistry() {}

    /** Registers each non-null segment as redirecting to {@code head}. Existing mappings are overwritten. */
    public static void register(EntityType<?> head, EntityType<?>... segments) {
        if (head == null || segments == null) return;
        for (EntityType<?> segment : segments) {
            if (segment == null || segment == head) continue;
            SEGMENT_TO_HEAD.put(segment, head);
        }
    }

    /** Convenience for spec-driven registration where segment IDs are {@link ResourceLocation}s; missing types skipped. */
    public static void registerByIds(ResourceLocation headId, ResourceLocation... segmentIds) {
        if (headId == null || segmentIds == null) return;
        EntityType<?> head = BuiltInRegistries.ENTITY_TYPE.getOptional(headId).orElse(null);
        if (head == null) return;
        for (ResourceLocation segmentId : segmentIds) {
            if (segmentId == null) continue;
            BuiltInRegistries.ENTITY_TYPE.getOptional(segmentId)
                    .ifPresent(seg -> { if (seg != head) SEGMENT_TO_HEAD.put(seg, head); });
        }
    }

    /** Head type to use when the player imprints {@code segment}, or empty when {@code segment} isn't a known chain part. */
    public static Optional<EntityType<?>> headFor(EntityType<?> segment) {
        if (segment == null) return Optional.empty();
        return Optional.ofNullable(SEGMENT_TO_HEAD.get(segment));
    }

    /** True when {@code segment} has a registered head — i.e. imprinting it would redirect under the config flag. */
    public static boolean isSegment(EntityType<?> segment) {
        return segment != null && SEGMENT_TO_HEAD.containsKey(segment);
    }
}
