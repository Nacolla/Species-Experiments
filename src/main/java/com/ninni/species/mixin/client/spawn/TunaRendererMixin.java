package com.ninni.species.mixin.client.spawn;

import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Soft-dep mixin: in {@code TunaRenderer.setupRotations}, forces {@code isInWater()=true} for
 *  disguise-tagged Tunas so the on-land 90° Z-flip doesn't apply. */
@Pseudo
@Mixin(targets = "com.ninni.spawn.client.renderer.entity.TunaRenderer", remap = false)
public abstract class TunaRendererMixin {

    @Redirect(
            method = "setupRotations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWater()Z"),
            remap = true,
            require = 0
    )
    private boolean species$forceInWaterIfDisguise(Entity tuna) {
        if (tuna instanceof LivingEntity le && le.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            return true;
        }
        return tuna.isInWater();
    }
}
