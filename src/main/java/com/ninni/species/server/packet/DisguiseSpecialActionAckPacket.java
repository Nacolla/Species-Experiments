package com.ninni.species.server.packet;

import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.server.disguise.DisguiseBehaviorRegistry;
import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client broadcast paired with {@link DisguiseSpecialActionPacket}. Each client runs
 *  {@code onSpecialAction} on its local disguise body so visual state updates without the
 *  disguise being a tracked level entity. */
public final class DisguiseSpecialActionAckPacket {

    private final int wearerId;

    public DisguiseSpecialActionAckPacket(int wearerId) {
        this.wearerId = wearerId;
    }

    public static void write(DisguiseSpecialActionAckPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.wearerId);
    }

    public static DisguiseSpecialActionAckPacket read(FriendlyByteBuf buf) {
        return new DisguiseSpecialActionAckPacket(buf.readInt());
    }

    public static void handle(DisguiseSpecialActionAckPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(DisguiseSpecialActionAckPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!(mc.level.getEntity(msg.wearerId) instanceof LivingEntity wearer)) return;
        LivingEntity disguise = ((LivingEntityAccess) wearer).getDisguisedEntity();
        if (disguise == null) return;
        com.ninni.species.api.disguise.ActionContext context =
                com.ninni.species.api.disguise.ActionContext.of(wearer);
        try {
            DisguiseBehaviorRegistry.get(disguise).onSpecialAction(wearer, disguise, context);
            DisguiseCosmeticRegistry.get(disguise).onSpecialAction(wearer, disguise, context);
        } catch (Throwable ignored) {}
    }
}
