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

    /** Sub-entities in render order with world positions set. Empty is valid (segments absent
     *  or mod not loaded). */
    Iterable<? extends Entity> getSubEntities(LivingEntity wearer, LivingEntity disguise);

    /** A no-op provider that always returns an empty iterable. */
    SubEntityProvider EMPTY = (wearer, disguise) -> Collections.emptyList();
}
