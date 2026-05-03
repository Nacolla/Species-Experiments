package com.ninni.species.registry;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpeciesConfig {

    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue PLAY_DISGUISE_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COSMETICS_DEMO;
    public static final ForgeConfigSpec.BooleanValue SEGMENT_CHAIN_GROUND_CLAMP;
    public static final ForgeConfigSpec.BooleanValue REDIRECT_SEGMENT_TO_HEAD;
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
                        "chicken disguise plays the ghast scream on death, spider disguise renders",
                        "shrunk to 60% world scale.",
                        "Off by default — flip to true to verify the Cosmetics API end-to-end via the",
                        "vanilla-mob overrides. The demos only fire when wearing a Wicked Mask",
                        "disguised as the relevant vanilla mob, not for natural pigs/cows/etc."
                )
                .define("enableCosmeticsDemo", false);
        SEGMENT_CHAIN_GROUND_CLAMP = SERVER_BUILDER
                .comment(
                        "If true, swimming-style chain disguises (e.g. Gum Worm) clamp each segment's",
                        "Y to the terrain surface so segments swim through the ground/sand instead of",
                        "trailing the wearer's view pitch into the air. Disable for free-floating chains."
                )
                .define("segmentChainGroundClamp", true);
        REDIRECT_SEGMENT_TO_HEAD = SERVER_BUILDER
                .comment(
                        "If true, sneak+right-clicking a chain segment (centipede body/tail, anaconda",
                        "part, gum-worm segment, void-worm part, bone-serpent part) imprints the",
                        "chain's head so the disguise renders the full body. The actual segment id",
                        "is preserved in the mask NBT — toggling this off and rejoining the world",
                        "renders just the clicked segment."
                )
                .define("redirectSegmentToHead", true);
        SERVER_BUILDER.pop();
        SERVER_SPEC = SERVER_BUILDER.build();
    }

    private SpeciesConfig() {}
}
