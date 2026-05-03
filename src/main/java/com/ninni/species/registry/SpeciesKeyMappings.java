package com.ninni.species.registry;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class SpeciesKeyMappings {

    public static final KeyMapping EXTEND_KEY = new KeyMapping("key.extend", 265, "key.categories.species");
    public static final KeyMapping RETRACT_KEY = new KeyMapping("key.retract", 264, "key.categories.species");
    /** Triggers a per-disguise special action (e.g. Spawn Seal lay-on-side / belly-up toggle).
     *  Routed through {@code DisguiseBehavior.onSpecialAction} and {@code DisguiseCosmetics.onSpecialAction}
     *  so any third-party disguise behavior or cosmetics implementation can hook the same key. */
    public static final KeyMapping DISGUISE_ACTION_KEY = new KeyMapping(
            "key.species.disguise_action", GLFW.GLFW_KEY_G, "key.categories.species");

}
