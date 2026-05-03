package com.ninni.species.server.disguise.panacea.spec;

/** Cascades synced state down the chain; runs after position/rotation. */
@FunctionalInterface
public interface StatePropagator {
    void propagate(SegmentContext ctx);
}
