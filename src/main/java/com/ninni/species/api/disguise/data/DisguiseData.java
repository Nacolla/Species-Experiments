package com.ninni.species.api.disguise.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Parsed datapack entry for a disguise. All fields are optional.
 * Loaded from {@code data/<namespace>/species_disguises/<entity_path>.json}.
 */
public record DisguiseData(
        @Nullable ResourceLocation texture,
        @Nullable SoundEvent hurtSound,
        @Nullable SoundEvent deathSound,
        @Nullable SoundEvent ambientSound,
        @Nullable Component nameTag,
        @Nullable Float worldScale,
        @Nullable Integer glowColor,
        @Nullable Float inventoryScale,
        @Nullable Float inventoryYOffset,
        @Nullable Double cameraSizeMin,
        List<MobEffectInstance> wearerEffects
) {
    /** Convenience constructor for entries that don't apply wearer effects. */
    public DisguiseData(@Nullable ResourceLocation texture, @Nullable SoundEvent hurtSound,
                        @Nullable SoundEvent deathSound, @Nullable SoundEvent ambientSound,
                        @Nullable Component nameTag, @Nullable Float worldScale,
                        @Nullable Integer glowColor, @Nullable Float inventoryScale,
                        @Nullable Float inventoryYOffset, @Nullable Double cameraSizeMin) {
        this(texture, hurtSound, deathSound, ambientSound, nameTag, worldScale, glowColor,
                inventoryScale, inventoryYOffset, cameraSizeMin, Collections.emptyList());
    }

    /** True if any cosmetic axis is set. Topology axes are not counted. */
    public boolean hasAnyCosmetic() {
        return texture != null || hurtSound != null || deathSound != null || ambientSound != null
                || nameTag != null || worldScale != null || glowColor != null
                || (wearerEffects != null && !wearerEffects.isEmpty());
    }
}
