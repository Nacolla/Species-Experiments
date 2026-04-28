package com.ninni.species.server.disguise.panacea;

import com.ninni.species.api.disguise.DisguiseRenderLayer;
import com.ninni.species.api.disguise.SubEntityProvider;
import com.ninni.species.server.disguise.BoundroidPairManager;
import com.ninni.species.server.disguise.GumWormSegmentManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-{@link EntityType} topology overrides consulted by {@link DisguiseTopology}:
 * sub-entity provider, inventory scale, camera-size floor, inventory Y-offset, render layers.
 * Register during {@code FMLCommonSetupEvent} or earlier.
 */
public final class DisguiseTopologyRegistry {

    private static final Map<EntityType<?>, SubEntityProvider> SUB_ENTITY_PROVIDERS = new HashMap<>();
    private static final Map<EntityType<?>, Float> INVENTORY_SCALE_OVERRIDES = new HashMap<>();
    private static final Map<EntityType<?>, Double> CAMERA_SIZE_OVERRIDES = new HashMap<>();
    private static final Map<EntityType<?>, Float> INVENTORY_Y_OFFSETS = new HashMap<>();
    private static final Map<EntityType<?>, java.util.List<DisguiseRenderLayer>> RENDER_LAYERS = new HashMap<>();

    /** Providers consulted for every disguise; each self-gates on applicability. */
    private static final java.util.List<SubEntityProvider> GLOBAL_SUB_ENTITY_PROVIDERS = new java.util.ArrayList<>();

    /** Composed provider per type. Invalidated on registration mutation. */
    private static final Map<EntityType<?>, SubEntityProvider> COMPOSED_PROVIDER_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private DisguiseTopologyRegistry() {}

    private static void invalidateProviderCache() { COMPOSED_PROVIDER_CACHE.clear(); }

    // --------------------------------------------------------------------
    // Public registration API
    // --------------------------------------------------------------------

    /** Register a {@link SubEntityProvider} for a disguise with independent client-side sub-entities. Replaces any prior registration. */
    public static void registerSubEntities(EntityType<?> type, SubEntityProvider provider) {
        if (type == null || provider == null) return;
        SUB_ENTITY_PROVIDERS.put(type, provider);
        invalidateProviderCache();
    }

    /** Register a global {@link SubEntityProvider} consulted for every disguise. Prefer this when the provider self-determines applicability. */
    public static void registerGlobalSubEntityProvider(SubEntityProvider provider) {
        if (provider == null) return;
        GLOBAL_SUB_ENTITY_PROVIDERS.add(provider);
        invalidateProviderCache();
    }

    /**
     * Override the inventory-preview scale (multiplied on top of auto-fit).
     * Use {@link #clearInventoryScale} to remove; passing {@code 1.0F} does NOT remove.
     * No-op for null type.
     */
    public static void setInventoryScale(EntityType<?> type, float scale) {
        if (type == null) return;
        INVENTORY_SCALE_OVERRIDES.put(type, scale);
    }

    /** Removes any inventory-scale override for the type; falls back to auto-fit. */
    public static void clearInventoryScale(EntityType<?> type) {
        if (type == null) return;
        INVENTORY_SCALE_OVERRIDES.remove(type);
    }

    /**
     * Pin the camera visual-size floor. Actual distance uses the larger of the computed
     * union and this floor. Use for streaming-segment disguises where the union starts small.
     */
    public static void setCameraSizeMinimum(EntityType<?> type, double minVisualSize) {
        if (type == null) return;
        CAMERA_SIZE_OVERRIDES.put(type, minVisualSize);
    }

    /** Removes any camera-size-minimum override for the type. */
    public static void clearCameraSizeMinimum(EntityType<?> type) {
        if (type == null) return;
        CAMERA_SIZE_OVERRIDES.remove(type);
    }

    /**
     * Vertical translate (world units, pre-scale) for entities whose model centroid sits
     * above the entity origin. Negative values shift DOWN before scaling, recentring the
     * visible body in the inventory frame. Pass {@code 0.0F} to clear.
     */
    public static void setInventoryYOffset(EntityType<?> type, float yOffset) {
        if (type == null) return;
        INVENTORY_Y_OFFSETS.put(type, yOffset);
    }

    /** Removes any inventory Y-offset override for the type. */
    public static void clearInventoryYOffset(EntityType<?> type) {
        if (type == null) return;
        INVENTORY_Y_OFFSETS.remove(type);
    }

    /** Adds an extra render-pass layer for the given type. Multiple layers are invoked in registration order. No-op for null args. */
    public static void registerRenderLayer(EntityType<?> type, DisguiseRenderLayer layer) {
        if (type == null || layer == null) return;
        RENDER_LAYERS.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(layer);
    }

    /** Returns the render layers for the disguise's type, or empty if none. */
    public static java.util.List<DisguiseRenderLayer> getRenderLayers(LivingEntity disguise) {
        if (disguise == null) return java.util.Collections.emptyList();
        java.util.List<DisguiseRenderLayer> layers = RENDER_LAYERS.get(disguise.getType());
        return layers != null ? layers : java.util.Collections.emptyList();
    }

