package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Snapshots pos + rotations into scratch before a destructive positioner runs. Pairs with
 *  {@link RestorePrevFrameHook} for positioners that wipe the prev-frame interpolation fields. */
public final class CapturePrevFrameHook implements TickHook {

    public static final String KEY_OLD_X = "capture.oldX";
    public static final String KEY_OLD_Y = "capture.oldY";
    public static final String KEY_OLD_Z = "capture.oldZ";
    public static final String KEY_OLD_Y_ROT = "capture.oldYRot";
    public static final String KEY_OLD_X_ROT = "capture.oldXRot";
    public static final String KEY_OLD_Y_BODY_ROT = "capture.oldYBodyRot";
    public static final String KEY_OLD_Y_HEAD_ROT = "capture.oldYHeadRot";

    public static final CapturePrevFrameHook INSTANCE = new CapturePrevFrameHook();
    private CapturePrevFrameHook() {}

    @Override
    public void run(SegmentContext ctx) {
        Entity seg = ctx.seg();
        ctx.putScratch(KEY_OLD_X, seg.getX());
        ctx.putScratch(KEY_OLD_Y, seg.getY());
        ctx.putScratch(KEY_OLD_Z, seg.getZ());
        ctx.putScratch(KEY_OLD_Y_ROT, seg.getYRot());
        ctx.putScratch(KEY_OLD_X_ROT, seg.getXRot());
        LivingEntity ls = ctx.livingSeg();
        if (ls != null) {
            ctx.putScratch(KEY_OLD_Y_BODY_ROT, ls.yBodyRot);
            ctx.putScratch(KEY_OLD_Y_HEAD_ROT, ls.yHeadRot);
        }
    }
}
