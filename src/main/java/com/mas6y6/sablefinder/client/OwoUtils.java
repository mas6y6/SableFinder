package com.mas6y6.sablefinder.client;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

// i hate the library name so much
public class OwoUtils {
    public static LabelComponent wrappingLabel(Component text) {
        return (LabelComponent) Components.label(text).horizontalSizing(Sizing.fill(100));
    }

    public static @NotNull Component rotateLabel(boolean enabled) {
        return Component.translatable(
                "sablefinder.ui.rot",
                Component.translatable(enabled ? "sablefinder.ui.on" : "sablefinder.ui.off")
        );
    }

    public static @NotNull Component wireframeLabel(boolean enabled) {
        return Component.translatable(
                "sablefinder.ui.wf",
                Component.translatable(enabled ? "sablefinder.ui.on" : "sablefinder.ui.off")
        );
    }

    public static @NotNull FlowLayout createSectionCard(Component title) {
        FlowLayout card = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        card.surface(Surface.DARK_PANEL);
        card.padding(Insets.of(4));
        card.gap(2);
        card.child(wrappingLabel(title));

        return card;
    }

    public static @NotNull FlowLayout createDataRow(@NotNull Component label, Component @NotNull ... components) {
        FlowLayout row = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        row.gap(1);
        row.child(wrappingLabel(Component.literal("§7").append(label)));

        Arrays.stream(components).forEach(component ->
                row.child(wrappingLabel(component))
        );

        if (components.length > 0) {
            StringBuilder copyText = new StringBuilder();

            for (Component component : components) {
                if (!copyText.isEmpty()) {
                    copyText.append(' ');
                }

                copyText.append(component.getString());
            }

            row.child(createCopyButton(
                    Component.literal(copyText.toString().replaceAll("§.", ""))
            ));
        }

        return row;
    }

    public static @NotNull ButtonComponent createCopyButton(@NotNull Component text) {
        ButtonComponent copy = (ButtonComponent) Components.button(
                Component.translatable("sablefinder.ui.button.copy"),
                button -> {
                    button.setMessage(Component.translatable("sablefinder.ui.copied"));

                    Minecraft.getInstance().keyboardHandler.setClipboard(text.getString());
                }
        ).tooltip(text);

        copy.horizontalSizing(Sizing.fixed(30));
        copy.verticalSizing(Sizing.fixed(10));

        return copy;
    }

    public static @NotNull ButtonComponent fullWidthButton(Component label, @NotNull Consumer<ButtonComponent> action) {
        ButtonComponent button = Components.button(label, action);
        button.horizontalSizing(Sizing.fill(100));
        button.verticalSizing(Sizing.fixed(20));

        return button;
    }
}
