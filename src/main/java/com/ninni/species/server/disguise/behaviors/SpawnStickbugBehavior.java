package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.api.util.ReflectionHelper;
import com.ninni.species.api.util.PerSideStateMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/** Spawn Stickbug disguise: wearer crouch toggles mob ↔ stick-item form. We pin
 *  {@code HidingCooldown} high so the natural auto-toggle never fires, then drive
 *  {@code hide}/{@code stopHiding} ourselves on the crouch edge. */
public final class SpawnStickbugBehavior implements DisguiseBehavior {

    public static final SpawnStickbugBehavior INSTANCE = new SpawnStickbugBehavior();

    private static final int COOLDOWN_SUPPRESS = 1_000_000;

    private static volatile boolean reflectionInited;
    private static Method setItemMethod;
    private static Method hideMethod;
    private static Method stopHidingMethod;
    private static Method setHidingCooldownMethod;

    private static final class State {
        boolean crouching;
    }

    private final PerSideStateMap<State> states = new PerSideStateMap<>();

    private SpawnStickbugBehavior() {}

    @Override
    public void onCreated(LivingEntity wearer, LivingEntity disguise) {
        initReflection(disguise.getClass());
        if (setItemMethod != null) {
            try { setItemMethod.invoke(disguise, ItemStack.EMPTY); }
            catch (ReflectiveOperationException ignored) {}
        }
        if (setHidingCooldownMethod != null) {
            try { setHidingCooldownMethod.invoke(disguise, COOLDOWN_SUPPRESS); }
            catch (ReflectiveOperationException ignored) {}
        }
    }

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        initReflection(disguise.getClass());

        if (setHidingCooldownMethod != null) {
            try { setHidingCooldownMethod.invoke(disguise, COOLDOWN_SUPPRESS); }
            catch (ReflectiveOperationException ignored) {}
        }

        boolean crouchingNow = wearer.isCrouching();
        State st = states.computeIfAbsent(wearer, State::new);
        if (crouchingNow == st.crouching) return;

        if (crouchingNow) {
            // hide(): item=STICK + particles + transform sound.
            if (hideMethod != null) {
                try { hideMethod.invoke(disguise); }
                catch (ReflectiveOperationException ignored) {}
            }
        } else {
            // stopHiding(null, false): item=EMPTY + particles + sound. null skips the jump-kick
            // push; false skips the SCARED sound.
            if (stopHidingMethod != null) {
                try { stopHidingMethod.invoke(disguise, (Vec3) null, false); }
                catch (ReflectiveOperationException ignored) {}
            }
        }
        st.crouching = crouchingNow;
    }

    @Override
    public void onDestroyed(LivingEntity wearer, LivingEntity disguise) {
        states.remove(wearer);
    }

    private static void initReflection(Class<?> entityClass) {
        if (reflectionInited) return;
        synchronized (SpawnStickbugBehavior.class) {
            if (reflectionInited) return;
            setItemMethod = ReflectionHelper.publicMethod(entityClass, "setItem", ItemStack.class);
            hideMethod = ReflectionHelper.publicMethod(entityClass, "hide");
            stopHidingMethod = ReflectionHelper.publicMethod(entityClass, "stopHiding", Vec3.class, boolean.class);
            setHidingCooldownMethod = ReflectionHelper.publicMethod(entityClass, "setHidingCooldown", int.class);
            reflectionInited = true;
        }
    }
}
