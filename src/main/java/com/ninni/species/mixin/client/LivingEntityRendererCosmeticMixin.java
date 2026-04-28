package com.ninni.species.mixin.client;

import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticContext;
import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry;
import com.ninni.species.api.disguise.DisguiseCosmetics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks {@code LivingEntityRenderer.getRenderType} at RETURN to swap in {@link DisguiseCosmetics#overrideTexture}
 * while {@link DisguiseCosmeticContext#current()} is non-null; subclass overrides of {@code getRenderType} bypass this.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCosmeticMixin {

    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    private void species$cosmeticTextureOverride(
            LivingEntity entity,
            boolean bodyVisible,
            boolean translucent,
            boolean glowing,
            CallbackInfoReturnable<RenderType> cir) {
        LivingEntity wearer = DisguiseCosmeticContext.current();
        if (wearer == null) return; // Not inside a disguise render — fall through.

        DisguiseCosmetics cosmetics = DisguiseCosmeticRegistry.get(entity);
        ResourceLocation override = cosmetics.overrideTexture(wearer, entity);
        if (override == null) return;

        RenderType original = cir.getReturnValue();
        if (original == null) return; // Parent returned null (entity wasn't rendered) — don't synthesize a RenderType.

        // Rebuild the RenderType with the override texture, mirroring
        // LivingEntityRenderer.getRenderType branch logic to preserve
        // translucency, body-visibility, and outline variants.
        if (translucent) {
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(override));
        } else if (bodyVisible) {
            cir.setReturnValue(RenderType.entityCutoutNoCull(override));
        } else if (glowing) {
            cir.setReturnValue(RenderType.outline(override));
        }
        // Otherwise: non-standard path — leave the original return value alone.
    }
}
