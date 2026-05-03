package com.ninni.species.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes {@code EntityRenderer}'s protected shadow fields so cross-class mixins (and the
 *  segment-shadow loop) can read them without reflection. */
@OnlyIn(Dist.CLIENT)
@Mixin(EntityRenderer.class)
public interface EntityRendererShadowAccessor {
    @Accessor("shadowRadius")
    float species$getShadowRadius();

    @Accessor("shadowStrength")
    float species$getShadowStrength();
}
