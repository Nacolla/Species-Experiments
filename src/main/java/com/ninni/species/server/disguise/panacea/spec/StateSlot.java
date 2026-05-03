package com.ninni.species.server.disguise.panacea.spec;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-wearer or per-segment state container for spec-driven managers — declared on the
 * {@link ChainSpec} so cleanup runs automatically when the chain is removed.
 */
public final class StateSlot<K, V> {

    public enum Scope { WEARER, SEGMENT }

    private final Scope scope;
    private final Supplier<V> initialValue;
    private final Map<K, V> data = new ConcurrentHashMap<>();

    StateSlot(Scope scope, Supplier<V> initialValue) {
        this.scope = scope;
        this.initialValue = initialValue;
    }

    public Scope scope() { return scope; }

    public V get(K key) { return data.get(key); }

    /** Returns the existing value or creates a fresh one via the initial-value supplier. */
    public V getOrCreate(K key) {
        return data.computeIfAbsent(key, k -> initialValue.get());
    }

    public void put(K key, V value) { data.put(key, value); }

    public void remove(K key) { data.remove(key); }

    public boolean containsKey(K key) { return data.containsKey(key); }

    /** Convenience helpers for the two common scopes. */
    public static <V> StateSlot<UUID, V> perWearer(Supplier<V> init) {
        return new StateSlot<>(Scope.WEARER, init);
    }

    public static <V> StateSlot<Integer, V> perSegment(Supplier<V> init) {
        return new StateSlot<>(Scope.SEGMENT, init);
    }

    /** Bulk cleanup for chain destruction — removes all entries belonging to the given keys. */
    public void removeAll(Iterable<K> keys) {
        for (K k : keys) data.remove(k);
    }

    /** Read-only key set view (for cleanup iteration). */
    public java.util.Set<K> keys() { return data.keySet(); }
}
