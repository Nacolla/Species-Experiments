package com.ninni.species.server.disguise.panacea.spec;

/** Drives per-segment animation state for the renderer; common impl mirrors wearer.walkAnimation. */
@FunctionalInterface
public interface AnimDriver {
    void drive(SegmentContext ctx);
}
