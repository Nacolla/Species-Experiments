package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.api.util.PerSideStateMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;

/** Vanilla Bat disguise: ground/flying/ceiling postures from wearer surroundings, with a
 *  Y-offset lerp so transitions read as motion rather than teleports. */
public final class BatBehavior implements DisguiseBehavior {

    public static final BatBehavior INSTANCE = new BatBehavior();

    private static final int TRANSITION_TICKS = 10;
    /** Delay ticks before flipping the model upside-down on ceiling enter. */
    private static final int CEILING_FLIP_DELAY_TICKS = 10;

    private enum BatState { GROUND, FLYING, CEILING }

    private static final class WearerState {
        BatState target = BatState.FLYING;
        float currentYOffset = 0f;
        float targetYOffset = 0f;
        int transitionRemaining = 0;
        int ceilingFlipDelay = 0;
    }

    private final PerSideStateMap<WearerState> states = new PerSideStateMap<>();

    private BatBehavior() {}

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        if (!(disguise instanceof Bat bat)) return;

        WearerState st = states.computeIfAbsent(wearer, WearerState::new);
        BatState target = detectState(wearer);
        if (target != st.target) {
            boolean enteringCeiling = target == BatState.CEILING && st.target != BatState.CEILING;
            st.target = target;
            st.targetYOffset = computeYOffset(target, wearer, disguise);
            st.transitionRemaining = TRANSITION_TICKS;
            // Entering CEILING delays the upside-down flip; leaving it flips upright immediately.
            st.ceilingFlipDelay = enteringCeiling ? CEILING_FLIP_DELAY_TICKS : 0;
        }

        if (st.transitionRemaining > 0) {
            float step = 1f / st.transitionRemaining;
            st.currentYOffset += (st.targetYOffset - st.currentYOffset) * step;
            st.transitionRemaining--;
            if (st.transitionRemaining == 0) {
                st.currentYOffset = st.targetYOffset;
            }
        }

        if (st.ceilingFlipDelay > 0) st.ceilingFlipDelay--;
        bat.setResting(st.target == BatState.CEILING && st.ceilingFlipDelay <= 0);
    }

    @Override
    public float renderYOffset(LivingEntity wearer, LivingEntity disguise, float partialTick) {
        WearerState st = states.get(wearer);
        return st != null ? st.currentYOffset : 0f;
    }

    @Override
    public void onDestroyed(LivingEntity wearer, LivingEntity disguise) {
        states.remove(wearer);
    }

    /** Ceiling takes precedence over on-ground; otherwise on-ground vs. mid-air. */
    private static BatState detectState(LivingEntity wearer) {
        BlockPos above = wearer.blockPosition().above((int) Math.ceil(wearer.getBbHeight()));
        if (wearer.level().getBlockState(above).isSolid()) return BatState.CEILING;
        if (wearer.onGround()) return BatState.GROUND;
        return BatState.FLYING;
    }

    /** Render Y offset relative to wearer feet: 0 on ground, centered in bbox flying, top of bbox on ceiling. */
    private static float computeYOffset(BatState state, LivingEntity wearer, LivingEntity disguise) {
        return switch (state) {
            case GROUND -> 0f;
            case FLYING -> (wearer.getBbHeight() - disguise.getBbHeight()) * 0.5f;
            case CEILING -> wearer.getBbHeight() - disguise.getBbHeight();
        };
    }
}
