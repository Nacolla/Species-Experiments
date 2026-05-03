package com.ninni.species.server.disguise.panacea.spec;

import java.lang.reflect.Method;

/** Lazy resolver for AM's {@code AnacondaPartIndex} enum (soft-dep). Caches the static
 *  {@code sizeAt(int)} method and the {@code HEAD} constant; both return null if AM is absent
 *  or the class moved. Lives outside {@link ReflectionPlan} (no static-member modelling there). */
public final class AnacondaPartIndexResolver {

    public static final String FQN = "com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex";

    private static volatile boolean inited;
    private static Class<?> clazz;
    private static Method sizeAtMethod;
    private static Object headConstant;

    private AnacondaPartIndexResolver() {}

    private static synchronized void ensureInited() {
        if (inited) return;
        try {
            clazz = Class.forName(FQN);
            sizeAtMethod = clazz.getMethod("sizeAt", int.class);
            headConstant = clazz.getField("HEAD").get(null);
        } catch (ReflectiveOperationException ignored) {}
        inited = true;
    }

    public static Class<?> clazz() { ensureInited(); return clazz; }
    public static Object head() { ensureInited(); return headConstant; }

    public static Object sizeAt(int n) {
        ensureInited();
        if (sizeAtMethod == null) return null;
        try { return sizeAtMethod.invoke(null, n); }
        catch (ReflectiveOperationException e) { return null; }
    }
}
