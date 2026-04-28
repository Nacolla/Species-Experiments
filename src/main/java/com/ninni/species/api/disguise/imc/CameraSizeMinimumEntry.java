package com.ninni.species.api.disguise.imc;

import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#SET_CAMERA_SIZE_MINIMUM}. Visual size pin in blocks. */
public record CameraSizeMinimumEntry(EntityType<?> type, double minVisualSize) {}
