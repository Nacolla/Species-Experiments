package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tremorsaurus: delegates inventory rotation lock to {@link StraightenViaBridgeBehavior} (the model's
 * {@code faceTarget} has no straighten gate) and dampens walk-anim phase/amplitude per frame.
 */
public final class TremorsaurusBehavior implements DisguiseBehavior {

    public static final TremorsaurusBehavior INSTANCE = new TremorsaurusBehavior();

    /** Multiplied into {@code walkAnimation.position} per frame ({@code limbSwing}); reducing
     *  stretches the cycle without affecting lift height. */
    private static final float WALK_ANIM_SPEED_MULTIPLIER = 0.7F;

    /** Scales leg-lift magnitude (limbSwingAmount); player-cadence amplitude on the large model is exaggerated, 0.55 normalises it. */
    private static final float WALK_ANIM_AMPLITUDE_MULTIPLIER = 0.55F;

    private TremorsaurusBehavior() {}

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        // Re-applied per frame — syncWearerStateToDisguise resets walkAnimation.position/speed
        // to wearer values just before preRender.
        disguise.walkAnimation.position *= WALK_ANIM_SPEED_MULTIPLIER;
        disguise.walkAnimation.speed *= WALK_ANIM_AMPLITUDE_MULTIPLIER;

        // Delegate inventory rotation lock + bridge flag to shared behavior.
        StraightenViaBridgeBehavior.INSTANCE.preRender(wearer, disguise, partialTick, inInventory);
    }

    @Override
    public void postRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        StraightenViaBridgeBehavior.INSTANCE.postRender(wearer, disguise, partialTick, inInventory);
    }
}
