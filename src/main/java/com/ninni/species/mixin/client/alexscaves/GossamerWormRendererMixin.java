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
 * Soft-dep mixin: same pattern as {@link HullbreakerRendererMixin} for {@code GossamerWormRenderer.render},
 * forcing {@code straighten=true} so {@code GossamerWormModel.setupAnim}'s trail-buffer rotations are skipped.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.client.render.entity.GossamerWormRenderer", remap = false)
public abstract class GossamerWormRendererMixin {

    @Shadow private boolean sepia;

    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lcom/github/alexmodguy/alexscaves/client/render/entity/GossamerWormRenderer;sepia:Z",
            opcode = Opcodes.GETFIELD
        ),
        remap = false
    )
    private boolean species$forceStraightenIfDisguise(@Coerce Object self) {
        if (ModelStraightenBridge.FORCE_STRAIGHTEN) return true;
        return this.sepia;
    }
}
