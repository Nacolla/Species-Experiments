package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.client.events.InventoryRenderCheck;
import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

/**
 * {@link EnderDragon} disguise: +180 yawOffset (renderer rotates by -yaw, model faces -Z), forces
 * {@code SITTING_FLAMING} phase, and overwrites {@code positions[]} latency buffer for inventory mouse-following.
 */
public final class EnderDragonBehavior implements DisguiseBehavior {

    public static final EnderDragonBehavior INSTANCE = new EnderDragonBehavior();

    private double[][] savedPositions;
    private int savedPosPointer;

    private EnderDragonBehavior() {}

    @Override
    public float yawOffset(LivingEntity disguise) {
        return 180.0F;
    }

    @Override
    public void preTick(LivingEntity wearer, LivingEntity disguise) {
        if (!(disguise instanceof EnderDragon dragon)) return;
        try {
            if (dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.SITTING_FLAMING) {
                dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_FLAMING);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void preRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (!(disguise instanceof EnderDragon dragon)) return;
        if (!isInventoryRender()) return;

        savedPositions = new double[dragon.positions.length][];
        for (int i = 0; i < dragon.positions.length; i++) {
            savedPositions[i] = dragon.positions[i].clone();
        }
        savedPosPointer = dragon.posPointer;

        double yaw = dragon.getYRot();
        double y = dragon.getY();
        for (int i = 0; i < dragon.positions.length; i++) {
            dragon.positions[i][0] = yaw;
            dragon.positions[i][1] = y;
        }
    }

    @Override
    public void postRender(LivingEntity wearer, LivingEntity disguise, float partialTick, boolean inInventory) {
        if (!inInventory) return;
        if (!(disguise instanceof EnderDragon dragon)) return;
        if (savedPositions == null) return;

        for (int i = 0; i < savedPositions.length; i++) {
            dragon.positions[i] = savedPositions[i];
        }
        dragon.posPointer = savedPosPointer;
        savedPositions = null;
    }

    private static boolean isInventoryRender() {
        return InventoryRenderCheck.isInventoryRender();
    }
}
