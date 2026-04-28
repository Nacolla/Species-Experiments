package com.ninni.species.mixin.client;

import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesItems;
import com.ninni.species.server.disguise.panacea.DisguiseTopology;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * {@code @ModifyArg} on {@code Camera.setup}'s {@code getMaxZoom(D)} call, scaling 3rd/2nd-person distance
 * to fit large Wicked Mask disguises; vanilla block-collision clamp still applies afterwards.
 */
@OnlyIn(Dist.CLIENT)
@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double species$adjustZoomForDisguise(double original) {
        Camera self = (Camera) (Object) this;
        Entity entity = self.getEntity();
        if (!(entity instanceof LivingEntity wearer)) return original;
        if (!wearer.getItemBySlot(EquipmentSlot.HEAD).is(SpeciesItems.WICKED_MASK.get())) return original;

        LivingEntity disguise = ((LivingEntityAccess) wearer).getDisguisedEntity();
        if (disguise == null || !disguise.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) return original;

        // Intrinsic dimensions only — sub-entity union is position-dependent, and behaviors
        // that move the disguise body (e.g. WinchDisguiseBehavior lift to ceiling) cause
        // visualSize to oscillate as wearer.y fluctuates, jittering the camera. Use bbWidth/bbHeight
        // (size, not span) plus the per-type cameraSizeMinimum floor for streaming-segment cases.
        double intrinsic = Math.max(disguise.getBbWidth(), disguise.getBbHeight());
        double pinned = com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.getCameraSizeMinimum(disguise);
        double visualSize = Math.max(intrinsic, pinned);
        if (visualSize <= DisguiseTopology.PLAYER_BASELINE) return original;

        // Linear: +1.5 blocks of distance per extra block of silhouette. Gentler than
        // a multiplicative ratio; cap at 48 so tier-5 IaF dragons and multi-segment
        // giants still fit without over-extending smaller disguises.
        double scaled = original + (visualSize - DisguiseTopology.PLAYER_BASELINE) * 1.5;
        return Math.min(scaled, 48.0);
    }
}
