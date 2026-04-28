package com.ninni.species.api.disguise.imc;

import com.ninni.species.api.disguise.SubEntityProvider;
import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#REGISTER_SUB_ENTITY_PROVIDER}. */
public record SubEntityProviderRegistration(EntityType<?> type, SubEntityProvider provider) {}
