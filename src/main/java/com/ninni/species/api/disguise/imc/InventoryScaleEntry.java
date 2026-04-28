package com.ninni.species.api.disguise.imc;

import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#SET_INVENTORY_SCALE}. Scale is multiplied on top of auto-fit. */
public record InventoryScaleEntry(EntityType<?> type, float scale) {}
