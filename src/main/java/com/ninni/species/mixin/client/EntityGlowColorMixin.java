package com.ninni.species.mixin.client;

import com.ninni.species.api.disguise.DisguiseCosmetics;
import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesItems;
import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Override the glow-outline color for disguised wearers. */
@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void species$disguiseGlowColorOverride(CallbackInfoReturnable<Integer> cir) {
        if (!(((Object) this) instanceof LivingEntity wearer)) return;
        if (!wearer.getItemBySlot(EquipmentSlot.HEAD).is(SpeciesItems.WICKED_MASK.get())) return;
        LivingEntity disguise = ((LivingEntityAccess) wearer).getDisguisedEntity();
        if (disguise == null || disguise.isRemoved()) return;

        DisguiseCosmetics cosmetics = DisguiseCosmeticRegistry.get(disguise);
        Integer override = cosmetics.overrideGlowColor(wearer, disguise);
        if (override != null) cir.setReturnValue(override);
    }
}
