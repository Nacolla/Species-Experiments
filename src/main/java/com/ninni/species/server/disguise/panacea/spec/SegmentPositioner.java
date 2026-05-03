package com.ninni.species.server.disguise.panacea.spec;

/** Writes the segment's new position; may also set rotation when physics ties them together. */
@FunctionalInterface
public interface SegmentPositioner {
    void position(SegmentContext ctx);
}
