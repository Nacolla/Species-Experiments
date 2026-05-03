package com.ninni.species.server.disguise.panacea.spec;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/** Pins CHILD_UUID = wearer.UUID so {@code getChild()} resolves and the head's spawn-loop is skipped. */
public final class ChildUuidServerGuard implements ServerGuard {

    private final String childIdMethodKey;

    public ChildUuidServerGuard(String childIdMethodKey) {
        this.childIdMethodKey = childIdMethodKey;
    }

    @Override
    public void apply(LivingEntity wearer, LivingEntity disguise, ReflectionPlan refl) {
        Method m = refl.method(childIdMethodKey);
        if (m == null) return;
        try { m.invoke(disguise, wearer.getUUID()); } catch (ReflectiveOperationException ignored) {}
    }
}
