package com.ninni.species.server.item.util;

import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesNetwork;
import com.ninni.species.server.packet.MaskTriggerAnimPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Bridges WickedMask disguise animations to GeckoLib's {@code triggerAnim} via a wearer-keyed packet,
 * since the disguise is not in any level and GeckoLib's network keys by entity id. Soft-dep, reflective.
 */
public final class WickedMaskAnim {
    private static volatile boolean reflectionInited;
    private static Class<?> geoEntityClass;
    private static Method triggerAnimMethod;

    private WickedMaskAnim() {}

    public static void triggerDisguiseAnim(LivingEntity wearer, @Nullable String controllerName, String animName) {
        if (wearer == null) return;
        if (wearer.level().isClientSide) {
            applyToDisguise(wearer, controllerName, animName);
        } else {
            SpeciesNetwork.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> wearer),
                    new MaskTriggerAnimPacket(wearer.getId(), controllerName, animName)
            );
        }
    }

    public static void applyToDisguise(LivingEntity wearer, @Nullable String controllerName, String animName) {
        initReflection();
        if (triggerAnimMethod == null) return;
        LivingEntity disguise = ((LivingEntityAccess) wearer).getDisguisedEntity();
        if (disguise == null || !geoEntityClass.isInstance(disguise)) return;
        try {
            triggerAnimMethod.invoke(disguise, controllerName, animName);
        } catch (ReflectiveOperationException ignored) {}
    }

    private static void initReflection() {
        if (reflectionInited) return;
        synchronized (WickedMaskAnim.class) {
            if (reflectionInited) return;
            reflectionInited = true;
            if (!ModList.get().isLoaded("geckolib")) return;
            try {
                geoEntityClass = Class.forName("software.bernie.geckolib.animatable.GeoEntity");
                triggerAnimMethod = geoEntityClass.getMethod("triggerAnim", String.class, String.class);
            } catch (ReflectiveOperationException ignored) {
                geoEntityClass = null;
                triggerAnimMethod = null;
            }
        }
    }
}
