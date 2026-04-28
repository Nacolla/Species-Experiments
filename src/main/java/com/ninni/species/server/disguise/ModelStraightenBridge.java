package com.ninni.species.server.disguise;

import com.ninni.species.mixin_util.EntityRenderDispatcherAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Forces AlexsCaves multipart models into their {@code straighten} pose for inventory render.
 * Conditional renderers (Lux/Atlatitan) use {@link #setStraightenViaReflection};
 * unconditional ones (Hullbreaker/Tremorzilla/GossamerWorm) read {@link #FORCE_STRAIGHTEN} via mixin.
 */
public final class ModelStraightenBridge {

    /** Flipped on/off around the renderer.render call by behaviors. Read by Mixins. */
    public static volatile boolean FORCE_STRAIGHTEN = false;

    /** Cache of the {@code straighten} field per model class (avoids reflection lookup per frame). */
    private static final Map<Class<?>, Field> STRAIGHTEN_FIELD_CACHE = new ConcurrentHashMap<>();

    /** Sentinel value stored when a model class has no straighten field (avoids re-probing). */
    private static final Field NOT_PRESENT_SENTINEL;
    static {
        try {
            NOT_PRESENT_SENTINEL = ModelStraightenBridge.class.getDeclaredField("NOT_PRESENT_SENTINEL");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModelStraightenBridge() {}

    /** Writes {@code straighten = value} on the disguise's model via reflection; returns the previous value, or null if no field. */
    public static Boolean setStraightenViaReflection(LivingEntity disguise, boolean value) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(disguise);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) return null;
        Object model = livingRenderer.getModel();
        if (model == null) return null;
        Field f = resolveStraightenField(model.getClass());
        if (f == null) return null;
        try {
            boolean prev = f.getBoolean(model);
            f.setBoolean(model, value);
            return prev;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Field resolveStraightenField(Class<?> modelClass) {
        Field cached = STRAIGHTEN_FIELD_CACHE.get(modelClass);
        if (cached != null) return cached == NOT_PRESENT_SENTINEL ? null : cached;

        Class<?> c = modelClass;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField("straighten");
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    STRAIGHTEN_FIELD_CACHE.put(modelClass, f);
                    return f;
                }
            } catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        STRAIGHTEN_FIELD_CACHE.put(modelClass, NOT_PRESENT_SENTINEL);
        return null;
    }

    /** True iff inventory rendering is currently active (via the dispatcher accessor). */
    public static boolean isInventoryRender() {
        return Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access
                && access.getRenderingInventoryEntity();
    }
}
