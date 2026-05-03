package com.ninni.species.api.disguise.imc;

import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#SET_SHADOW_OVERRIDE}. {@code radius} is the dark-patch
 *  size; pass {@link Float#NaN} to skip the radius and only set strength (or vice-versa). */
public record ShadowOverrideEntry(EntityType<?> type, float radius, float strength) {

    /** Convenience: only override the radius; leave strength at the renderer's intrinsic. */
    public static ShadowOverrideEntry radius(EntityType<?> type, float radius) {
        return new ShadowOverrideEntry(type, radius, Float.NaN);
    }

    /** Convenience: only override the strength. */
    public static ShadowOverrideEntry strength(EntityType<?> type, float strength) {
        return new ShadowOverrideEntry(type, Float.NaN, strength);
    }
}
