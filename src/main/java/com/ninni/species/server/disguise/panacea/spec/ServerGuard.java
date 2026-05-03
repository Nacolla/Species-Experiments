package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

/** Server-side one-shot guard at disguise creation; pins state to suppress natural spawn/cooldown logic. */
@FunctionalInterface
public interface ServerGuard {
    void apply(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl);
}
