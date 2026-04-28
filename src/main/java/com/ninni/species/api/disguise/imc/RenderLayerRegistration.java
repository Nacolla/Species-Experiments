package com.ninni.species.api.disguise.imc;

import com.ninni.species.api.disguise.DisguiseRenderLayer;
import net.minecraft.world.entity.EntityType;

/** IMC payload for {@link SpeciesIMCKeys#REGISTER_RENDER_LAYER}. */
public record RenderLayerRegistration(EntityType<?> type, DisguiseRenderLayer layer) {}
