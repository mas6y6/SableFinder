package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.config.ConfigServer;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ConfigScreen extends BaseOwoScreen<FlowLayout> {

    private static final int INVALID_COLOR = 0xFFAA0000;

    private final @Nullable Screen parent;

    private @Nullable TextBoxComponent suggestionTtlBox;
    private @Nullable TextBoxComponent saveIntervalBox;

    private ConfigScreen(@Nullable Screen parent) {
        this.parent = parent;
    }

    public static @NotNull Screen create(@Nullable Screen parent) {
        return new ConfigScreen(parent);
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .padding(Insets.of(8))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout main = Containers.verticalFlow(Sizing.fill(90), Sizing.fill(80));
        main.surface(Surface.DARK_PANEL);
        main.padding(Insets.of(6));
        main.gap(4);

        main.child(OwoUtils.wrappingLabel(Component.translatable("sablefinder.config.title")));
        main.child(OwoUtils.wrappingLabel(
                Component.translatable("sablefinder.config.subtitle").withColor(0x8A8A8A)
        ));

        FlowLayout options = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        options.gap(4);

        options.child(createIntOption(
                "sablefinder.config.suggestion_cache_ttl_ms",
                "sablefinder.config.suggestion_cache_ttl_ms.tooltip",
                ConfigServer.SUGGESTION_CACHE_TTL_MS,
                box -> this.suggestionTtlBox = box
        ));

        options.child(createIntOption(
                "sablefinder.config.sub_level_save_min_interval_ms",
                "sablefinder.config.sub_level_save_min_interval_ms.tooltip",
                ConfigServer.SUB_LEVEL_SAVE_MIN_INTERVAL_MS,
                box -> this.saveIntervalBox = box
        ));

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                options
        );
        scroll.scrollbarThiccness(4);

        main.child(scroll);
        main.child(createButtonRow());

        rootComponent.child(main);
    }

    private @NotNull FlowLayout createButtonRow() {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(4);

        ButtonComponent save = Components.button(
                Component.translatable("sablefinder.config.save"),
                button -> {
                    saveAll();
                    onClose();
                }
        );
        save.horizontalSizing(Sizing.fill(100));

        ButtonComponent reset = Components.button(
                Component.translatable("sablefinder.config.reset"),
                button -> {
                    ConfigServer.SUGGESTION_CACHE_TTL_MS.set(ConfigServer.SUGGESTION_CACHE_TTL_MS.getDefault());
                    ConfigServer.SUB_LEVEL_SAVE_MIN_INTERVAL_MS.set(ConfigServer.SUB_LEVEL_SAVE_MIN_INTERVAL_MS.getDefault());
                    ConfigServer.SPEC.save();

                    this.suggestionTtlBox.text(String.valueOf(ConfigServer.SUGGESTION_CACHE_TTL_MS.getDefault()));
                    this.saveIntervalBox.text(String.valueOf(ConfigServer.SUB_LEVEL_SAVE_MIN_INTERVAL_MS.getDefault()));
                }
        );
        reset.horizontalSizing(Sizing.fixed(60));

        ButtonComponent done = Components.button(
                Component.translatable("gui.done"),
                button -> onClose()
        );
        done.horizontalSizing(Sizing.fixed(60));

        row.child(save);
        row.child(reset);
        row.child(done);

        return row;
    }

    private @NotNull FlowLayout createIntOption(
            @NotNull String labelKey,
            @NotNull String tooltipKey,
            @NotNull ModConfigSpec.IntValue value,
            @NotNull Consumer<TextBoxComponent> onCreate
    ) {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.BLANK.and(Surface.outline(0xFF5F6368)));
        card.padding(Insets.of(4));
        card.gap(2);

        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(14));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(4);

        LabelComponent label = Components.label(Component.translatable(labelKey));
        label.horizontalSizing(Sizing.fill(100));

        TextBoxComponent box = Components.textBox(Sizing.fixed(90), String.valueOf(value.get()));
        box.tooltip(Component.translatable(tooltipKey));

        ButtonComponent reset = Components.button(Component.literal("↺"), button -> {
            box.text(String.valueOf(value.getDefault()));
            UISounds.playInteractionSound();
        });
        reset.horizontalSizing(Sizing.fixed(14));

        row.child(label);
        row.child(box);
        row.child(reset);

        card.child(row);
        onCreate.accept(box);

        return card;
    }

    private void saveAll() {
        applyInt(ConfigServer.SUGGESTION_CACHE_TTL_MS, this.suggestionTtlBox);
        applyInt(ConfigServer.SUB_LEVEL_SAVE_MIN_INTERVAL_MS, this.saveIntervalBox);

        ConfigServer.SPEC.save();
    }

    private void applyInt(@NotNull ModConfigSpec.IntValue target, @Nullable TextBoxComponent box) {
        if (box == null) {
            return;
        }

        final int parsed;
        try {
            parsed = Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException exception) {
            box.setTextColor(INVALID_COLOR);
            return;
        }

        final int min = minOf(target);
        final int max = maxOf(target);

        if (parsed < min || parsed > max) {
            box.setTextColor(INVALID_COLOR);
            return;
        }

        box.setTextColor(TextBoxComponent.DEFAULT_TEXT_COLOR);
        target.set(parsed);
    }

    private int minOf(@NotNull ModConfigSpec.IntValue value) {
        final ModConfigSpec.Range<Integer> range = value.getSpec().getRange();
        assert range != null;
        return range.getMin();
    }

    private int maxOf(@NotNull ModConfigSpec.IntValue value) {
        final ModConfigSpec.Range<Integer> range = value.getSpec().getRange();
        assert range != null;
        return range.getMax();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}