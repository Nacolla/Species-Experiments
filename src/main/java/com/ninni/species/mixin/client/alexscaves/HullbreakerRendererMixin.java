package com.ninni.species.mixin.client.alexscaves;

import com.ninni.species.server.disguise.ModelStraightenBridge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Soft-dep mixin: in {@code HullbreakerRenderer.render}, redirects the GETFIELD of {@code sepia}
 * (the line {@code this.model.straighten = sepia;}) to return true when
 * {@link ModelStraightenBridge#FORCE_STRAIGHTEN} is set, forcing straighten without flipping sepia.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.client.render.entity.HullbreakerRenderer", remap = false)
public abstract class HullbreakerRendererMixin {

    @Shadow private boolean sepia;

    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lcom/github/alexmodguy/alexscaves/client/render/entity/HullbreakerRenderer;sepia:Z",
            opcode = Opcodes.GETFIELD
        ),
        remap = false
    )
    private boolean species$forceStraightenIfDisguise(@Coerce Object self) {
        if (ModelStraightenBridge.FORCE_STRAIGHTEN) return true;
        return this.sepia;
    }
}
