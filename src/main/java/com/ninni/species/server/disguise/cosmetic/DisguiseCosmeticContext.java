package com.ninni.species.server.disguise.cosmetic;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-thread wearer context for texture-override mixins during disguise body render.
 * Stack-based so nested renders (sub-entity that is itself a wearer) restore the outer
 * wearer on inner pop. {@link ThreadLocal} — push/pop only on the client render thread.
 */
@ApiStatus.Internal
public final class DisguiseCosmeticContext {

    private static final ThreadLocal<Deque<LivingEntity>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private DisguiseCosmeticContext() {}

    public static void push(LivingEntity wearer) {
        STACK.get().push(wearer);
    }

    /** Call inside a {@code finally} to pair every {@link #push}. */
    public static void pop() {
        Deque<LivingEntity> stack = STACK.get();
        if (!stack.isEmpty()) stack.pop();
    }

    /** Innermost wearer, or null when not in a disguise render. */
    public static LivingEntity current() {
        Deque<LivingEntity> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }
}
