package com.ninni.species.mixin;

import com.ninni.species.client.events.WickedMaskDisguiseEvents;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels {@code Entity.push(Entity)} when the source is a disguise body or a part of one —
 *  catches the {@code other.push(this)} pattern that bypasses Forge events. */
@Mixin(Entity.class)
public abstract class EntityPushDisguiseMixin {

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void species$cancelPushFromDisguise(Entity source, CallbackInfo ci) {
        if (source == null) return;
        if (source.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
            ci.cancel();
            return;
        }
        if (source instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null && parent.getTags().contains(WickedMaskDisguiseEvents.DISGUISE_TAG)) {
                ci.cancel();
            }
        }
    }
}
