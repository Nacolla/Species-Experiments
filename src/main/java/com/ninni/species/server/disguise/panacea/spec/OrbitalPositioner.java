package com.ninni.species.server.disguise.panacea.spec;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

/**
 * Positions the segment on a circle around the parent's lagged frame. Reads radius/angleYaw/offsetY
 * from per-segment fields when present so each part can override the configured defaults.
 */
public final class OrbitalPositioner implements SegmentPositioner {

    private final float defaultRadius;
    private final float defaultAngleYaw;
    private final float defaultOffsetY;
    private final String radiusFieldKey;
    private final String angleYawFieldKey;
    private final String offsetYFieldKey;

    public OrbitalPositioner(float defaultRadius, float defaultAngleYaw, float defaultOffsetY,
                             String radiusFieldKey, String angleYawFieldKey, String offsetYFieldKey) {
        this.defaultRadius = defaultRadius;
        this.defaultAngleYaw = defaultAngleYaw;
        this.defaultOffsetY = defaultOffsetY;
        this.radiusFieldKey = radiusFieldKey;
        this.angleYawFieldKey = angleYawFieldKey;
        this.offsetYFieldKey = offsetYFieldKey;
    }

    @Override
    public void position(SegmentContext ctx) {
        Entity seg = ctx.seg();
        Entity prev = ctx.prev();
        ReflectionPlan refl = ctx.reflection();

        seg.setNoGravity(true);

        float radius = readFloat(seg, refl.field(radiusFieldKey), defaultRadius);
        float angleYaw = readFloat(seg, refl.field(angleYawFieldKey), defaultAngleYaw);
        float offsetY = readFloat(seg, refl.field(offsetYFieldKey), defaultOffsetY);

        double newX = prev.xo + radius * Math.cos(prev.yRotO * Mth.DEG_TO_RAD + angleYaw);
        double newY = prev.yo + offsetY;
        double newZ = prev.zo + radius * Math.sin(prev.yRotO * Mth.DEG_TO_RAD + angleYaw);

        if (ctx.isLivingSeg()) SegmentChainManager.savePrevFrameLiving(ctx.livingSeg());
        else SegmentChainManager.savePrevFrame(seg);

        seg.setPos(newX, newY, newZ);
    }

    private static float readFloat(Object instance, Field f, float fallback) {
        if (f == null) return fallback;
        try { return f.getFloat(instance); }
        catch (IllegalAccessException e) { return fallback; }
    }
}
