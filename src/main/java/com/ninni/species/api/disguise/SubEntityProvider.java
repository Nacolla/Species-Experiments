package com.ninni.species.api.disguise;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;

/**
 * Provides independent client-side auxiliary entities accompanying a disguise (segment chains, paired companions);
 * Forge {@link net.minecraftforge.entity.PartEntity} children are auto-handled, not via this. Register via {@link SpeciesAPI} or IMC.
 */
@FunctionalInterface
public interface SubEntityProvider {

    /**
     * Returns sub-entities in render order. Each entity's world position must be set;
     * callers compute offsets relative to the wearer. Returning empty is valid when
     * segments are absent or the relevant mod is not loaded.
     */
    Iterable<? extends Entity> getSubEntities(LivingEntity wearer, LivingEntity disguise);

    /** A no-op provider that always returns an empty iterable. */
    SubEntityProvider EMPTY = (wearer, disguise) -> Collections.emptyList();
}
