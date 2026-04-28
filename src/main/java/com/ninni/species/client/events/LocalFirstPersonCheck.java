package com.ninni.species.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only "wearer == local player AND camera is first-person" probe; isolates {@link Minecraft}
 * away from server-loaded classes for the {@link com.ninni.species.server.disguise.DisguiseTickContext} push.
 */
@OnlyIn(Dist.CLIENT)
public final class LocalFirstPersonCheck {

    private LocalFirstPersonCheck() {}

    public static boolean isLocalFirstPersonWearer(LivingEntity wearer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        if (wearer != mc.player) return false;
        return mc.options != null
                && mc.options.getCameraType() != null
                && mc.options.getCameraType().isFirstPerson();
    }
}
