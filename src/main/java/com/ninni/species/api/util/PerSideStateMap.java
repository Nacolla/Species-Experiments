package com.ninni.species.api.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-wearer state, separated server- vs. client-side. Singleplayer ticks both sides in the
 * same JVM; a single map would let whichever side ticks second overwrite the other's edge work.
 */
public final class PerSideStateMap<T> {

    private final Map<UUID, T> serverStates = new ConcurrentHashMap<>();
    private final Map<UUID, T> clientStates = new ConcurrentHashMap<>();

    public Map<UUID, T> side(boolean clientSide) {
        return clientSide ? clientStates : serverStates;
    }

    public Map<UUID, T> side(LivingEntity wearer) {
        return side(wearer.level().isClientSide);
    }

    public T computeIfAbsent(LivingEntity wearer, Supplier<T> factory) {
        return side(wearer).computeIfAbsent(wearer.getUUID(), k -> factory.get());
    }

    public T get(LivingEntity wearer) {
        return side(wearer).get(wearer.getUUID());
    }

    /** Drop the wearer's state from both sides. Call from {@code onDestroyed}. */
    public void remove(LivingEntity wearer) {
        if (wearer == null) return;
        UUID id = wearer.getUUID();
        serverStates.remove(id);
        clientStates.remove(id);
    }
}
