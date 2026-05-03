package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Writes {@link CapturePrevFrameHook}'s scratch values back into the segment's prev-frame
 *  fields. {@code forceYBodyRotEqualsYHeadRot} pins {@code yBodyRot = yHeadRot} for chains
 *  without body/head yaw split. */
public final class RestorePrevFrameHook implements TickHook {

    private final boolean forceYBodyRotEqualsYHeadRot;

    public RestorePrevFrameHook(boolean forceYBodyRotEqualsYHeadRot) {
        this.forceYBodyRotEqualsYHeadRot = forceYBodyRotEqualsYHeadRot;
    }

    public static RestorePrevFrameHook standard() {
        return new RestorePrevFrameHook(false);
    }

    public static RestorePrevFrameHook withYBodyRotForcedToYHeadRot() {
        return new RestorePrevFrameHook(true);
    }

    @Override
    public void run(SegmentContext ctx) {
        // Armor stand wearer: positioner already aligned -O fields for partialTick=0; restoring
        // the pre-positioner snapshot would overwrite that.
        if (ctx.wearer() instanceof net.minecraft.world.entity.decoration.ArmorStand) return;

        Entity seg = ctx.seg();
        Double oldX = ctx.scratch(CapturePrevFrameHook.KEY_OLD_X);
        Double oldY = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Y);
        Double oldZ = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Z);
        Float oldYRot = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Y_ROT);
        Float oldXRot = ctx.scratch(CapturePrevFrameHook.KEY_OLD_X_ROT);
        if (oldX == null || oldY == null || oldZ == null || oldYRot == null || oldXRot == null) return;

        seg.xo = oldX; seg.yo = oldY; seg.zo = oldZ;
        seg.xOld = oldX; seg.yOld = oldY; seg.zOld = oldZ;
        seg.yRotO = oldYRot;
        seg.xRotO = oldXRot;

        LivingEntity ls = ctx.livingSeg();
        if (ls != null) {
            Float oldYBodyRot = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Y_BODY_ROT);
            Float oldYHeadRot = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Y_HEAD_ROT);
            if (oldYBodyRot != null) ls.yBodyRotO = oldYBodyRot;
            if (oldYHeadRot != null) ls.yHeadRotO = oldYHeadRot;
            if (forceYBodyRotEqualsYHeadRot) ls.yBodyRot = ls.yHeadRot;
        }
    }
}
