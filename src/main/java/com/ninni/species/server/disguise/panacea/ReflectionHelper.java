package com.ninni.species.server.disguise.panacea;

import net.minecraft.network.syncher.EntityDataAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** @deprecated Thin alias for {@link com.ninni.species.api.util.ReflectionHelper}; new callers
 *  should import the API package directly. */
@Deprecated
public final class ReflectionHelper {

    private ReflectionHelper() {}

    public static Method publicMethod(Class<?> c, String name, Class<?>... params) {
        return com.ninni.species.api.util.ReflectionHelper.publicMethod(c, name, params);
    }

    public static Method declaredMethod(Class<?> from, String name, Class<?>... params) {
        return com.ninni.species.api.util.ReflectionHelper.declaredMethod(from, name, params);
    }

    public static Field declaredField(Class<?> from, String name) {
        return com.ninni.species.api.util.ReflectionHelper.declaredField(from, name);
    }

    public static Field declaredFieldOfType(Class<?> from, String name, Class<?> type) {
        return com.ninni.species.api.util.ReflectionHelper.declaredFieldOfType(from, name, type);
    }

    public static <T> EntityDataAccessor<T> accessor(Class<?> from, String name) {
        return com.ninni.species.api.util.ReflectionHelper.accessor(from, name);
    }
}
