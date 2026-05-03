package com.ninni.species.server.disguise.panacea.spec;

/** Writes segment yaw/pitch (plus renderer-read synced fields like {@code WORM_YAW},
 *  {@code BODY_XROT}). Runs after {@link SegmentPositioner} so atan2-toward-parent reads the
 *  just-written position. */
@FunctionalInterface
public interface SegmentRotator {
    void rotate(SegmentContext ctx);
}
