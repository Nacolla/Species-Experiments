package com.ninni.species.server.packet;

import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesNetwork;
import com.ninni.species.server.disguise.DisguiseBehaviorRegistry;
import com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Client → server: wearer pressed {@code DISGUISE_ACTION_KEY}. Server runs the authoritative
 *  {@code onSpecialAction}, then broadcasts {@link DisguiseSpecialActionAckPacket} so every
 *  tracking client replays it (the disguise isn't a level entity, so synced data won't reach). */
public final class DisguiseSpecialActionPacket {

    public DisguiseSpecialActionPacket() {}

    public static void write(DisguiseSpecialActionPacket msg, FriendlyByteBuf buf) {}

    public static DisguiseSpecialActionPacket read(FriendlyByteBuf buf) {
        return new DisguiseSpecialActionPacket();
    }

    public static void handle(DisguiseSpecialActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            LivingEntity disguise = ((LivingEntityAccess) player).getDisguisedEntity();
            if (disguise == null) return;
            com.ninni.species.api.disguise.ActionContext context =
                    com.ninni.species.api.disguise.ActionContext.of(player);
            try {
                DisguiseBehaviorRegistry.get(disguise).onSpecialAction(player, disguise, context);
                DisguiseCosmeticRegistry.get(disguise).onSpecialAction(player, disguise, context);
            } catch (Throwable ignored) {}
            // Broadcast to tracking clients (and sender) so each side runs the action locally.
            // Context is recomputed per-side on the ack so each viewer's local wearer state drives it.
            SpeciesNetwork.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new DisguiseSpecialActionAckPacket(player.getId()));
        });
        ctx.get().setPacketHandled(true);
    }
}
