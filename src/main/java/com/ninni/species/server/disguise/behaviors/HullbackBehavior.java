package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.client.events.InventoryRenderCheck;
import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Behavior for Whaleborne's Hullback (multipart, positioned via private {@code updatePartPositions}).
 * Allows xRot only in inventory; pre-conditions part arrays to a rigid layout and re-runs
 * updatePartPositions wrapped in a full snapshot/restore so world-render isn't polluted. Soft-dep, reflective.
 */
public final class HullbackBehavior implements DisguiseBehavior {

    public static final HullbackBehavior INSTANCE = new HullbackBehavior();

    private static volatile boolean reflectionInited;
    private static Method updatePartPositionsMethod;
    private static Field partPositionField;
    private static Field partYRotField;
    private static Field partXRotField;
    private static Field prevPartPositionsField;
    private static Field oldPartPositionField;
    private static Field oldPartYRotField;
    private static Field oldPartXRotField;
    private static Field stationaryTicksField;

    // Snapshot storage (render thread only). Each pre/postRender call is paired.
    private Vec3[] savedPartPosition;
    private float[] savedPartYRot;
    private float[] savedPartXRot;
    private Vec3[] savedPrevPartPositions;
    private Vec3[] savedOldPartPosition;
    private float[] savedOldPartYRot;
    private float[] savedOldPartXRot;
    // Sub-entity state. updatePartPositions() calls moveTo() on each part; without
    // restoring those, setOldPosAndRots() bakes the inventory mouse pose into
    // oldPartPosition[] — visible as a one-frame jitter and lerp drift.
    private Vec3[] savedSubEntityPos;
    private float[] savedSubEntityYRot;
    private float[] savedSubEntityXRot;
    private float[] savedSubEntityYRotO;
    private float[] savedSubEntityXRotO;
    // Snapshot stationaryTicks to neutralize swim oscillation in inventory.
    private int savedStationaryTicks;
    private boolean savedInventoryNeutralized;

    private HullbackBehavior() {}

    @Override
    public boolean shouldApplyXRot(LivingEntity wearer, LivingEntity disguise, boolean inInventory) {
        // Tilt allowed in inventory (mouse-driven xRot); suppressed in world — whales don't bend.
        return inInventory;
    }

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (!isInventoryRender()) return;
        initReflection(disguise.getClass());

        snapshot(disguise);

