package com.ninni.species.api.disguise;

import net.minecraft.world.entity.LivingEntity;

/**
 * Lambda-friendly handler for the special-action keybind. Used by
 * {@link DisguiseBehavior#specialAction(SpecialActionHandler)}.
 */
@FunctionalInterface
public interface SpecialActionHandler {
    void handle(LivingEntity wearer, LivingEntity disguise, ActionContext context);
}
