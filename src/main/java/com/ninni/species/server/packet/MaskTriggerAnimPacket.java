package com.ninni.species.server.packet;

import com.ninni.species.server.item.util.WickedMaskAnim;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class MaskTriggerAnimPacket {
    private final int wearerId;
    @Nullable
    private final String controllerName;
    private final String animName;

    public MaskTriggerAnimPacket(int wearerId, @Nullable String controllerName, String animName) {
        this.wearerId = wearerId;
        this.controllerName = controllerName;
        this.animName = animName;
    }

    public static void write(MaskTriggerAnimPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.wearerId);
        buf.writeBoolean(packet.controllerName != null);
        if (packet.controllerName != null) buf.writeUtf(packet.controllerName);
        buf.writeUtf(packet.animName);
    }

    public static MaskTriggerAnimPacket read(FriendlyByteBuf buf) {
        int wearerId = buf.readInt();
        String controller = buf.readBoolean() ? buf.readUtf() : null;
        String animName = buf.readUtf();
        return new MaskTriggerAnimPacket(wearerId, controller, animName);
    }

    public static void handle(MaskTriggerAnimPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MaskTriggerAnimPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getEntity(packet.wearerId) instanceof LivingEntity wearer) {
            WickedMaskAnim.applyToDisguise(wearer, packet.controllerName, packet.animName);
        }
    }
}
