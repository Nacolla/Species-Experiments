package com.ninni.species.api.disguise;

import net.minecraft.world.entity.EntityType;

/** Public facade for the disguise system. Hard-dep mods call directly; soft-dep mods use Forge IMC (see {@code imc.SpeciesIMCKeys}). */
public final class SpeciesAPI {

    private SpeciesAPI() {}

    // ---- Behavior ------------------------------------------------------

    /** Register a behavior for an entity type, replacing any prior entry. */
    public static void registerBehavior(EntityType<?> type, DisguiseBehavior behavior) {
        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.register(type, behavior);
    }

    /** Compose a behavior with any existing entry for the type (existing runs first). */
    public static void composeBehavior(EntityType<?> type, DisguiseBehavior behavior) {
        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.compose(type, behavior);
    }

    /** Register a behavior applied to every disguise. The behavior should self-gate on applicability. */
    public static void registerGlobalBehavior(DisguiseBehavior behavior) {
        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.registerGlobal(behavior);
    }

    // ---- Cosmetics -----------------------------------------------------

    /** Register cosmetics for an entity type, replacing any prior entry. */
    public static void registerCosmetics(EntityType<?> type, DisguiseCosmetics cosmetics) {
        com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.register(type, cosmetics);
    }

    /** Compose cosmetics on top of any existing registration via first-non-null-wins precedence. */
    public static void composeCosmetics(EntityType<?> type, DisguiseCosmetics cosmetics) {
        com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.compose(type, cosmetics);
    }

    // ---- Topology ------------------------------------------------------

    /** Register a sub-entity provider for an entity type (independent client-side entities). */
    public static void registerSubEntityProvider(EntityType<?> type, SubEntityProvider provider) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.registerSubEntities(type, provider);
    }

    /** Register a global sub-entity provider that self-gates on applicability per disguise. */
    public static void registerGlobalSubEntityProvider(SubEntityProvider provider) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.registerGlobalSubEntityProvider(provider);
    }

    /** Override the inventory-preview scale (multiplied on top of auto-fit). */
    public static void setInventoryScale(EntityType<?> type, float scale) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.setInventoryScale(type, scale);
    }

    /** Pin the camera visual-size floor for streaming-segment disguises. */
    public static void setCameraSizeMinimum(EntityType<?> type, double minVisualSize) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.setCameraSizeMinimum(type, minVisualSize);
    }

    /** Vertical translate (world units, pre-scale) applied during inventory render. Negative shifts down. */
    public static void setInventoryYOffset(EntityType<?> type, float yOffset) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.setInventoryYOffset(type, yOffset);
    }

    /** Add an extra render-pass layer for the given type. Multiple layers run in registration order. */
    public static void registerRenderLayer(EntityType<?> type, DisguiseRenderLayer layer) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.registerRenderLayer(type, layer);
    }
}