    // --------------------------------------------------------------------
    // Lookup API consumed by DisguiseTopology
    // --------------------------------------------------------------------

    public static SubEntityProvider getSubEntityProvider(LivingEntity disguise) {
        if (disguise == null) return SubEntityProvider.EMPTY;
        return COMPOSED_PROVIDER_CACHE.computeIfAbsent(disguise.getType(), DisguiseTopologyRegistry::composeProvider);
    }

    private static SubEntityProvider composeProvider(EntityType<?> type) {
        SubEntityProvider perType = SUB_ENTITY_PROVIDERS.get(type);
        if (GLOBAL_SUB_ENTITY_PROVIDERS.isEmpty()) {
            return perType != null ? perType : SubEntityProvider.EMPTY;
        }
        // Capture immutable snapshots once so the cached lambda doesn't iterate the live globals list.
        SubEntityProvider[] globals = GLOBAL_SUB_ENTITY_PROVIDERS.toArray(new SubEntityProvider[0]);
        return (wearer, dis) -> {
            java.util.List<Entity> combined = null;
            if (perType != null) {
                for (Entity e : perType.getSubEntities(wearer, dis)) {
                    if (combined == null) combined = new java.util.ArrayList<>(4);
                    combined.add(e);
                }
            }
            for (SubEntityProvider g : globals) {
                for (Entity e : g.getSubEntities(wearer, dis)) {
                    if (combined == null) combined = new java.util.ArrayList<>(4);
                    combined.add(e);
                }
            }
            return combined != null ? combined : java.util.Collections.emptyList();
        };
    }

    public static boolean hasSubEntityProvider(LivingEntity disguise) {
        return disguise != null && SUB_ENTITY_PROVIDERS.containsKey(disguise.getType());
    }

    /** Whether ANY topology override is registered for the given type. */
    public static boolean hasOverride(EntityType<?> type) {
        if (type == null) return false;
        return SUB_ENTITY_PROVIDERS.containsKey(type)
                || INVENTORY_SCALE_OVERRIDES.containsKey(type)
                || CAMERA_SIZE_OVERRIDES.containsKey(type)
                || INVENTORY_Y_OFFSETS.containsKey(type);
    }

    /** Returns the inventory scale override for the disguise's type, or {@code null} if none registered. */
    @javax.annotation.Nullable
    public static Float getInventoryScaleOverride(LivingEntity disguise) {
        if (disguise == null) return null;
        return INVENTORY_SCALE_OVERRIDES.get(disguise.getType());
    }

    public static double getCameraSizeMinimum(LivingEntity disguise) {
        if (disguise == null) return 0.0;
        Double v = CAMERA_SIZE_OVERRIDES.get(disguise.getType());
        return v != null ? v : 0.0;
    }

    /** Returns the inventory Y-offset override for the disguise's type, or {@code null} if none registered. */
    @javax.annotation.Nullable
    public static Float getInventoryYOffsetOverride(LivingEntity disguise) {
        if (disguise == null) return null;
        return INVENTORY_Y_OFFSETS.get(disguise.getType());
    }

    // --------------------------------------------------------------------
    // Built-in defaults (Species' first-party disguise customisations)
    // --------------------------------------------------------------------

    /**
     * Registers Species' built-in topology overrides. Called from {@code FMLCommonSetupEvent}.
     * Soft-dep entries are no-ops if the relevant mod is absent.
     */
    public static void registerDefaults() {
        // Force-load manager classes so their static initializers register global sub-entity
        // providers. Without explicit loading, a manager never referenced at mod-init never
        // runs its static block, and its sub-entities are invisible to render dispatch.
        try {
            Class.forName(BoundroidPairManager.class.getName());
            Class.forName(GumWormSegmentManager.class.getName());
            Class.forName(com.ninni.species.server.disguise.MineGuardianAnchorManager.class.getName());
        } catch (ClassNotFoundException ignored) {}

        // Gum Worm: camera floor at 30 so framing is correct from tick 0 while segments stream in.
        ifPresent("alexscaves", "gum_worm", type -> setCameraSizeMinimum(type, 30.0));

        // Hullback: tail extends ~14 blocks along Z beyond AABB; 0.5 fits the full chain.
        ifPresent("whaleborne", "hullback", type -> setInventoryScale(type, 0.5F));

        // Forsaken: model reaches ~5 blocks above foot baseline vs 3.5-block bbHeight; 0.7 keeps head inside frame.
        ifPresent("alexscaves", "forsaken", type -> setInventoryScale(type, 0.7F));
    }

    @FunctionalInterface
    private interface TypeConsumer { void accept(EntityType<?> type); }

    private static void ifPresent(String namespace, String path, TypeConsumer action) {
        ResourceLocation id = new ResourceLocation(namespace, path);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type != null) action.accept(type);
    }
}
