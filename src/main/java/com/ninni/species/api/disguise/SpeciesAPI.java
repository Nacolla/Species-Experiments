package com.ninni.species.api.disguise;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

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

    /** Composed behavior for the type (per-type entry stacked over globals). Never null —
     *  returns the default no-op when nothing is registered. */
    public static DisguiseBehavior getBehavior(EntityType<?> type) {
        return com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(type);
    }

    /** Whether a per-type behavior is registered (does not count globals). */
    public static boolean hasBehavior(EntityType<?> type) {
        return com.ninni.species.server.disguise.DisguiseBehaviorRegistry.hasOverride(type);
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

    /** Composed cosmetics for the type. Never null — returns the default no-op when nothing is registered. */
    public static DisguiseCosmetics getCosmetics(EntityType<?> type) {
        return com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(type);
    }

    /** Whether per-type cosmetics are registered (does not count globals). */
    public static boolean hasCosmetics(EntityType<?> type) {
        return com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.hasOverride(type);
    }

    // ---- Wearer / disguise lookup --------------------------------------

    /** Resolve the disguise body the wearer currently has, if any. */
    public static Optional<LivingEntity> getDisguiseBody(LivingEntity wearer) {
        if (!(wearer instanceof com.ninni.species.mixin_util.LivingEntityAccess access)) return Optional.empty();
        return Optional.ofNullable(access.getDisguisedEntity());
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

    /** Vertical translate (world units) applied during in-world render. Use to lift small/flat creatures off the wearer's feet. */
    public static void setWorldYOffset(EntityType<?> type, float yOffset) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.setWorldYOffset(type, yOffset);
    }

    /** Add an extra render-pass layer for the given type. Multiple layers run in registration order. */
    public static void registerRenderLayer(EntityType<?> type, DisguiseRenderLayer layer) {
        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.registerRenderLayer(type, layer);
    }

    // ---- Chain disguises -----------------------------------------------

    /** Register a segmented-chain disguise (head + linked body parts) from a {@code ChainSpec}.
     *  Equivalent to {@code new SegmentChainManager(spec).register()}. */
    public static void registerChainDisguise(com.ninni.species.server.disguise.panacea.spec.ChainSpec spec) {
        new com.ninni.species.server.disguise.panacea.SegmentChainManager(spec).register();
    }

    /** Map chain segment types to their head so imprinting a segment can redirect to the head when
     *  the server config flag is on. {@code SegmentChainManager.register()} populates this from the
     *  ChainSpec automatically — call directly only for hardcoded chains that bypass the spec API. */
    public static void registerChainHeadMapping(EntityType<?> head, EntityType<?>... segments) {
        com.ninni.species.server.disguise.ChainHeadRegistry.register(head, segments);
    }
}
