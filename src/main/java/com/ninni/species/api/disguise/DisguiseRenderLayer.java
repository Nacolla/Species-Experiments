package com.ninni.species.api.disguise;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Extra render pass for geometry without a world entity (outlines, auras, overlays).
 * Invoked after main body render, before sub-entity render; pose stack is at body-render state.
 */
@FunctionalInterface
public interface DisguiseRenderLayer {

    /** Render an extra layer; push/pop {@code poseStack} internally for own transforms. */
    void render(LivingEntity wearer,
                LivingEntity disguise,
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                float partialTick,
                int packedLight,
                boolean inInventory);
}
