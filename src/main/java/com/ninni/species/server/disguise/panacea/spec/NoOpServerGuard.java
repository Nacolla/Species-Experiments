package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

/** Server guard that does nothing. Used for chains whose head class has no spawn-loop to suppress. */
public final class NoOpServerGuard implements ServerGuard {
    public static final NoOpServerGuard INSTANCE = new NoOpServerGuard();
    private NoOpServerGuard() {}

    @Override
    public void apply(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {}
}
