package com.ninni.species.mixin.alexscaves;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Suppresses VESPER_FLAP for disguise-tagged Vesper bodies. Soft-dep, fail silent. */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity", remap = false)
public abstract class EntityVesperMixin {

    @WrapWithCondition(
            method = "m_8119_",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexmodguy/alexscaves/server/entity/living/VesperEntity;m_216990_(Lnet/minecraft/sounds/SoundEvent;)V",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private boolean species$suppressFlapForDisguise(@Coerce Object self, SoundEvent sound) {
        if (self instanceof LivingEntity disguise && disguise.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            LivingEntity wearer = com.ninni.species.server.disguise.DisguiseBodyRegistry.getWearer(disguise);
            if (wearer != null && com.ninni.species.server.disguise.dsl.WearerPredicates.CEILING_HANGING.test(wearer)) {
                return false;
            }
        }
        return true;
    }
}
