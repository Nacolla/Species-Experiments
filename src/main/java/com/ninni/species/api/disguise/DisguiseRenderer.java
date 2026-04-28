package com.ninni.species.api.disguise;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Full render-pipeline override resolved via {@link DisguiseCosmetics#overrideRenderer}.
 * Replaces the vanilla renderer entirely; pose state is already applied upstream.
 */
@FunctionalInterface
public interface DisguiseRenderer {

    /** Renders the disguise body in place of the vanilla renderer; delegate to {@code baseRenderer} for selective wrapping. */
    void render(LivingEntity wearer,
                LivingEntity disguise,
                EntityRenderer<?> baseRenderer,
                float bodyYaw,
                float partialTick,
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight);
}
