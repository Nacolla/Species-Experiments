package com.ninni.species.mixin;

import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.mixin_util.PlayerAccess;
import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry;
import com.ninni.species.api.disguise.DisguiseCosmetics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccess {
    private @Unique int harpoonId;

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }


    public int getHarpoonId() {
        return harpoonId;
    }

    @Override
    public void setHarpoonId(int id) {
        this.harpoonId = id;
    }

    /** Cosmetic hurt-sound override; hooks {@link Player#getHurtSound} (the LivingEntity inject never fires for players because Player overrides it). */
    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void species$cosmeticHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        Player self = (Player)(Object) this;
        LivingEntity disguise = ((LivingEntityAccess) self).getDisguisedEntity();
        if (disguise == null || disguise.isRemoved()) return;
        try {
            DisguiseCosmetics cosmetics = DisguiseCosmeticRegistry.get(disguise);
            SoundEvent override = cosmetics.overrideHurtSound(self, disguise, source);
            if (override != null) cir.setReturnValue(override);
        } catch (Throwable ignored) {}
    }

    /**
     * Cosmetic death-sound override. Same rationale as {@link #species$cosmeticHurtSound}:
     * {@code Player} overrides {@code getDeathSound}, so the hook must live here.
     */
    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void species$cosmeticDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        Player self = (Player)(Object) this;
        LivingEntity disguise = ((LivingEntityAccess) self).getDisguisedEntity();
        if (disguise == null || disguise.isRemoved()) return;
        try {
            DisguiseCosmetics cosmetics = DisguiseCosmeticRegistry.get(disguise);
            SoundEvent override = cosmetics.overrideDeathSound(self, disguise);
            if (override != null) cir.setReturnValue(override);
        } catch (Throwable ignored) {}
    }
}
