package com.ninni.species.server.disguise.dsl;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;

/**
 * Pins every yaw to {@code yBodyRot} and pitch to 0 during inventory render so {@code netHeadYaw}/{@code headPitch}
 * are zero; ThreadLocal snapshot is restored in {@link #postInventoryPose} to prevent state leak on render exception.
 */
public final class InventoryRotationLockBehavior implements DisguiseBehavior {

    public static final InventoryRotationLockBehavior INSTANCE = new InventoryRotationLockBehavior();

    private static final ThreadLocal<Snapshot> SNAPSHOT = new ThreadLocal<>();

    private static final class Snapshot {
        float yHeadRot;
        float yHeadRotO;
        float yBodyRotO;
        float yRot;
        float yRotO;
        float xRot;
        float xRotO;
    }

    private InventoryRotationLockBehavior() {}

    @Override
    public void preInventoryPose(LivingEntity wearer, LivingEntity disguise, float partialTick) {
        Snapshot snap = new Snapshot();
        SNAPSHOT.set(snap);
        snap.yHeadRot = disguise.yHeadRot;
        snap.yHeadRotO = disguise.yHeadRotO;
        snap.yBodyRotO = disguise.yBodyRotO;
        snap.yRot = disguise.getYRot();
        snap.yRotO = disguise.yRotO;
        snap.xRot = disguise.getXRot();
        snap.xRotO = disguise.xRotO;

        // Lock every yaw to yBodyRot, every pitch to 0.
        float lockedYaw = disguise.yBodyRot;
        disguise.yBodyRotO = lockedYaw;
        disguise.yHeadRot = lockedYaw;
        disguise.yHeadRotO = lockedYaw;
        disguise.setYRot(lockedYaw);
        disguise.yRotO = lockedYaw;
        disguise.setXRot(0F);
        disguise.xRotO = 0F;
    }

    @Override
    public void postInventoryPose(LivingEntity wearer, LivingEntity disguise, float partialTick) {
        Snapshot snap = SNAPSHOT.get();
        if (snap == null) return;
        SNAPSHOT.remove();
        disguise.yHeadRot = snap.yHeadRot;
        disguise.yHeadRotO = snap.yHeadRotO;
        disguise.yBodyRotO = snap.yBodyRotO;
        disguise.setYRot(snap.yRot);
        disguise.yRotO = snap.yRotO;
        disguise.setXRot(snap.xRot);
        disguise.xRotO = snap.xRotO;
    }
}
