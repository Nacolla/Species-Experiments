package com.ninni.species.server.disguise.panacea.spec;

import com.ninni.species.server.disguise.panacea.SegmentChainManager;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/** Cliff-fall clamp for parent-Y-derived chains so trailing segments don't suspend mid-air;
 *  {@code cliffEnter}/{@code cliffExit} hysteresis avoids flicker on stair traversal. */
public final class HystereticGroundClampHook implements TickHook {

    private final double cliffEnter;
    private final double cliffExit;
    private final double cliffFallRate;
    private final BooleanSupplier enabled;
    private final ConcurrentHashMap<Integer, Double> stableY = new ConcurrentHashMap<>();

    public HystereticGroundClampHook(double cliffEnter, double cliffExit, double cliffFallRate,
                                     BooleanSupplier enabled) {
        this.cliffEnter = cliffEnter;
        this.cliffExit = cliffExit;
        this.cliffFallRate = cliffFallRate;
        this.enabled = enabled;
    }

    @Override
    public void run(SegmentContext ctx) {
        if (!enabled.getAsBoolean()) return;
        Double oldY = ctx.scratch(CapturePrevFrameHook.KEY_OLD_Y);
        if (oldY == null) return;

        Entity seg = ctx.seg();
        double naturalY = seg.getY();
        double surfaceY = SegmentChainManager.computeSurfaceY(seg.level(), seg.getX(), naturalY, seg.getZ());

        Double prevStable = stableY.get(seg.getId());
        if (prevStable != null) {
            if (oldY - surfaceY < cliffExit) {
                stableY.remove(seg.getId());
            } else {
                double settled = Math.max(prevStable - cliffFallRate, surfaceY);
                seg.setPos(seg.getX(), settled, seg.getZ());
                stableY.put(seg.getId(), settled);
            }
        } else if (naturalY - surfaceY > cliffEnter) {
            double seed = Math.max(oldY - cliffFallRate, surfaceY);
            seg.setPos(seg.getX(), seed, seg.getZ());
            stableY.put(seg.getId(), seed);
        }
    }
}
