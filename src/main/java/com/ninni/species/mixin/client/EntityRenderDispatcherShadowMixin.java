package com.ninni.species.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesItems;
import com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Swaps the player's shadow size/strength to match the worn disguise — so a Stickbug
 *  disguise drops a stickbug-sized shadow, not a player-shaped one. Targets the field reads
 *  of {@code shadowRadius}/{@code shadowStrength} inside {@code render}. */
@OnlyIn(Dist.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherShadowMixin {

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shadowRadius:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD)
    )
    private float species$swapShadowRadius(EntityRenderer<?> renderer,
                                           Entity entity, double x, double y, double z,
                                           float yaw, float partialTick,
                                           PoseStack poseStack, MultiBufferSource buffer,
                                           int packedLight) {
        LivingEntity disguise = species$disguiseFor(entity);
        if (disguise == null) return ((EntityRendererShadowAccessor) renderer).species$getShadowRadius();
        Float override = DisguiseTopologyRegistry.getShadowRadiusOverride(disguise);
        if (override != null) return override;
        EntityRenderer<?> dr = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(disguise);
        return dr != null
                ? ((EntityRendererShadowAccessor) dr).species$getShadowRadius()
                : ((EntityRendererShadowAccessor) renderer).species$getShadowRadius();
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shadowStrength:F",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD)
    )
    private float species$swapShadowStrength(EntityRenderer<?> renderer,
                                             Entity entity, double x, double y, double z,
                                             float yaw, float partialTick,
                                             PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight) {
        LivingEntity disguise = species$disguiseFor(entity);
        if (disguise == null) return ((EntityRendererShadowAccessor) renderer).species$getShadowStrength();
        Float override = DisguiseTopologyRegistry.getShadowStrengthOverride(disguise);
        if (override != null) return override;
        EntityRenderer<?> dr = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(disguise);
        return dr != null
                ? ((EntityRendererShadowAccessor) dr).species$getShadowStrength()
                : ((EntityRendererShadowAccessor) renderer).species$getShadowStrength();
    }

    /** Same gating as {@code ForgeClientEvents.livingEntityRenderer}. Returns the disguise body,
     *  or null when {@code entity} isn't a disguise-wearing player. */
    private static LivingEntity species$disguiseFor(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        if (!(living instanceof LivingEntityAccess access)) return null;
        ItemStack head = living.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(SpeciesItems.WICKED_MASK.get())) return null;
        if (!head.hasTag() || head.getTag() == null || !head.getTag().contains("id")) return null;
        if (living.hasEffect(MobEffects.INVISIBILITY)) return null;
        if (living instanceof Player p && p.isSpectator()) return null;
        LivingEntity disguise = access.getDisguisedEntity();
        if (disguise == null || disguise.isRemoved() || disguise.getType() == null) return null;
        return disguise;
    }
}
