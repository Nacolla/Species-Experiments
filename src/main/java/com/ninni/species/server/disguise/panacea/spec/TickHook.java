package com.ninni.species.server.disguise.panacea.spec;

/** Per-tick hook running before/after the {@link SegmentPositioner}; pre/post pairs
 *  communicate through {@link SegmentContext}'s scratch map. */
@FunctionalInterface
public interface TickHook {
    void run(SegmentContext ctx);
}
