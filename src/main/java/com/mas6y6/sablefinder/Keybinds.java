package com.mas6y6.sablefinder;

import com.mojang.blaze3d.platform.InputConstants;
import cpw.mods.util.Lazy;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final Lazy<KeyMapping> REFRESH_KEY = Lazy.of(() -> {
        return new KeyMapping(
                "key.sableFinder.refresh_key",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.sablefinder"
        );
    });

    public static final Lazy<KeyMapping> GO_TO_NEXT_CONTRAPTION_KEY = Lazy.of(() -> {
        return new KeyMapping(
                "key.sableFinder.next_contraption",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                "key.categories.sablefinder"
        );
    });

    public static final Lazy<KeyMapping> GO_TO_PREVIOUS_CONTRAPTION_KEY = Lazy.of(() -> {
        return new KeyMapping(
                "key.sableFinder.previous_contraption",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                "key.categories.sablefinder"
        );
    });
}
