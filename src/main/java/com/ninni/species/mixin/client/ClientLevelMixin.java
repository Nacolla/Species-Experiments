package com.ninni.species.mixin.client;

import com.ninni.species.server.disguise.DisguiseBodyRegistry;
import com.ninni.species.server.disguise.DisguiseTickContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client hooks: (1) {@code getEntity(int)} fallback to {@link DisguiseBodyRegistry#findById} so id-tracked particles
 * find disguise bodies (which aren't in the level's entity registry); (2) suppress particle emission during
 * {@code disguise.tick()} for the first-person wearer (gated by {@link DisguiseTickContext#isSuppressing}).
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getEntity(I)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"), cancellable = true)
    private void species$disguiseBodyLookupFallback(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() != null) return; // standard lookup found something
        LivingEntity disguise = DisguiseBodyRegistry.findById(id);
        if (disguise != null) cir.setReturnValue(disguise);
    }

    /** Covers both {@code addParticle} overloads — the no-force one routes through this signature. */
    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void species$suppressDisguiseParticleInFirstPerson(
            ParticleOptions options, boolean force, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            CallbackInfo ci) {
        if (DisguiseTickContext.isSuppressing()) ci.cancel();
    }

    /** Same suppression for {@code addAlwaysVisibleParticle}; no-force overload also routes here. */
    @Inject(
            method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void species$suppressDisguiseAlwaysVisibleParticleInFirstPerson(
            ParticleOptions options, boolean force, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            CallbackInfo ci) {
        if (DisguiseTickContext.isSuppressing()) ci.cancel();
    }
}
