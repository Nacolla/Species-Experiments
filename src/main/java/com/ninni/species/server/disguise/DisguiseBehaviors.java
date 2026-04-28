package com.ninni.species.server.disguise;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.server.disguise.behaviors.EnderDragonBehavior;
import com.ninni.species.server.disguise.behaviors.GloomothBehavior;
import com.ninni.species.server.disguise.behaviors.HippogryphBehavior;
import com.ninni.species.server.disguise.behaviors.HullbackBehavior;
import com.ninni.species.server.disguise.behaviors.IafDragonBehavior;
import com.ninni.species.server.disguise.behaviors.LuxtructosaurusBehavior;
import com.ninni.species.server.disguise.behaviors.SauropodBehavior;
import com.ninni.species.server.disguise.behaviors.StraightenViaBridgeBehavior;
import com.ninni.species.server.disguise.behaviors.TremorsaurusBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Registers Species' built-in {@link DisguiseBehavior}s during setup; modded entries use
 * {@link #registerIfPresent} so soft-deps register only when the entity type exists.
 */
public final class DisguiseBehaviors {

    private DisguiseBehaviors() {}

    public static void registerDefaults() {
        // Force class load so static initialisers of self-registering global behaviors run.
        try {
            Class.forName(com.ninni.species.server.disguise.dsl.AutoFlightSyncBehavior.class.getName());
        } catch (ClassNotFoundException ignored) {}

        // Vanilla
        DisguiseBehaviorRegistry.register(EntityType.ENDER_DRAGON, EnderDragonBehavior.INSTANCE);

        // Modded — soft-dep registration. Only register if the entity type exists.
        registerIfPresent("whaleborne", "hullback", HullbackBehavior.INSTANCE);

        // Ice and Fire dragons — every dragon family share EntityDragonBase, so the
        // same behavior plugs in for fire/ice/lightning variants.
        registerIfPresent("iceandfire", "fire_dragon", IafDragonBehavior.INSTANCE);
        registerIfPresent("iceandfire", "ice_dragon", IafDragonBehavior.INSTANCE);
        registerIfPresent("iceandfire", "lightning_dragon", IafDragonBehavior.INSTANCE);

        // IaF flight mobs: AutoFlightSync handles flag-sync globally; only the rotation lock
        // remains type-specific. Hippogryph keeps its bespoke entry for the airBorneCounter clamp.
        registerIfPresent("iceandfire", "amphithere", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "hippogryph", HippogryphBehavior.INSTANCE);
        registerIfPresent("iceandfire", "myrmex_royal", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "stymphalian_bird", StraightenViaBridgeBehavior.INSTANCE);

        // IaF entities needing only the rotation lock: each has unconditional faceTarget in setupAnim
        // that distorts head/neck in inventory preview without it. Straighten flag is a no-op for these
        // models but the rotation lock alone is sufficient.
        registerIfPresent("iceandfire", "cockatrice", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "cyclops", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "gorgon", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "troll", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "siren", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "ghost", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "pixie", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "myrmex_worker", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "myrmex_soldier", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "myrmex_sentinel", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "myrmex_queen", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "dread_thrall", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "dread_ghoul", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "dread_lich", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("iceandfire", "dread_knight", StraightenViaBridgeBehavior.INSTANCE);

        // AC sauropod-class. Atlatitan: plain walk fix. Luxtructosaurus: same + ENRAGED-flag
        // suppressor (tick spawns fire-spit particles and glow when isEnraged()=true, leaking into the disguise).
        registerIfPresent("alexscaves", "atlatitan", SauropodBehavior.INSTANCE);
        registerIfPresent("alexscaves", "luxtructosaurus", LuxtructosaurusBehavior.INSTANCE);

        // AC Subterranodon + Vesper: AutoFlightSync handles flight + ceiling-cling flags;
        // only the rotation lock remains.
        registerIfPresent("alexscaves", "subterranodon", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "vesper", StraightenViaBridgeBehavior.INSTANCE);

        // AC entities needing inventory rotation lock and/or straighten-bridge flag.
        // StraightenViaBridgeBehavior applies both: bridge flag sets model.straighten=true
        // (gates sub-entity rotation on Hullbreaker/Tremorzilla/GossamerWorm); rotation lock
        // forces yHeadRot=yBodyRot so faceTarget(netHeadYaw, headPitch) reads zero
        // (TremorzillaModel:525-526,667-671; TremorsaurusModel:400).
        registerIfPresent("alexscaves", "hullbreaker", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "tremorzilla", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "gossamer_worm", StraightenViaBridgeBehavior.INSTANCE);
        // Additional AC mobs with unconditional faceTarget in setupAnim.
        // Models verified: MagnetronModel:79, RaycatModel:132, TeletorModel:140,
        // UnderzealotModel:194, VallumraptorModel:586.
        registerIfPresent("alexscaves", "magnetron", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "raycat", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "teletor", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "underzealot", StraightenViaBridgeBehavior.INSTANCE);
        registerIfPresent("alexscaves", "vallumraptor", StraightenViaBridgeBehavior.INSTANCE);
        // Gloomoth: FLYING flag + flightRoll from wearer yaw delta.
        // Bespoke because aiStep zeroes yRotO-yRot; see GloomothBehavior.
        registerIfPresent("alexscaves", "gloomoth", GloomothBehavior.INSTANCE);
        // Forsaken: unconditional faceTarget(netHeadYaw, headPitch, 1, neck, skull)
        // at ForsakenModel.java:644 sinks skull into chest during inventory preview.
        // Rotation-lock forces netHeadYaw→0; straighten flag is a no-op (no field).
        registerIfPresent("alexscaves", "forsaken", StraightenViaBridgeBehavior.INSTANCE);
        // Tremorsaurus: bridge fix + dampened walkSpeed=0.8F leg cycle.
        registerIfPresent("alexscaves", "tremorsaurus", TremorsaurusBehavior.INSTANCE);
    }

    private static void registerIfPresent(String namespace, String path, DisguiseBehavior behavior) {
        ResourceLocation id = new ResourceLocation(namespace, path);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type != null) {
            DisguiseBehaviorRegistry.register(type, behavior);
        }
    }
}
