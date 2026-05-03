package com.ninni.species.server.disguise.dsl;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Per-tick driver for {@code setterName(boolean)} flags on the disguise, each fed by a wearer
 *  predicate. Replaces flag-writes the cleared AI goals would have produced; missing setters
 *  silently skip. */
public final class ReflectiveBooleanFlagBehavior implements DisguiseBehavior {

    /** Flag entry: setter name (public {@code boolean} method) + wearer predicate. */
    public record FlagSync(String setterName, Predicate<LivingEntity> wearerPredicate) {}

    private final FlagSync[] flags;

    /** Per-disguise-class resolved setters; the same instance can serve multiple types. */
    private final Map<Class<?>, Method[]> resolved = new ConcurrentHashMap<>();

    /** Flags invoked in the order given; at least one required. */
    public ReflectiveBooleanFlagBehavior(FlagSync... flags) {
        if (flags == null || flags.length == 0) {
            throw new IllegalArgumentException("ReflectiveBooleanFlagBehavior needs at least one flag");
        }
        this.flags = flags.clone();
    }

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        Method[] setters = resolved.computeIfAbsent(disguise.getClass(), this::resolveFor);
        for (int i = 0; i < flags.length; i++) {
            Method m = setters[i];
            if (m == null) continue;
            try {
                m.invoke(disguise, flags[i].wearerPredicate.test(wearer));
            } catch (ReflectiveOperationException ignored) {
                // Swallowed to keep the tick loop alive.
            }
        }
    }

    private Method[] resolveFor(Class<?> entityClass) {
        Method[] setters = new Method[flags.length];
        for (int i = 0; i < flags.length; i++) {
            try {
                setters[i] = entityClass.getMethod(flags[i].setterName, boolean.class);
            } catch (NoSuchMethodException ignored) {
                // Leave null; other flags still resolve.
            }
        }
        return setters;
    }
}
