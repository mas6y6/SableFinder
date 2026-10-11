package com.mas6y6.sablefinder.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class SearchScreen extends BaseOwoScreen<FlowLayout> {
    private final SableFinderScreen parent;
    private final FlowLayout box = (FlowLayout) Containers.verticalFlow(Sizing.fill(),Sizing.fill()).surface(Surface.DARK_PANEL);

    public SearchScreen(SableFinderScreen parent) {
        this.parent = parent;
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
                .verticalAlignment(VerticalAlignment.CENTER);

        rootComponent.child(box);


    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
