package com.ninni.species.server.disguise.dsl;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;

/**
 * Multiplies {@code walkAnimation.position} (cadence/limbSwing) and {@code walkAnimation.speed} (amplitude/limbSwingAmount)
 * per render frame. Factors below 1.0 dampen mobs whose hard-coded {@code walkSpeed} looks frantic at player cadence.
 */
public final class WalkAnimationDampingBehavior implements DisguiseBehavior {

    private final float positionFactor;
    private final float speedFactor;

    /**
     * @param positionFactor multiplier for {@code walkAnimation.position} (cadence); 1.0 = unchanged.
     * @param speedFactor    multiplier for {@code walkAnimation.speed} (amplitude); 1.0 = unchanged.
     */
    public WalkAnimationDampingBehavior(float positionFactor, float speedFactor) {
        this.positionFactor = positionFactor;
        this.speedFactor = speedFactor;
    }

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (positionFactor != 1.0F) {
            disguise.walkAnimation.position *= positionFactor;
        }
        if (speedFactor != 1.0F) {
            disguise.walkAnimation.speed *= speedFactor;
        }
    }
}
