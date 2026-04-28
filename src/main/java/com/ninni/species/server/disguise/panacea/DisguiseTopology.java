package com.ninni.species.server.disguise.panacea;

import com.ninni.species.api.disguise.SubEntityProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;


/**
 * Topology-aware visual queries for disguises (SINGLE, FORGE_PARTS, SEPARATE_WORLD_ENTITIES, CUSTOM_MULTIPART).
 * Read-side facade for {@code CameraMixin}/{@code ForgeClientEvents}; consults {@link DisguiseTopologyRegistry} for overrides.
 */
public final class DisguiseTopology {

    /** Callback for {@link #forEachRenderableSubEntity}. Primitive offsets avoid per-call {@code double[]} allocation. */
    @FunctionalInterface
    public interface SubEntityRenderer {
        void accept(Entity sub, double dx, double dy, double dz);
    }


    /** Player baseline silhouette dimension — anything below this gets vanilla camera distance. */
    public static final double PLAYER_BASELINE = 2.0;

    /** Inventory preview's notional player height (matches vanilla rendering inside InventoryScreen). */
    public static final float INVENTORY_PLAYER_HEIGHT = 1.8F;

    /** Player vertical centroid above feet (0.9 = half of 1.8). Inventory frame is built around this point. */
    public static final double PLAYER_CENTROID_ABOVE_FEET = INVENTORY_PLAYER_HEIGHT * 0.5;

    /** Lower bound on the per-axis scale applied to a disguise rendered in the inventory. */
    public static final float INVENTORY_MIN_SCALE = 0.3F;

    /** Upper bound on the per-axis scale applied to a disguise rendered in the inventory. */
    public static final float INVENTORY_MAX_SCALE = 1.0F;

    private DisguiseTopology() {}

    // --------------------------------------------------------------------
    // Visual size — used by CameraMixin to pull camera back for big disguises
    // --------------------------------------------------------------------

    /** Largest dimension of the disguise silhouette (entity AABB ∪ parts ∪ sub-entities); pass null wearer to skip sub-entities. */
    public static double computeMaxVisualSize(LivingEntity disguise, LivingEntity wearer) {
        if (disguise == null) return 0.0;

        AABB box = disguise.getBoundingBox();

        // Forge multipart parts (FORGE_PARTS topology).
        if (disguise.isMultipartEntity()) {
            PartEntity<?>[] parts = disguise.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) box = box.minmax(part.getBoundingBox());
                }
            }
        }

        // Separate-world-entity sub-entities (SEPARATE_WORLD_ENTITIES topology).
        if (wearer != null) {
            SubEntityProvider provider = DisguiseTopologyRegistry.getSubEntityProvider(disguise);
            for (Entity sub : provider.getSubEntities(wearer, disguise)) {
                if (sub != null && !sub.isRemoved()) box = box.minmax(sub.getBoundingBox());
            }
        }

        double computed = Math.max(Math.max(box.getXsize(), box.getYsize()), box.getZsize());

        // Per-type floor covers streaming-segment cases where the union starts small.
        double pinned = DisguiseTopologyRegistry.getCameraSizeMinimum(disguise);
        return Math.max(computed, pinned);
    }

    // --------------------------------------------------------------------
    // Inventory scale — applied when rendering the disguise in the inventory preview
    // --------------------------------------------------------------------

    /** Auto-fit per-axis inventory scale: divides max silhouette dimension by {@link #INVENTORY_PLAYER_HEIGHT}, clamps to min/max; manual override multiplies on top. */
    public static float getInventoryScale(LivingEntity disguise) {
        if (disguise == null) return 1.0F;

        Float manual = DisguiseTopologyRegistry.getInventoryScaleOverride(disguise);
        float maxPlayer = Math.max(INVENTORY_PLAYER_HEIGHT, 0.6F);
        float maxDimension;

        if (manual != null) {
            // Manual overrides were tuned against max(bbH,bbW); use the same heuristic
            // so multiplying the override on top preserves the intended result.
            maxDimension = Math.max(disguise.getBbHeight(), disguise.getBbWidth());
        } else if (disguise.isMultipartEntity()) {
            // Union parts so wings/tails extending beyond the main AABB are accounted for.
            AABB box = disguise.getBoundingBox();
            PartEntity<?>[] parts = disguise.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) box = box.minmax(part.getBoundingBox());
                }
            }
            maxDimension = (float) Math.max(Math.max(box.getXsize(), box.getYsize()), box.getZsize());
        } else {
            // Z is the depth axis in inventory (barely visible); max(bbH,bbW) suffices.
            maxDimension = Math.max(disguise.getBbHeight(), disguise.getBbWidth());
        }

        float genericScale;
        if (maxDimension > maxPlayer) {
            genericScale = Mth.clamp(maxPlayer / maxDimension, INVENTORY_MIN_SCALE, INVENTORY_MAX_SCALE);
        } else {
            genericScale = 1.0F;
        }

        return manual != null ? genericScale * manual : genericScale;
    }

    /** Manual-only pre-scale Y translate (negative = down) for inventory; register via {@link DisguiseTopologyRegistry#setInventoryYOffset}. */
    public static float getInventoryYOffset(LivingEntity disguise) {
        if (disguise == null) return 0.0F;
        Float manual = DisguiseTopologyRegistry.getInventoryYOffsetOverride(disguise);
        return manual != null ? manual : 0.0F;
    }

    // --------------------------------------------------------------------
    // Sub-entity render iteration — replaces inline Gum Worm + Boundroid loops
    // --------------------------------------------------------------------

    /** Iterates sub-entities for manual render dispatch; callback receives sub + offset relative to wearer. */
    public static void forEachRenderableSubEntity(
            LivingEntity wearer,
            LivingEntity disguise,
            SubEntityRenderer consumer) {
        if (wearer == null || disguise == null || consumer == null) return;
        SubEntityProvider provider = DisguiseTopologyRegistry.getSubEntityProvider(disguise);
        for (Entity sub : provider.getSubEntities(wearer, disguise)) {
            if (sub == null || sub.isRemoved()) continue;
            consumer.accept(sub,
                    sub.getX() - wearer.getX(),
                    sub.getY() - wearer.getY(),
                    sub.getZ() - wearer.getZ());
        }
    }
}
