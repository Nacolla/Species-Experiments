package com.ninni.species.server.disguise.cosmetic;

import com.ninni.species.api.disguise.DisguiseCosmetics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-{@link EntityType} {@link DisguiseCosmetics} lookup, falling back to {@link DefaultDisguiseCosmetics}.
 * Registration during mod setup. Independent of behavior and topology registries.
 */
public final class DisguiseCosmeticRegistry {

    private static final Map<EntityType<?>, DisguiseCosmetics> BY_TYPE = new HashMap<>();

    /** Fallback layer beneath per-type entries; first-non-null-wins precedence. */
    private static final java.util.List<DisguiseCosmetics> GLOBAL_COSMETICS = new java.util.ArrayList<>();

    /** Composed result per type. Invalidated on every registration mutation. */
    private static final Map<EntityType<?>, DisguiseCosmetics> COMPOSED_CACHE = new ConcurrentHashMap<>();

    private DisguiseCosmeticRegistry() {}

    private static void invalidateCache() { COMPOSED_CACHE.clear(); }

    /** Replace any existing entry. Use {@link #compose} to stack instead. */
    public static void register(EntityType<?> type, DisguiseCosmetics cosmetics) {
        if (type == null || cosmetics == null) return;
        BY_TYPE.put(type, cosmetics);
        invalidateCache();
    }

    /** Stack on top of any existing entry via {@link CompositeDisguiseCosmetics}. */
    public static void compose(EntityType<?> type, DisguiseCosmetics cosmetics) {
        if (type == null || cosmetics == null) return;
        DisguiseCosmetics existing = BY_TYPE.get(type);
        if (existing == null) {
            BY_TYPE.put(type, cosmetics);
        } else {
            BY_TYPE.put(type, CompositeDisguiseCosmetics.of(existing, cosmetics));
        }
        invalidateCache();
    }

    /** Add to {@link #GLOBAL_COSMETICS}. */
    public static void registerGlobal(DisguiseCosmetics cosmetics) {
        if (cosmetics == null) return;
        GLOBAL_COSMETICS.add(cosmetics);
        invalidateCache();
    }

    /** Composed result for the disguise's type, never null. */
    public static DisguiseCosmetics get(LivingEntity disguise) {
        if (disguise == null) return DefaultDisguiseCosmetics.INSTANCE;
        return COMPOSED_CACHE.computeIfAbsent(disguise.getType(), DisguiseCosmeticRegistry::compute);
    }

    private static DisguiseCosmetics compute(EntityType<?> type) {
        DisguiseCosmetics perType = BY_TYPE.get(type);
        if (GLOBAL_COSMETICS.isEmpty()) {
            return perType != null ? perType : DefaultDisguiseCosmetics.INSTANCE;
        }
        int size = GLOBAL_COSMETICS.size() + (perType != null ? 1 : 0);
        DisguiseCosmetics[] all = new DisguiseCosmetics[size];
        int i = 0;
        if (perType != null) all[i++] = perType;
        for (DisguiseCosmetics g : GLOBAL_COSMETICS) all[i++] = g;
        return CompositeDisguiseCosmetics.of(all);
    }

    public static boolean hasOverride(EntityType<?> type) {
        return type != null && BY_TYPE.containsKey(type);
    }

    /** Called from {@code FMLCommonSetupEvent}. Empty by default; downstream mods register cosmetics here. */
    public static void registerDefaults() {
    }
}
