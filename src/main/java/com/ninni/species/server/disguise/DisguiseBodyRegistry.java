package com.ninni.species.server.disguise;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side disguise-id → body index used as fallback by {@link com.ninni.species.mixin.client.ClientLevelMixin#getEntity}
 * since disguise bodies aren't in the level's entity registry. Also tracks wearer for soft-dep mixin gating.
 */
public final class DisguiseBodyRegistry {

    private static final Map<Integer, LivingEntity> BY_ID = new ConcurrentHashMap<>();

    /** disguise.id → wearer. Allows soft-dep mixins to gate behaviour on wearer state. */
    private static final Map<Integer, LivingEntity> WEARER_OF = new ConcurrentHashMap<>();

    private DisguiseBodyRegistry() {}

    public static void register(LivingEntity disguise, LivingEntity wearer) {
        if (disguise == null) return;
        BY_ID.put(disguise.getId(), disguise);
        if (wearer != null) WEARER_OF.put(disguise.getId(), wearer);
    }

    /** Removes the entry only if it maps to this exact instance, preventing swap-order races. */
    public static void unregister(LivingEntity disguise) {
        if (disguise == null) return;
        BY_ID.remove(disguise.getId(), disguise);
        WEARER_OF.remove(disguise.getId());
    }

    /** Returns the wearer registered alongside this disguise, or null. */
    public static LivingEntity getWearer(LivingEntity disguise) {
        return disguise == null ? null : WEARER_OF.get(disguise.getId());
    }

    /**
     * Returns the disguise body for the given entity ID, or {@code null}.
     * Lazily purges stale entries (body removed before registry was updated).
     */
    public static LivingEntity findById(int id) {
        LivingEntity disguise = BY_ID.get(id);
        if (disguise != null && disguise.isRemoved()) {
            BY_ID.remove(id, disguise);
            return null;
        }
        return disguise;
    }

    /** True iff {@code entity} is a registered disguise body (identity-checked). False for
     *  natural mobs of the same type, the wearer, or unregistered new bodies. */
    public static boolean isDisguiseBody(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        LivingEntity registered = BY_ID.get(entity.getId());
        return registered != null && registered == entity;
    }
}
