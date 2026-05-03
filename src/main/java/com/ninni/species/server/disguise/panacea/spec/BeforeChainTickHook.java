package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

/** Once-per-tick hook running BEFORE the segment loop, for wearer-driven state advances the
 *  chain depends on (VoidWorm WORM_ANGLE accumulator, Anaconda ring-buffer advance). */
@FunctionalInterface
public interface BeforeChainTickHook {
    void run(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl);
}
