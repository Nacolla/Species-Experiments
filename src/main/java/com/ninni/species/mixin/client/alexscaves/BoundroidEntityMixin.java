package com.ninni.species.mixin.client.alexscaves;

import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft-dep mixin: cancels {@code BoundroidEntity.tick} on disguise-tagged bodies (its Winch-spawn block
 * would duplicate {@link com.ninni.species.server.disguise.BoundroidPairManager}'s) and syncs only animation state.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.BoundroidEntity", remap = false)
public abstract class BoundroidEntityMixin {

    /** {@code groundProgress} drives the float/stand pose blend; must be updated manually since the full tick is cancelled. */
    @Shadow private float groundProgress;
    @Shadow private float prevGroundProgress;

    // method = {"tick", "m_8119_"} array + remap=false: AC isn't on the compile
    // classpath so the AP can't resolve BoundroidEntity.tick's inheritance and writes
    // no refmap entry. Listing both the Mojang name and the 1.20.1 SRG name lets
    // whichever matches the runtime class succeed without consulting a missing refmap.
    @Inject(
        method = {"tick", "m_8119_"},
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void species$skipTickForDisguise(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            // Replicate only animation-state lines from BoundroidEntity.tick (lines 105-112).
            // Winch creation, slamming, target tracking skipped: Winch spawn duplicates
            // BoundroidPairManager's Winch; disguise body has no AI targets.
            this.prevGroundProgress = this.groundProgress;
            if (self.onGround() && this.groundProgress < 5F) {
                this.groundProgress++;
            }
            if (!self.onGround() && this.groundProgress > 0F) {
                this.groundProgress--;
            }
            ci.cancel();
        }
    }
}
