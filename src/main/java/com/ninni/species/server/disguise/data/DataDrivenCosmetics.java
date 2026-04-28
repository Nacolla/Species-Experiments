package com.ninni.species.server.disguise.data;

import com.ninni.species.api.disguise.DisguiseCosmetics;
import com.ninni.species.api.disguise.data.DisguiseData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Global {@link DisguiseCosmetics} that delegates each call to {@link DisguiseDataRegistry};
 * acts as a fallback layer (programmatic per-type registrations win via composite first-non-null).
 */
public final class DataDrivenCosmetics implements DisguiseCosmetics {

    public static final DataDrivenCosmetics INSTANCE = new DataDrivenCosmetics();

    private DataDrivenCosmetics() {}

    @Override
    public SoundEvent overrideHurtSound(LivingEntity wearer, LivingEntity disguise, DamageSource source) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.hurtSound() : null;
    }

    @Override
    public SoundEvent overrideDeathSound(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.deathSound() : null;
    }

    @Override
    public SoundEvent overrideAmbientSound(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.ambientSound() : null;
    }

    @Override
    public ResourceLocation overrideTexture(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.texture() : null;
    }

    @Override
    public Component overrideNameTag(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.nameTag() : null;
    }

    @Override
    public float overrideWorldScale(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null && d.worldScale() != null ? d.worldScale() : Float.NaN;
    }

    @Override
    public Integer overrideGlowColor(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null ? d.glowColor() : null;
    }

    @Override
    public java.util.Collection<net.minecraft.world.effect.MobEffectInstance> wearerEffectsWhileWorn(LivingEntity wearer, LivingEntity disguise) {
        DisguiseData d = DisguiseDataRegistry.get(disguise.getType());
        return d != null && d.wearerEffects() != null ? d.wearerEffects() : java.util.Collections.emptyList();
    }
}
