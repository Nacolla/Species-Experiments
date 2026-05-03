package com.ninni.species.server.disguise.panacea.spec;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** VoidWorm positioner: orbits the parent's lagged frame ({@code prev.xo/yo/zo}, never mixed
 *  with current — frame mismatch whips). Each tick's yaw is branch-aligned to prev WORM_YAW so
 *  the renderer's raw lerp takes the short arc across the ±π seam. */
public final class VoidWormPositioner implements SegmentPositioner {

    private static final float DEFAULT_RADIUS = 1.0F;
    private static final float DEFAULT_ANGLE_YAW = (180F + 90F) * Mth.DEG_TO_RAD;
    private static final float DEFAULT_OFFSET_Y = 0.0F;
    private static final float PITCH_APPROACH_RATE = 5.0F;
    private static final float INDEX_0_RADIUS_NORMAL = 0.4F;
    private static final float INDEX_0_RADIUS_TAIL = 0.8F;
    private static final int PORTAL_TRANSITION_THRESHOLD = 1;

    public static final VoidWormPositioner INSTANCE = new VoidWormPositioner();
    private VoidWormPositioner() {}

    @Override
    public void position(SegmentContext ctx) {
        Entity seg = ctx.seg();
        Entity prev = ctx.prev();
        ReflectionPlan refl = ctx.reflection();

        seg.setNoGravity(true);

        float radius = readFloat(seg, refl.field("radius"), DEFAULT_RADIUS);
        float angleYaw = readFloat(seg, refl.field("angleYaw"), DEFAULT_ANGLE_YAW);
        float offsetY = readFloat(seg, refl.field("offsetY"), DEFAULT_OFFSET_Y);
        float wormScale = invokeFloat(refl.method("getWormScale"), seg, 1.0F);

        double restrictRadius = radius;
        if (ctx.index() == 0) {
            boolean tail = invokeBoolean(refl.method("isTail"), seg);
            restrictRadius *= (tail ? INDEX_0_RADIUS_TAIL : INDEX_0_RADIUS_NORMAL);
        }

        double newX = prev.xo + restrictRadius * Math.cos(prev.yRotO * Mth.DEG_TO_RAD + angleYaw);
        // Vertical movement larger than bb-width: use parent's CURRENT y instead of lagged so the
        // chain doesn't lag behind during fast vertical motion.
        double yStretch = Math.abs(prev.getY() - prev.yo) > seg.getBbWidth() ? prev.getY() : prev.yo;
        double newY = yStretch + offsetY * wormScale;
        double newZ = prev.zo + restrictRadius * Math.sin(prev.yRotO * Mth.DEG_TO_RAD + angleYaw);

        EntityDataAccessor<Float> wormYawAcc = refl.accessorTyped("WORM_YAW");
        Field prevWormYawF = refl.field("prevWormYaw");
        if (prevWormYawF != null && wormYawAcc != null) {
            try { prevWormYawF.setFloat(seg, seg.getEntityData().get(wormYawAcc)); }
            catch (IllegalAccessException ignored) {}
        }

        if (ctx.isLivingSeg()) SegmentChainManager.savePrevFrameLiving(ctx.livingSeg());
        else SegmentChainManager.savePrevFrame(seg);

        if (invokeInt(refl.method("getPortalTicks"), seg, 0) > PORTAL_TRANSITION_THRESHOLD) return;

        seg.setPos(newX, newY, newZ);

        double dx = prev.xo - seg.getX();
        double dy = prev.yo - seg.getY();
        double dz = prev.zo - seg.getZ();
        float yawTarget = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float xRotTarget = -((float) (Mth.atan2(dy, Mth.sqrt((float) (dx * dx + dz * dz))) * Mth.RAD_TO_DEG));
        seg.setXRot(Mth.approachDegrees(seg.getXRot(), xRotTarget, PITCH_APPROACH_RATE));

        float prevWormYaw = (wormYawAcc != null) ? seg.getEntityData().get(wormYawAcc) : seg.yRotO;
        float yaw = prevWormYaw + Mth.wrapDegrees(yawTarget - prevWormYaw);
        seg.setYRot(yaw);
        if (wormYawAcc != null) seg.getEntityData().set(wormYawAcc, yaw);

        LivingEntity ls = ctx.livingSeg();
        if (ls != null) {
            ls.yHeadRot = yaw;
            // yBodyRot carries pitch transport (not yaw) for VoidWorm parts.
            ls.yBodyRot = prev.getXRot();
        }
    }

    private static float readFloat(Object instance, Field f, float fallback) {
        if (f == null) return fallback;
        try { return f.getFloat(instance); }
        catch (IllegalAccessException e) { return fallback; }
    }

    private static float invokeFloat(Method m, Object instance, float fallback) {
        if (m == null) return fallback;
        try { return (float) m.invoke(instance); }
        catch (ReflectiveOperationException e) { return fallback; }
    }

    private static int invokeInt(Method m, Object instance, int fallback) {
        if (m == null) return fallback;
        try { return (int) m.invoke(instance); }
        catch (ReflectiveOperationException e) { return fallback; }
    }

    private static boolean invokeBoolean(Method m, Object instance) {
        if (m == null) return false;
        try { return (boolean) m.invoke(instance); }
        catch (ReflectiveOperationException e) { return false; }
    }
}
