package com.mas6y6.sablefinder.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SableFinderScreen extends Screen {
    private static final int LIST_WIDTH = 220;
    private static final int LIST_HEIGHT = 24;

    private final List<UUID> sableContraptions = new ArrayList<>();
    private final Map<UUID, String> displayNames = new LinkedHashMap<>();

    public SableFinderScreen(CompoundTag compoundTag) {
        super(Component.translatable("gui.sablefinder"));

        ListTag sableContraptionsUUID = (ListTag) compoundTag.get("sableContraptionsUUID");

        if (sableContraptionsUUID != null) {
            sableContraptionsUUID.forEach((tag) -> {
                var contraptionTag = (CompoundTag) tag;

                UUID uuid = contraptionTag.getUUID("uuid");
                sableContraptions.add(uuid);
                displayNames.put(uuid, contraptionTag.getString("name"));
            });
        }
    }

    @Override
    protected void init() {
        super.init();

        ContraptionList contraptionList = new ContraptionList(this.minecraft, LIST_WIDTH, this.height - 32, 16);
        contraptionList.setPosition(0, 16);

        for (UUID uuid : sableContraptions) {
            String name = displayNames.get(uuid);
            contraptionList.addContraption(uuid, name == null || name.isEmpty() ? null : name);
        }

        this.addRenderableWidget(contraptionList);
    }

    private static String shortUuid(UUID uuid) {
        String value = uuid.toString();
        int dash = value.indexOf('-');
        return dash > 0 ? value.substring(0, dash) : value;
    }

    private static class ContraptionList extends ObjectSelectionList<ContraptionList.ContraptionEntry> {
        public ContraptionList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, LIST_HEIGHT);
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        public void addContraption(UUID uuid, String displayName) {
            this.addEntry(new ContraptionEntry(uuid, displayName));
        }

        private static class ContraptionEntry extends ObjectSelectionList.Entry<ContraptionEntry> {
            private final UUID uuid;
            private final String displayName;

            public ContraptionEntry(UUID uuid, String displayName) {
                this.uuid = uuid;
                this.displayName = displayName;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                graphics.fill(left, top, left + width, top + height, hovering ? 0x8040485C : 0x80000000);
                graphics.renderOutline(left, top, width, height, 0xFF8B8B8B);

                var font = Minecraft.getInstance().font;

                if (displayName != null) {
                    String title = font.plainSubstrByWidth(displayName, width - 10);
                    graphics.drawCenteredString(font, title, left + width / 2, top + 3, 0xFFFFFFFF);
                    graphics.drawCenteredString(font, shortUuid(uuid), left + width / 2, top + 13, 0xFFAAAAAA);
                } else {
                    graphics.drawCenteredString(font, shortUuid(uuid), left + width / 2, top + 7, 0xFFFFFFFF);
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(displayName != null ? displayName : shortUuid(uuid));
            }
        }
    }
}