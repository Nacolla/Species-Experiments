package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** Advances a per-wearer {@link YawRingBuffer} from wearer yaw each tick. The disguise's own
 *  yaw history doesn't update reliably without level-ticking, so chains maintain it externally. */
public final class YawRingAdvanceHook implements BeforeChainTickHook {

    private final StateSlot<UUID, YawRingBuffer> slot;

    public YawRingAdvanceHook(StateSlot<UUID, YawRingBuffer> slot) {
        this.slot = slot;
    }

    @Override
    public void run(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        slot.getOrCreate(wearer.getUUID()).advance(wearer.getYRot());
    }
}
