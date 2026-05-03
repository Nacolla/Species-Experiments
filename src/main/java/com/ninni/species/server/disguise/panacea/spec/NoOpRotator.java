package com.ninni.species.server.disguise.panacea.spec;

/** Rotator that does nothing; for chains whose positioner sets all rotation internally. */
public final class NoOpRotator implements SegmentRotator {
    public static final NoOpRotator INSTANCE = new NoOpRotator();
    private NoOpRotator() {}

    @Override
    public void rotate(SegmentContext ctx) {}
}
