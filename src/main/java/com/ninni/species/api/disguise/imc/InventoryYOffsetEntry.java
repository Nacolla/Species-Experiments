package com.ninni.species.api.disguise.imc;

import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#SET_INVENTORY_Y_OFFSET}. Negative values shift down. */
public record InventoryYOffsetEntry(EntityType<?> type, float yOffset) {}
