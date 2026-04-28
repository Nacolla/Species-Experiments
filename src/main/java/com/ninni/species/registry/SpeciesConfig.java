package com.ninni.species.registry;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpeciesConfig {

    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue PLAY_DISGUISE_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COSMETICS_DEMO;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        SERVER_BUILDER.push("wickedMask");
        PLAY_DISGUISE_SOUNDS = SERVER_BUILDER
                .comment(
                        "If true, the Wicked Mask disguise will emit the disguised mob's sounds",
                        "(ambient, hurt, step, etc.) on top of the wearer's own sounds.",
                        "Default false to avoid sound stacking and exploits."
                )
                .define("playDisguiseSounds", false);
        ENABLE_COSMETICS_DEMO = SERVER_BUILDER
                .comment(
                        "Enables the DisguiseCosmetics demo registrations: vanilla pig disguise",
                        "plays villager hurt/death sounds, vanilla cow disguise uses the red",
                        "mooshroom texture, vanilla sheep disguise emits heart particles, vanilla",
                        "chicken disguise plays the ghast scream on death.",
                        "ON by default — the demo is a test harness verifying the Cosmetics API",
                        "end-to-end. Set to false if you find the vanilla-mob cosmetic overrides",
                        "intrusive (the demos only fire when wearing a Wicked Mask disguised as",
                        "the relevant vanilla mob, not for natural pigs/cows/sheep/chickens)."
                )
                .define("enableCosmeticsDemo", true);
        SERVER_BUILDER.pop();
        SERVER_SPEC = SERVER_BUILDER.build();
    }

    private SpeciesConfig() {}
}
