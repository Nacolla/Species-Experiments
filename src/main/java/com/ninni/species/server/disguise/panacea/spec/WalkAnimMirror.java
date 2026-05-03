package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

/** Copies the wearer's walkAnimation onto the segment. Client segments never run aiStep, so without this the rig freezes. */
public final class WalkAnimMirror implements AnimDriver {

    private final float idleFloor;

    public WalkAnimMirror(float idleFloor) {
        this.idleFloor = idleFloor;
    }

    public static WalkAnimMirror noFloor() {
        return new WalkAnimMirror(0.0F);
    }

    @Override
    public void drive(SegmentContext ctx) {
        LivingEntity ls = ctx.livingSeg();
        if (ls == null) return;
        try {
            ls.walkAnimation.update(Math.max(ctx.wearer().walkAnimation.speed(), idleFloor), 1.0F);
        } catch (Throwable ignored) {}
    }
}
