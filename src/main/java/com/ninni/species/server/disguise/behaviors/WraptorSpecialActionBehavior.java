package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.api.util.ReflectionHelper;
import com.ninni.species.registry.SpeciesSoundEvents;
import com.ninni.species.server.disguise.util.DisguiseSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.lang.reflect.Field;

/** Wraptor disguise: keybind drives the ROARING pose; sound is played explicitly since the
 *  pose-only path skips it. {@code onCreated} backfills {@code timeSinceSheared} so
 *  {@code random.nextInt((int)(gameTime - 0L))} doesn't overflow on long-running worlds. */
public final class WraptorSpecialActionBehavior implements DisguiseBehavior {

    public static final WraptorSpecialActionBehavior INSTANCE = new WraptorSpecialActionBehavior();

    private static volatile boolean reflectionInited;
    private static Field timeSinceShearedField;

    private WraptorSpecialActionBehavior() {}

    @Override
    public void onCreated(LivingEntity wearer, LivingEntity disguise) {
        initReflection(disguise.getClass());
        if (timeSinceShearedField == null) return;
        try {
            long current = timeSinceShearedField.getLong(disguise);
            if (current == 0L) timeSinceShearedField.setLong(disguise, disguise.level().getGameTime());
        } catch (IllegalAccessException ignored) {}
    }

    @Override
    public void onSpecialAction(LivingEntity wearer, LivingEntity disguise,
                                com.ninni.species.api.disguise.ActionContext context) {
        if (disguise.getPose() == Pose.ROARING) return;
        disguise.setPose(Pose.ROARING);
        DisguiseSounds.playServerBroadcast(wearer, SpeciesSoundEvents.WRAPTOR_AGGRO.get(), 3.0F);
    }

    private static void initReflection(Class<?> entityClass) {
        if (reflectionInited) return;
        synchronized (WraptorSpecialActionBehavior.class) {
            if (reflectionInited) return;
            timeSinceShearedField = ReflectionHelper.declaredFieldOfType(entityClass, "timeSinceSheared", long.class);
            reflectionInited = true;
        }
    }
}
