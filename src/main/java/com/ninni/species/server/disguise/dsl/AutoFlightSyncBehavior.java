package com.ninni.species.server.disguise.dsl;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.server.disguise.DisguiseBehaviorRegistry;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Auto-discovers conventional flight setters ({@code setFlying}/{@code setHovering}/{@code setHanging}/{@code setLatched})
 * on disguise classes and writes each per tick from {@link WearerPredicates}; self-registers as global behavior.
 */
public final class AutoFlightSyncBehavior implements DisguiseBehavior {

    public static final AutoFlightSyncBehavior INSTANCE = new AutoFlightSyncBehavior();

    static {
        DisguiseBehaviorRegistry.registerGlobal(INSTANCE);
    }

    private record FlightSetter(Method method, Predicate<LivingEntity> predicate) {}

    private static final FlightSetter[] EMPTY = new FlightSetter[0];

    private static final Map<Class<?>, FlightSetter[]> RESOLVED = new ConcurrentHashMap<>();

    private AutoFlightSyncBehavior() {}

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        FlightSetter[] setters = RESOLVED.computeIfAbsent(disguise.getClass(), AutoFlightSyncBehavior::resolve);
        if (setters.length == 0) return;
        for (FlightSetter s : setters) {
            try {
                s.method.invoke(disguise, s.predicate.test(wearer));
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    private static FlightSetter[] resolve(Class<?> entityClass) {
        Method setFlying = findMethod(entityClass, "setFlying");
        Method setHovering = findMethod(entityClass, "setHovering");
        Method setHanging = findMethod(entityClass, "setHanging");
        Method setLatched = findMethod(entityClass, "setLatched");

        int count = (setFlying != null ? 1 : 0) + (setHovering != null ? 1 : 0)
                + (setHanging != null ? 1 : 0) + (setLatched != null ? 1 : 0);
        if (count == 0) return EMPTY;

        FlightSetter[] result = new FlightSetter[count];
        int i = 0;
        if (setFlying != null) {
            // Make FLYING mutually exclusive with ceiling-cling when both setters exist.
            boolean hasCeilingCling = setHanging != null || setLatched != null;
            Predicate<LivingEntity> p = hasCeilingCling
                    ? WearerPredicates.FLYING_NOT_HANGING
                    : WearerPredicates.FLYING;
            result[i++] = new FlightSetter(setFlying, p);
        }
        if (setHovering != null) {
            result[i++] = new FlightSetter(setHovering, WearerPredicates.HOVERING);
        }
        if (setHanging != null) {
            result[i++] = new FlightSetter(setHanging, WearerPredicates.CEILING_HANGING);
        }
        if (setLatched != null) {
            result[i++] = new FlightSetter(setLatched, WearerPredicates.CEILING_HANGING);
        }
        return result;
    }

    private static Method findMethod(Class<?> entityClass, String name) {
        try {
            return entityClass.getMethod(name, boolean.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
