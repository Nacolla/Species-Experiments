package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.api.util.PerSideStateMap;
import com.ninni.species.server.entity.util.SpeciesPose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/** Ghoul disguise: keybind drives the SEARCH alert for 200t. Tracked via {@code Level.getGameTime}
 *  (side-consistent) since {@code disguise.tickCount} is rewritten per-frame by the render pipeline. */
public final class GhoulSpecialActionBehavior implements DisguiseBehavior {

    public static final GhoulSpecialActionBehavior INSTANCE = new GhoulSpecialActionBehavior();

    /** Matches {@code SearchingGoal.start}: {@code searchTimer = 200}. */
    private static final long SEARCH_DURATION_TICKS = 200L;

    private static final class State {
        long firedGameTime;
        boolean active;
    }

    private final PerSideStateMap<State> states = new PerSideStateMap<>();

    private GhoulSpecialActionBehavior() {}

    @Override
    public void onSpecialAction(LivingEntity wearer, LivingEntity disguise,
                                com.ninni.species.api.disguise.ActionContext context) {
        Pose searching = SpeciesPose.SEARCHING.get();
        // Force STANDING -> SEARCHING so onSyncedDataUpdated dispatches even when a previous
        // press is still mid-window on this side.
        if (disguise.getPose() == searching) {
            disguise.setPose(Pose.STANDING);
        }
        disguise.setPose(searching);
        State st = states.computeIfAbsent(wearer, State::new);
        st.firedGameTime = wearer.level().getGameTime();
        st.active = true;
    }

    @Override
    public void postTick(LivingEntity wearer, LivingEntity disguise) {
        State st = states.get(wearer);
        if (st == null || !st.active) return;
        if (wearer.level().getGameTime() - st.firedGameTime < SEARCH_DURATION_TICKS) return;
        if (disguise.getPose() == SpeciesPose.SEARCHING.get()) {
            disguise.setPose(Pose.STANDING);
        }
        st.active = false;
    }

    @Override
    public void onDestroyed(LivingEntity wearer, LivingEntity disguise) {
        states.remove(wearer);
    }
}
