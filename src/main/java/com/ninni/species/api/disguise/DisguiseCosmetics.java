package com.ninni.species.api.disguise;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * Cosmetic axes (sounds, texture, name tag, scale, glow, particles, full {@link DisguiseRenderer} override) for the
 * Wicked Mask system; all methods have no-op defaults. Third-party mods register via {@link SpeciesAPI} or IMC.
 */
public interface DisguiseCosmetics {

    /** Override the wearer's hurt sound while disguised. Return {@code null} to fall through. */
    @Nullable
    default SoundEvent overrideHurtSound(LivingEntity wearer, LivingEntity disguise, DamageSource source) {
        return null;
    }

    /** Override the wearer's death sound while disguised. Return {@code null} to fall through. */
    @Nullable
    default SoundEvent overrideDeathSound(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /** Override the wearer's ambient sound. Return {@code null} to fall through. */
    @Nullable
    default SoundEvent overrideAmbientSound(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /** Override the disguise body's render texture. Return {@code null} to use the renderer's own. */
    @Nullable
    default ResourceLocation overrideTexture(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /** Called every client tick while the disguise is active. Default is no-op. */
    default void emitParticles(LivingEntity wearer, LivingEntity disguise, Level level, RandomSource rng) {}

    /** Override the name tag shown above the disguise. Return {@code null} to fall through. */
    @Nullable
    default Component overrideNameTag(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /** Override the world-render scale (separate from inventory scale). Return {@code Float.NaN} or 0 to fall through. */
    default float overrideWorldScale(LivingEntity wearer, LivingEntity disguise) {
        return Float.NaN;
    }

    /** Override the glow-outline ARGB color. Return {@code null} to fall through. */
    @Nullable
    default Integer overrideGlowColor(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /**
     * Override the entire body-render pass. Return {@code null} to use the vanilla renderer.
     * Use only when texture/sound/layer hooks are insufficient — e.g. fully replacing the model.
     */
    @Nullable
    default DisguiseRenderer overrideRenderer(LivingEntity wearer, LivingEntity disguise) {
        return null;
    }

    /**
     * Effects applied to the wearer on equip/swap and removed on un-equip. Use
     * {@link MobEffectInstance#INFINITE_DURATION} for effects that persist while worn.
     */
    default Collection<MobEffectInstance> wearerEffectsWhileWorn(LivingEntity wearer, LivingEntity disguise) {
        return Collections.emptyList();
    }
}
