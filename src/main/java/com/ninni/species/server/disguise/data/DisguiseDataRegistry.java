package com.ninni.species.server.disguise.data;

import com.ninni.species.api.disguise.data.DisguiseData;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Parsed {@link DisguiseData} entries from datapack JSON, swapped atomically on reload.
 * Read by {@link DataDrivenCosmetics} and the topology bridge.
 */
public final class DisguiseDataRegistry {

    private static final AtomicReference<Map<EntityType<?>, DisguiseData>> BY_TYPE =
            new AtomicReference<>(Collections.emptyMap());

    private DisguiseDataRegistry() {}

    /** Atomic swap of the full snapshot. */
    public static void replaceAll(Map<EntityType<?>, DisguiseData> snapshot) {
        BY_TYPE.set(snapshot == null ? Collections.emptyMap() : Map.copyOf(snapshot));
    }

    public static Map<EntityType<?>, DisguiseData> view() {
        return new HashMap<>(BY_TYPE.get());
    }

    @Nullable
    public static DisguiseData get(EntityType<?> type) {
        return type == null ? null : BY_TYPE.get().get(type);
    }
}
