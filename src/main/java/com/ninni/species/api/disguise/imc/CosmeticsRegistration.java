package com.ninni.species.api.disguise.imc;

import com.ninni.species.api.disguise.DisguiseCosmetics;
import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#REGISTER_COSMETICS} and {@link SpeciesIMCKeys#COMPOSE_COSMETICS}. */
public record CosmeticsRegistration(EntityType<?> type, DisguiseCosmetics cosmetics) {}
