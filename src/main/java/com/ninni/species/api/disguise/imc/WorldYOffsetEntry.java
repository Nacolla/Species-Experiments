package com.ninni.species.api.disguise.imc;

import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#SET_WORLD_Y_OFFSET}. Negative values shift down. */
public record WorldYOffsetEntry(EntityType<?> type, float yOffset) {}
