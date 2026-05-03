package com.ninni.species.server.disguise.dsl;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.function.Predicate;

/**
 * Writes a {@link Boolean} {@link EntityDataAccessor} on the disguise per tick from a wearer predicate.
 * Use when no public setter exists or to bypass setter side-effects; lazy hierarchy walk, silent no-op if absent.
 */
public final class ReflectiveDataAccessorFlagBehavior implements DisguiseBehavior {

    private final String accessorFieldName;
    private final Predicate<LivingEntity> wearerPredicate;
    private volatile boolean reflectionInited;
    private EntityDataAccessor<Boolean> accessor;

    public ReflectiveDataAccessorFlagBehavior(String accessorFieldName, Predicate<LivingEntity> wearerPredicate) {
        if (accessorFieldName == null || accessorFieldName.isEmpty()) {
            throw new IllegalArgumentException("accessorFieldName must be non-empty");
        }
        if (wearerPredicate == null) {
            throw new IllegalArgumentException("wearerPredicate must be non-null");
        }
        this.accessorFieldName = accessorFieldName;
        this.wearerPredicate = wearerPredicate;
    }

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        if (!reflectionInited) initReflection(disguise.getClass());
        EntityDataAccessor<Boolean> a = accessor;
        if (a == null) return;
        try {
            disguise.getEntityData().set(a, wearerPredicate.test(wearer));
        } catch (Throwable ignored) {
            // Swallowed to keep the tick loop alive.
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void initReflection(Class<?> entityClass) {
        if (reflectionInited) return;
        Class<?> c = entityClass;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(accessorFieldName);
                f.setAccessible(true);
                Object value = f.get(null);
                if (value instanceof EntityDataAccessor<?>) {
                    accessor = (EntityDataAccessor<Boolean>) value;
                    break;
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            c = c.getSuperclass();
        }
        reflectionInited = true;
    }
}
