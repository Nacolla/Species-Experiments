package com.ninni.species.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes {@code EntityRenderDispatcher}'s {@code private static renderShadow} so the segment
 *  loop can draw shadows for chain parts that bypass the dispatcher's normal render path. */
@OnlyIn(Dist.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherShadowAccessor {
    @Invoker("renderShadow")
    static void species$renderShadow(PoseStack poseStack, MultiBufferSource buffer,
                                     Entity entity, float weight, float partialTick,
                                     LevelReader level, float radius) {
        throw new AssertionError("Mixin invoker stub");
    }
}
