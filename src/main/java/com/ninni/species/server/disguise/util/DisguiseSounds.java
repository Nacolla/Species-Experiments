package com.ninni.species.server.disguise.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Helpers for one-shot sounds played from disguise behaviors. */
public final class DisguiseSounds {

    private DisguiseSounds() {}

    /**
     * Server-side broadcast at the wearer's position. Skipped on the client to avoid double
     * playback (the server's broadcast already reaches the local client).
     */
    public static void playServerBroadcast(LivingEntity wearer, SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (wearer.level().isClientSide) return;
        wearer.level().playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), sound, source, volume, pitch);
    }

    /** Convenience: {@link SoundSource#PLAYERS} channel, default pitch 1.0. */
    public static void playServerBroadcast(LivingEntity wearer, SoundEvent sound, float volume) {
        playServerBroadcast(wearer, sound, SoundSource.PLAYERS, volume, 1.0F);
    }
}
