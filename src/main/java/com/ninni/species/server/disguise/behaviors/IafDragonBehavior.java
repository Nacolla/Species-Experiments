package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.client.events.InventoryRenderCheck;
import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Behavior for Ice and Fire's dragon family. {@link #preTick} drives IaF's {@code setFlying}/{@code setHovering}
 * from wearer state; {@link #postTick} clears pitch/roll buffers when grounded (IaF gates both calc and decay on !onGround).
 * Soft-dep, reflective with first-call caching.
 */
public final class IafDragonBehavior implements DisguiseBehavior {

    public static final IafDragonBehavior INSTANCE = new IafDragonBehavior();

    private static volatile boolean reflectionInited;
    private static Method setFlyingMethod;
    private static Method setHoveringMethod;
    private static Method getDragonPitchMethod;
    private static Method setDragonPitchMethod;
    private static Field prevDragonPitchField;
    private static Field turnBufferField;
    private static Field tailBufferField;
    private static Field rollBufferField;
    private static Field pitchBufferField;
    private static Field pitchBufferBodyField;
    private static Field yawVariationField;
    private static Field prevYawVariationField;
    private static Field pitchVariationField;
    private static Field prevPitchVariationField;
    private static Field yawTimerField;
    private static Field pitchTimerField;

    /**
     * Per-render snapshot in a ThreadLocal so each preRender/postRender pair has its own
     * state. A render exception between snapshot and restore leaves no global residue;
     * postRender always calls {@link ThreadLocal#remove}.
     */
    private static final ThreadLocal<Snapshot> SNAPSHOT = new ThreadLocal<>();

    private static final class Snapshot {
        boolean savedDragonPitchValid;
        float savedDragonPitch;
        float savedPrevDragonPitch;
        // Full rotation snapshot — all fields pinned to yBodyRot/0 so netHeadYaw=0,
        // headPitch=0 → model.faceTarget(0,0,...) adds no bend to neckParts
        // (DragonTabulaModelAnimator.java:92).
        float savedYHeadRot;
        float savedYHeadRotO;
        float savedYBodyRotO;
        float savedYRot;
        float savedYRotO;
        float savedXRot;
        float savedXRotO;
        // Saved chain buffer variations (5 buffers × 4 floats + 2 timers each)
        float[] savedBufferYaw;
        float[] savedBufferPrevYaw;
        float[] savedBufferPitch;
        float[] savedBufferPrevPitch;
        int[] savedBufferYawTimer;
        int[] savedBufferPitchTimer;
    }

    private IafDragonBehavior() {}

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        initReflection(disguise.getClass());
        if (setFlyingMethod == null) return;

        boolean wearerFlying = wearer.isFallFlying()
                || wearer.isSwimming()
                || (wearer instanceof Player p && p.getAbilities().flying);

        try {
            setFlyingMethod.invoke(disguise, wearerFlying);
            if (setHoveringMethod != null) {
                // Hover when player is creative-flying but not moving fast — approximation.
                boolean wearerHovering = (wearer instanceof Player p && p.getAbilities().flying)
                        && wearer.getDeltaMovement().horizontalDistanceSqr() < 0.005;
                setHoveringMethod.invoke(disguise, wearerHovering);
            }
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (!isInventoryRender()) return;
        initReflection(disguise.getClass());

        Snapshot snap = new Snapshot();
        SNAPSHOT.set(snap);

        // (1) Chain buffer snapshot + zero. Runs unconditionally, independent of
        // dragonPitch reflection.
        Field[] bufferFields = {turnBufferField, tailBufferField, rollBufferField, pitchBufferField, pitchBufferBodyField};
        snap.savedBufferYaw = new float[bufferFields.length];
        snap.savedBufferPrevYaw = new float[bufferFields.length];
        snap.savedBufferPitch = new float[bufferFields.length];
        snap.savedBufferPrevPitch = new float[bufferFields.length];
        snap.savedBufferYawTimer = new int[bufferFields.length];
        snap.savedBufferPitchTimer = new int[bufferFields.length];
        for (int i = 0; i < bufferFields.length; i++) {
            try {
                if (bufferFields[i] == null) continue;
                Object buf = bufferFields[i].get(disguise);
                if (buf == null) continue;
                if (yawVariationField != null) snap.savedBufferYaw[i] = yawVariationField.getFloat(buf);
                if (prevYawVariationField != null) snap.savedBufferPrevYaw[i] = prevYawVariationField.getFloat(buf);
                if (pitchVariationField != null) snap.savedBufferPitch[i] = pitchVariationField.getFloat(buf);
                if (prevPitchVariationField != null) snap.savedBufferPrevPitch[i] = prevPitchVariationField.getFloat(buf);
                if (yawTimerField != null) snap.savedBufferYawTimer[i] = yawTimerField.getInt(buf);
                if (pitchTimerField != null) snap.savedBufferPitchTimer[i] = pitchTimerField.getInt(buf);
                zeroBuffer(disguise, bufferFields[i]);
            } catch (ReflectiveOperationException ignored) {}
        }

        // (2) Full rotation lock — pin every yaw/pitch field to yBodyRot/0.
        // Vanilla InventoryScreen sets yBodyRot≠yHeadRot by design and leaves
        // *RotO at world values, so lerp can land 90-180° off target; IaF's neck
        // chain amplifies this into a visible twist. Locking everything kills
        // both effects (DragonTabulaModelAnimator.java:92).
        snap.savedYHeadRot = disguise.yHeadRot;
        snap.savedYHeadRotO = disguise.yHeadRotO;
        snap.savedYBodyRotO = disguise.yBodyRotO;
        snap.savedYRot = disguise.getYRot();
        snap.savedYRotO = disguise.yRotO;
        snap.savedXRot = disguise.getXRot();
        snap.savedXRotO = disguise.xRotO;

        float lockedYaw = disguise.yBodyRot;
        disguise.yBodyRotO = lockedYaw;
        disguise.yHeadRot = lockedYaw;
        disguise.yHeadRotO = lockedYaw;
        disguise.setYRot(lockedYaw);
        disguise.yRotO = lockedYaw;
        disguise.setXRot(0F);
        disguise.xRotO = 0F;

        // (3) dragonPitch override. RenderDragonBase.scale (line 50-51) applies
        // mulPose(Axis.XP.rotationDegrees(dragonPitch)) before setupAnim — stale world
        // value causes a visible global tilt in inventory. Force to 0.
        if (setDragonPitchMethod != null && prevDragonPitchField != null) {
            try {
                snap.savedDragonPitch = (Float) getDragonPitchMethod.invoke(disguise);
                snap.savedPrevDragonPitch = prevDragonPitchField.getFloat(disguise);
                snap.savedDragonPitchValid = true;
                setDragonPitchMethod.invoke(disguise, 0F);
                prevDragonPitchField.setFloat(disguise, 0F);
            } catch (ReflectiveOperationException ignored) {
                snap.savedDragonPitchValid = false;
            }
        }
    }

    @Override
    public void postRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        Snapshot snap = SNAPSHOT.get();
        if (snap == null) return;
        // Always remove ThreadLocal — defensive cleanup regardless of downstream throws.
        SNAPSHOT.remove();

        // Restore chain buffer state.
        if (snap.savedBufferYaw != null) {
            Field[] bufferFields = {turnBufferField, tailBufferField, rollBufferField, pitchBufferField, pitchBufferBodyField};
            for (int i = 0; i < bufferFields.length; i++) {
                try {
                    if (bufferFields[i] == null) continue;
                    Object buf = bufferFields[i].get(disguise);
                    if (buf == null) continue;
                    if (yawVariationField != null) yawVariationField.setFloat(buf, snap.savedBufferYaw[i]);
                    if (prevYawVariationField != null) prevYawVariationField.setFloat(buf, snap.savedBufferPrevYaw[i]);
                    if (pitchVariationField != null) pitchVariationField.setFloat(buf, snap.savedBufferPitch[i]);
                    if (prevPitchVariationField != null) prevPitchVariationField.setFloat(buf, snap.savedBufferPrevPitch[i]);
                    if (yawTimerField != null) yawTimerField.setInt(buf, snap.savedBufferYawTimer[i]);
                    if (pitchTimerField != null) pitchTimerField.setInt(buf, snap.savedBufferPitchTimer[i]);
                } catch (ReflectiveOperationException ignored) {}
            }
        }

        disguise.yHeadRot = snap.savedYHeadRot;
        disguise.yHeadRotO = snap.savedYHeadRotO;
        disguise.yBodyRotO = snap.savedYBodyRotO;
        disguise.setYRot(snap.savedYRot);
        disguise.yRotO = snap.savedYRotO;
        disguise.setXRot(snap.savedXRot);
        disguise.xRotO = snap.savedXRotO;
        if (snap.savedDragonPitchValid) {
            try {
                setDragonPitchMethod.invoke(disguise, snap.savedDragonPitch);
                prevDragonPitchField.setFloat(disguise, snap.savedPrevDragonPitch);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    @Override
    public void postTick(LivingEntity wearer, LivingEntity disguise) {
        if (yawVariationField == null) return;

        // IFChainBuffer decay bug (IFChainBuffer.java:53-55): when |yawVariation| <
        // angleDecrement, sets variation = angleDecrement instead of 0, leaving a
        // permanent residual swing. Hand-clean small variations to enforce settle-at-zero.
        try {
            zeroSmallVariation(disguise, turnBufferField);
            zeroSmallVariation(disguise, tailBufferField);
            zeroSmallVariation(disguise, rollBufferField);
            zeroSmallVariation(disguise, pitchBufferField);
            zeroSmallVariation(disguise, pitchBufferBodyField);
        } catch (ReflectiveOperationException ignored) {}

        // calculate*Buffer is gated by !onGround() in IafDragonLogic.updateDragonClient,
        // so neither calculate nor decay runs while grounded — airborne state freezes.
        // Zero explicitly for a clean inventory preview.
        if (disguise.onGround() && rollBufferField != null) {
            try {
                zeroBuffer(disguise, rollBufferField);
                zeroBuffer(disguise, pitchBufferField);
                zeroBuffer(disguise, pitchBufferBodyField);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    /** Zeros buffer variations below the IaF decay threshold (catches the settle-at-decrement bug). */
    private static void zeroSmallVariation(LivingEntity disguise, Field bufferField) throws ReflectiveOperationException {
        if (bufferField == null) return;
        Object buffer = bufferField.get(disguise);
        if (buffer == null) return;
        // Threshold > IaF angleDecrement (5 for swing, 1 for wave/flap) to catch the
        // buggy residual without killing an actively swinging buffer.
        final float threshold = 6.0F;
        if (yawVariationField != null) {
            float v = yawVariationField.getFloat(buffer);
            if (Math.abs(v) < threshold) {
                yawVariationField.setFloat(buffer, 0.0F);
                if (prevYawVariationField != null) prevYawVariationField.setFloat(buffer, 0.0F);
                if (yawTimerField != null) yawTimerField.setInt(buffer, 0);
            }
        }
        if (pitchVariationField != null) {
            float v = pitchVariationField.getFloat(buffer);
            if (Math.abs(v) < threshold) {
                pitchVariationField.setFloat(buffer, 0.0F);
                if (prevPitchVariationField != null) prevPitchVariationField.setFloat(buffer, 0.0F);
                if (pitchTimerField != null) pitchTimerField.setInt(buffer, 0);
            }
        }
    }

    private static void zeroBuffer(LivingEntity disguise, Field bufferField) throws ReflectiveOperationException {
        Object buffer = bufferField.get(disguise);
        if (buffer == null) return;
        if (yawVariationField != null) yawVariationField.setFloat(buffer, 0.0F);
        if (prevYawVariationField != null) prevYawVariationField.setFloat(buffer, 0.0F);
        if (pitchVariationField != null) pitchVariationField.setFloat(buffer, 0.0F);
        if (prevPitchVariationField != null) prevPitchVariationField.setFloat(buffer, 0.0F);
        if (yawTimerField != null) yawTimerField.setInt(buffer, 0);
        if (pitchTimerField != null) pitchTimerField.setInt(buffer, 0);
    }

    private static void initReflection(Class<?> dragonClass) {
        if (reflectionInited) return;
        synchronized (IafDragonBehavior.class) {
            if (reflectionInited) return;
            // Set reflectionInited LAST: a transient resolution failure on a partial
            // subclass must not permanently disable retry for a correctly-typed disguise.
            try {
                setFlyingMethod = dragonClass.getMethod("setFlying", boolean.class);
            } catch (ReflectiveOperationException ignored) {}
            try {
                setHoveringMethod = dragonClass.getMethod("setHovering", boolean.class);
            } catch (ReflectiveOperationException ignored) {}
            try {
                getDragonPitchMethod = dragonClass.getMethod("getDragonPitch");
                setDragonPitchMethod = dragonClass.getMethod("setDragonPitch", float.class);
            } catch (ReflectiveOperationException ignored) {}
            prevDragonPitchField = grabField(dragonClass, "prevDragonPitch");
            turnBufferField = grabField(dragonClass, "turn_buffer");
            tailBufferField = grabField(dragonClass, "tail_buffer");
            rollBufferField = grabField(dragonClass, "roll_buffer");
            pitchBufferField = grabField(dragonClass, "pitch_buffer");
            pitchBufferBodyField = grabField(dragonClass, "pitch_buffer_body");

            // Probe both Citadel ChainBuffer and IaF IFChainBuffer — same field names on both.
            Class<?> bufferClass = findBufferClass(dragonClass.getClassLoader());
            if (bufferClass != null) {
                yawVariationField = grabField(bufferClass, "yawVariation");
                prevYawVariationField = grabField(bufferClass, "prevYawVariation");
                pitchVariationField = grabField(bufferClass, "pitchVariation");
                prevPitchVariationField = grabField(bufferClass, "prevPitchVariation");
                yawTimerField = grabField(bufferClass, "yawTimer");
                pitchTimerField = grabField(bufferClass, "pitchTimer");
            }
            reflectionInited = true;
        }
    }

    private static Class<?> findBufferClass(ClassLoader cl) {
        for (String name : new String[]{
                "com.github.alexthe666.iceandfire.client.model.IFChainBuffer",
                "com.github.alexthe666.citadel.client.model.ChainBuffer"
        }) {
            try {
                return Class.forName(name, false, cl);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Field grabField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static boolean isInventoryRender() {
        return InventoryRenderCheck.isInventoryRender();
    }
}
