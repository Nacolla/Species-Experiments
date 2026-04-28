package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.server.disguise.ModelStraightenBridge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;

/**
 * Behavior for AlexsCaves sauropods (Atlatitan, Luxtructosaurus, anything extending {@code SauropodBaseEntity}).
 * Drives {@code WALKING} from wearer movement, scales walk-anim phase, and forces {@code straighten=true}
 * via {@link ModelStraightenBridge#setStraightenViaReflection} for inventory render. Soft-dep; all reflection.
 */
public class SauropodBehavior implements DisguiseBehavior {

    public static final SauropodBehavior INSTANCE = new SauropodBehavior();

    /** Multiplier applied to walkAnimation.position so the leg cycle plays faster. */
    private static final float WALK_ANIM_SPEED_MULTIPLIER = 2.0F;

    private static volatile boolean reflectionInited;
    private static EntityDataAccessor<Boolean> walkingAccessor;

    // Inventory snapshot — single in-flight render at a time on the render thread.
    private boolean inventorySnapshot;
    private Boolean savedStraighten;

    protected SauropodBehavior() {}

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        initReflection(disguise.getClass());
        if (walkingAccessor == null) return;

        boolean wearerMoving = wearer.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5
                || wearer.walkAnimation.speed() > 0.05F;

        try {
            disguise.getEntityData().set((EntityDataAccessor) walkingAccessor, wearerMoving);
        } catch (Throwable ignored) {}
    }

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        // Scale walk animation phase — re-applied per frame because syncWearerStateToDisguise
        // resets walkAnimation.position to the wearer's value just before preRender.
        disguise.walkAnimation.position *= WALK_ANIM_SPEED_MULTIPLIER;

        if (!inInventory) return;
        if (!ModelStraightenBridge.isInventoryRender()) return;

        // Renderers only set straighten inside `if(sepia)`, so this external write persists.
        savedStraighten = ModelStraightenBridge.setStraightenViaReflection(disguise, true);
        inventorySnapshot = true;
    }

    @Override
    public void postRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (!inventorySnapshot) return;
        inventorySnapshot = false;
        if (savedStraighten != null) {
            ModelStraightenBridge.setStraightenViaReflection(disguise, savedStraighten);
            savedStraighten = null;
        }
    }

    private static void initReflection(Class<?> sauropodClass) {
        if (reflectionInited) return;
        synchronized (SauropodBehavior.class) {
            if (reflectionInited) return;
            // Walk the hierarchy to find the SauropodBaseEntity that declares WALKING.
            Class<?> c = sauropodClass;
            while (c != null && c != Object.class) {
                try {
                    Field f = c.getDeclaredField("WALKING");
                    f.setAccessible(true);
                    Object value = f.get(null);
                    if (value instanceof EntityDataAccessor<?>) {
                        walkingAccessor = (EntityDataAccessor<Boolean>) value;
                        break;
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
                c = c.getSuperclass();
            }
            // Set inited LAST — partial resolution failure must not lock out future retries.
            reflectionInited = true;
        }
    }
}
