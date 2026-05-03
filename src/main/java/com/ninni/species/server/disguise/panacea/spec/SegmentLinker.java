package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Per-segment spawn wiring (parent/child UUIDs, body index, type-specific config). Receives
 *  the full {@code chain} so GumWorm-style back-mutation onto the previous segment is supported. */
@FunctionalInterface
public interface SegmentLinker {
    void link(Entity seg, Entity prev, int index, LivingEntity wearer, LivingEntity disguise,
              List<Entity> chain, ReflectionPlan refl);
}
