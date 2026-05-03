package com.ninni.species.api.util;

import net.minecraft.network.syncher.EntityDataAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Soft-dep reflection lookups for disguise behaviors. All methods walk the class hierarchy and
 * return {@code null} on failure so callers can no-op when the target mod is absent.
 */
public final class ReflectionHelper {

    private ReflectionHelper() {}

    public static Method publicMethod(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); } catch (NoSuchMethodException e) { return null; }
    }

    public static Method declaredMethod(Class<?> from, String name, Class<?>... params) {
        Class<?> c = from;
        while (c != null && c != Object.class) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    public static Field declaredField(Class<?> from, String name) {
        Class<?> c = from;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    /** {@link #declaredField} plus a runtime type check; returns null if the field exists but isn't of {@code type}. */
    public static Field declaredFieldOfType(Class<?> from, String name, Class<?> type) {
        Field f = declaredField(from, name);
        return (f != null && f.getType() == type) ? f : null;
    }

    /** Resolves a static {@code EntityDataAccessor<?>} field by name. Caller asserts the type. */
    @SuppressWarnings("unchecked")
    public static <T> EntityDataAccessor<T> accessor(Class<?> from, String name) {
        Class<?> c = from;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(null);
                if (v instanceof EntityDataAccessor<?>) return (EntityDataAccessor<T>) v;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }
}
