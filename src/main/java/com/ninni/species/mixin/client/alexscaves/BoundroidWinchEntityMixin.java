package com.ninni.species.mixin.client.alexscaves;

import com.ninni.species.server.disguise.BoundroidPairManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft-dep mixin: redirects {@code BoundroidWinchEntity.getHead()} to the partner BoundroidEntity from
 * {@link BoundroidPairManager#getPartnerForWinch} so {@code getChainTo}'s instanceof BoundroidEntity branch fires for disguises.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.BoundroidWinchEntity", remap = false)
public abstract class BoundroidWinchEntityMixin {

    @Inject(method = "getHead", at = @At("HEAD"), cancellable = true, remap = false)
    private void species$getHeadDisguiseRedirect(CallbackInfoReturnable<Entity> cir) {
        Entity self = (Entity) (Object) this;
        Entity partner = BoundroidPairManager.getPartnerForWinch(self.getUUID());
        if (partner != null) {
            cir.setReturnValue(partner);
        }
    }
}
