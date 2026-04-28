package com.ninni.species.client.events;

import com.ninni.species.mixin_util.EntityRenderDispatcherAccess;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only "is this an inventory preview render?" probe via {@link EntityRenderDispatcherAccess};
 * centralised so behaviors don't each pull {@code Minecraft} into server-loaded classes.
 */
@OnlyIn(Dist.CLIENT)
public final class InventoryRenderCheck {

    private InventoryRenderCheck() {}

    public static boolean isInventoryRender() {
        return Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access
                && access.getRenderingInventoryEntity();
    }
}
