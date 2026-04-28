package com.ninni.species.api.disguise.imc;

import com.ninni.species.api.disguise.DisguiseBehavior;
import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#REGISTER_BEHAVIOR} and {@link SpeciesIMCKeys#COMPOSE_BEHAVIOR}. */
public record BehaviorRegistration(EntityType<?> type, DisguiseBehavior behavior) {}
