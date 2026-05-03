package com.ninni.species.mixin.client;

import com.ninni.species.mixin_util.LivingEntityAccess;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses particles near the camera while the local player is disguised in first-person. */
@OnlyIn(Dist.CLIENT)
@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    /** Radius (blocks, squared) around camera within which particles are suppressed during first-person disguise. */
    private static final double SUPPRESSION_RADIUS_SQ = 4.0 * 4.0;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void species$skipParticleInFirstPersonDisguise(VertexConsumer buffer, Camera camera, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.options == null || mc.options.getCameraType() == null
                || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        LivingEntity disguise = ((LivingEntityAccess) mc.player).getDisguisedEntity();
        if (disguise == null || disguise.isRemoved()) return;

        Particle self = (Particle) (Object) this;
        AABB box = self.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 cam = camera.getPosition();
        double dx = center.x - cam.x;
        double dy = center.y - cam.y;
        double dz = center.z - cam.z;
        if (dx * dx + dy * dy + dz * dz < SUPPRESSION_RADIUS_SQ) {
            ci.cancel();
        }
    }
}
