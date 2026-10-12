package com.mas6y6.sablefinder.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SearchScreen extends BaseOwoScreen<FlowLayout> {
    private final SableFinderScreen parent;
    private final FlowLayout master = (FlowLayout) Containers.verticalFlow(Sizing.fill(),Sizing.fill()).gap(4).surface(Surface.BLANK).padding(Insets.of(3));
    private final TextBoxComponent textAreaComponent = Components.textBox(Sizing.fill(),"");
    private final FlowLayout mainContent = (FlowLayout) Containers.horizontalFlow(Sizing.fill(),Sizing.fill()).surface(Surface.DARK_PANEL);

    {
        textAreaComponent.setHint(Component.translatable("sablefinder.ui.search.hint"));
    }

    public SearchScreen(SableFinderScreen parent) {
        this.parent = parent;
        mainContent.clearChildren();
        mainContent.child(constructSearchScreen());
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(5))
                .verticalAlignment(VerticalAlignment.CENTER);

        rootComponent.child(master);

        FlowLayout header = Containers.horizontalFlow(Sizing.fill(),Sizing.fill(10));
        header.surface(Surface.DARK_PANEL);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.padding(Insets.horizontal(6));
        header.child(Components.label(Component.translatable("sablefinder.ui.button.search")));
        header.child(Components.spacer());
        header.gap(4);
        header.child(
                Components.button(
                        Component.translatable("sablefinder.ui.button.search"),button -> {

                        }
                ).sizing(Sizing.content(),Sizing.fill(80))
        );
        header.child(
                Components.button(
                        Component.translatable("sablefinder.ui.button.close"),button -> {
                            onClose();
                        }
                ).sizing(Sizing.content(),Sizing.fill(80))
        );
        master.child(header);

        master.child(mainContent);
    }

    public FlowLayout constructSearchScreen() {
        var container = Containers.verticalFlow(Sizing.fill(),Sizing.fill());



        return container;
    }

    public FlowLayout constructLoadingScreen() {
        var container = Containers.verticalFlow(Sizing.fill(),Sizing.fill());
        container.alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);
        container.child(Components.label(Component.translatable("sablefinder.ui.search.searching")));

        return container;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
