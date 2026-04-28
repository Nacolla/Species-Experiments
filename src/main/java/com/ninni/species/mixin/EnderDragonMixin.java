package com.ninni.species.mixin;

import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Cancels {@code EnderDragon#knockBack(List)} and {@code hurt(List)} on disguise-tagged dragons; their direct {@code entity.push} bypasses Forge events. */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {

    @Inject(method = "knockBack(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void species$cancelKnockBackForDisguise(List<Entity> entities, CallbackInfo ci) {
        if (((Entity) (Object) this).getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            ci.cancel();
        }
    }

    @Inject(method = "hurt(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void species$cancelHurtListForDisguise(List<Entity> entities, CallbackInfo ci) {
        if (((Entity) (Object) this).getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            ci.cancel();
        }
    }
}
