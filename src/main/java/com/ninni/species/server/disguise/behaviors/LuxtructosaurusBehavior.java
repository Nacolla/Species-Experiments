package com.ninni.species.server.disguise.behaviors;

import com.ninni.species.api.disguise.DisguiseBehavior;
import com.ninni.species.server.disguise.dsl.CompositeDisguiseBehavior;
import com.ninni.species.server.disguise.dsl.ReflectiveBooleanFlagBehavior;
import com.ninni.species.server.disguise.dsl.WearerPredicates;

/**
 * Luxtructosaurus: composes {@link SauropodBehavior} with a {@code setEnraged(false)} flag-sync to prevent enraged-NBT glow/particle leak.
 */
public final class LuxtructosaurusBehavior {

    public static final DisguiseBehavior INSTANCE = CompositeDisguiseBehavior.of(
            SauropodBehavior.INSTANCE,
            new ReflectiveBooleanFlagBehavior(
                    new ReflectiveBooleanFlagBehavior.FlagSync("setEnraged", WearerPredicates.ALWAYS_FALSE)));

    private LuxtructosaurusBehavior() {}
}
