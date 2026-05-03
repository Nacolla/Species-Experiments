package com.ninni.species.server.disguise.panacea.spec;

/** Returns false to skip a segment's full tick step (positioner, rotator, propagator, anim,
 *  hooks). Mirrors VoidWorm's portal-transition freeze. */
@FunctionalInterface
public interface TickGuard {
    boolean shouldTick(SegmentContext ctx);
}