        // Pre-condition prevPartPositions to the rigid layout, force stationaryTicks > 0
        // (kills swimCycle), then re-run updatePartPositions() so every array and sub-entity
        // is written in one consistent pass.
        //
        // HullbackRenderer (lines 108-242) lerps partPosition[i]/oldPartPosition[i] by
        // partialTicks; stale prevPart values cause body/tail/fluke to lag behind the head
        // (HullbackEntity:3440-3445). Setting prevPart[i] to the rigid target makes the
        // lerp identity. stationaryTicks=60 gates swimCycle=0 (HullbackEntity:3407).
        rigidLayoutViaUpdatePartPositions(disguise);
    }

    /** Pre-conditions part arrays to rigid layout, then calls {@code updatePartPositions} for one consistent pass. */
    private void rigidLayoutViaUpdatePartPositions(LivingEntity disguise) {
        // Base offsets from HullbackEntity.updatePartPositions:3389-3395.
        Vec3[] baseOffsets = {
                new Vec3(0, 0, 6),     // nose
                new Vec3(0, 0, 2.5),   // head
                new Vec3(0, 0, -2.25), // body
                new Vec3(0, 0, -7),    // tail
                new Vec3(0, 0, -11)    // fluke
        };
        float yaw = disguise.getYRot();
        float pitch = disguise.getXRot();
        // Match sign convention from updatePartPositions:3426-3427.
        float yawRad = -yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        Vec3 entityPos = disguise.position();

        Vec3[] rigidPositions = new Vec3[baseOffsets.length];
        for (int i = 0; i < baseOffsets.length; i++) {
            Vec3 rotated = baseOffsets[i].yRot(yawRad).xRot(pitchRad);
            rigidPositions[i] = entityPos.add(rotated);
        }

        // Pre-condition prevPart to the rigid target so lerp drag (HullbackEntity:3440-3445)
        // is identity. Pre-fill oldPartPosition for consistent first-frame lerp.
        try {
            if (prevPartPositionsField != null) {
                Vec3[] arr = (Vec3[]) prevPartPositionsField.get(disguise);
                if (arr != null) System.arraycopy(rigidPositions, 0, arr, 0, Math.min(arr.length, rigidPositions.length));
            }
            if (oldPartPositionField != null) {
                Vec3[] arr = (Vec3[]) oldPartPositionField.get(disguise);
                if (arr != null) System.arraycopy(rigidPositions, 0, arr, 0, Math.min(arr.length, rigidPositions.length));
            }
            // stationaryTicks > 0 → swimCycle = 0 (HullbackEntity:3407).
            if (stationaryTicksField != null) {
                stationaryTicksField.setInt(disguise, 60);
            }
        } catch (ReflectiveOperationException ignored) {}

        // Pre-position sub-entities to the rigid layout. setOldPosAndRots() runs in
        // tick():1592-1594, not inside updatePartPositions, so it read the previous
        // sub-entity positions; oldPartPosition is already overwritten above. This
        // ensures any code path reading sub-entity positions gets matching data.
        if (disguise.isMultipartEntity()) {
            PartEntity<?>[] parts = disguise.getParts();
            if (parts != null) {
                for (int i = 0; i < parts.length && i < rigidPositions.length; i++) {
                    parts[i].setPos(rigidPositions[i].x, rigidPositions[i].y, rigidPositions[i].z);
                    parts[i].setYRot(yaw);
                    parts[i].setXRot(pitch);
                    parts[i].yRotO = yaw;
                    parts[i].xRotO = pitch;
                }
            }
        }

        // updatePartPositions() sees prev=rigid (no drag), writes partPosition[i]=rigid
        // and calls subEntities[i].moveTo — every renderer input in one consistent pass.
        try {
            if (updatePartPositionsMethod != null) {
                updatePartPositionsMethod.invoke(disguise);
            }
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void postRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (savedPartPosition == null) return;
        restore(disguise);
        if (savedInventoryNeutralized) {
            try {
                if (stationaryTicksField != null) {
                    stationaryTicksField.setInt(disguise, savedStationaryTicks);
                }
            } catch (ReflectiveOperationException ignored) {}
            savedInventoryNeutralized = false;
        }
        clearSnapshot();
    }

    private void snapshot(LivingEntity disguise) {
        try {
            savedPartPosition = cloneVec3Array((Vec3[]) partPositionField.get(disguise));
            savedPartYRot = ((float[]) partYRotField.get(disguise)).clone();
            savedPartXRot = ((float[]) partXRotField.get(disguise)).clone();
            savedPrevPartPositions = cloneVec3Array((Vec3[]) prevPartPositionsField.get(disguise));
            savedOldPartPosition = cloneVec3Array((Vec3[]) oldPartPositionField.get(disguise));
            savedOldPartYRot = ((float[]) oldPartYRotField.get(disguise)).clone();
            savedOldPartXRot = ((float[]) oldPartXRotField.get(disguise)).clone();
            snapshotSubEntities(disguise);
            // Snapshot stationaryTicks — preRender writes 60 to kill swimCycle; postRender restores.
            if (stationaryTicksField != null) {
                savedStationaryTicks = stationaryTicksField.getInt(disguise);
                savedInventoryNeutralized = true;
            }
        } catch (ReflectiveOperationException ignored) {
            clearSnapshot();
        }
    }

    private void snapshotSubEntities(LivingEntity disguise) {
        if (!disguise.isMultipartEntity()) return;
        PartEntity<?>[] parts = disguise.getParts();
        if (parts == null) return;
        savedSubEntityPos = new Vec3[parts.length];
        savedSubEntityYRot = new float[parts.length];
        savedSubEntityXRot = new float[parts.length];
        savedSubEntityYRotO = new float[parts.length];
        savedSubEntityXRotO = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            savedSubEntityPos[i] = parts[i].position();
            savedSubEntityYRot[i] = parts[i].getYRot();
            savedSubEntityXRot[i] = parts[i].getXRot();
            savedSubEntityYRotO[i] = parts[i].yRotO;
            savedSubEntityXRotO[i] = parts[i].xRotO;
        }
    }

    private void restore(LivingEntity disguise) {
        try {
            writeVec3Array(disguise, partPositionField, savedPartPosition);
            writeFloatArray(disguise, partYRotField, savedPartYRot);
            writeFloatArray(disguise, partXRotField, savedPartXRot);
            writeVec3Array(disguise, prevPartPositionsField, savedPrevPartPositions);
            writeVec3Array(disguise, oldPartPositionField, savedOldPartPosition);
            writeFloatArray(disguise, oldPartYRotField, savedOldPartYRot);
            writeFloatArray(disguise, oldPartXRotField, savedOldPartXRot);
            restoreSubEntities(disguise);
        } catch (ReflectiveOperationException ignored) {}
    }

    private void restoreSubEntities(LivingEntity disguise) {
        if (savedSubEntityPos == null) return;
        if (!disguise.isMultipartEntity()) return;
        PartEntity<?>[] parts = disguise.getParts();
        if (parts == null || parts.length != savedSubEntityPos.length) return;
        for (int i = 0; i < parts.length; i++) {
            parts[i].setPos(savedSubEntityPos[i].x, savedSubEntityPos[i].y, savedSubEntityPos[i].z);
            parts[i].setYRot(savedSubEntityYRot[i]);
            parts[i].setXRot(savedSubEntityXRot[i]);
            parts[i].yRotO = savedSubEntityYRotO[i];
            parts[i].xRotO = savedSubEntityXRotO[i];
        }
    }

    private void clearSnapshot() {
        savedPartPosition = null;
        savedPartYRot = null;
        savedPartXRot = null;
        savedPrevPartPositions = null;
        savedOldPartPosition = null;
        savedOldPartYRot = null;
        savedOldPartXRot = null;
        savedSubEntityPos = null;
        savedSubEntityYRot = null;
        savedSubEntityXRot = null;
        savedSubEntityYRotO = null;
        savedSubEntityXRotO = null;
    }

    private static Vec3[] cloneVec3Array(Vec3[] src) {
        if (src == null) return null;
        return src.clone(); // Vec3 is immutable, shallow clone is sufficient
    }

    private static void writeVec3Array(LivingEntity target, Field field, Vec3[] value) throws ReflectiveOperationException {
        if (field == null || value == null) return;
        Vec3[] dst = (Vec3[]) field.get(target);
        if (dst.length != value.length) field.set(target, value); // shouldn't happen, but safe
        else System.arraycopy(value, 0, dst, 0, value.length);
    }

    private static void writeFloatArray(LivingEntity target, Field field, float[] value) throws ReflectiveOperationException {
        if (field == null || value == null) return;
        float[] dst = (float[]) field.get(target);
        if (dst.length != value.length) field.set(target, value);
        else System.arraycopy(value, 0, dst, 0, value.length);
    }

    private static void initReflection(Class<?> hullbackClass) {
        if (reflectionInited) return;
        synchronized (HullbackBehavior.class) {
            if (reflectionInited) return;
            // Set reflectionInited LAST — transient failure must not lock out future retries.
            try {
                updatePartPositionsMethod = hullbackClass.getDeclaredMethod("updatePartPositions");
                updatePartPositionsMethod.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {}
            partPositionField = grabField(hullbackClass, "partPosition");
            partYRotField = grabField(hullbackClass, "partYRot");
            partXRotField = grabField(hullbackClass, "partXRot");
            prevPartPositionsField = grabField(hullbackClass, "prevPartPositions");
            oldPartPositionField = grabField(hullbackClass, "oldPartPosition");
            oldPartYRotField = grabField(hullbackClass, "oldPartYRot");
            oldPartXRotField = grabField(hullbackClass, "oldPartXRot");
            stationaryTicksField = grabField(hullbackClass, "stationaryTicks");
            reflectionInited = true;
        }
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
