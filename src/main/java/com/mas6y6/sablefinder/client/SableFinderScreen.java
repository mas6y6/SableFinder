package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.sable.SableContraptionData;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.*;

public class SableFinderScreen extends BaseOwoScreen<FlowLayout> {

    private CompoundTag sableContraptions;
    private final List<ContraptionEntry> contraptions = new ArrayList<>();
    private final @Nullable UUID selectedContraptionID;

    private LabelComponent foundCountLabel;
    private FlowLayout contraptionList;
    private FlowLayout detailsContent;
    private ButtonComponent reloadButton;
    private UUID selectedUuid = null;

    public SableFinderScreen(@Nullable CompoundTag sableContraptions, Optional<UUID> selectedContraptionID) {
        this.sableContraptions = sableContraptions;
        this.selectedContraptionID = selectedContraptionID.orElse(null);
        parseContraptions();
    }

    @Override
    protected void init() {
        super.init();

        if (selectedContraptionID != null) {
            selectContraption(selectedContraptionID);
        }
    }

    private void parseContraptions() {
        contraptions.clear();
        if (sableContraptions == null) {
            return;
        }

        if (sableContraptions.contains("sableContraptionsUUID", Tag.TAG_LIST)) {
            ListTag list = sableContraptions.getList("sableContraptionsUUID", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                UUID uuid = null;
                if (tag.hasUUID("uuid")) {
                    uuid = tag.getUUID("uuid");
                } else if (tag.contains("uuid", Tag.TAG_STRING)) {
                    try {
                        uuid = UUID.fromString(tag.getString("uuid"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (uuid != null) {
                    String name = tag.getString("name");
                    if (name.isEmpty()) {
                        name = "Contraption " + uuid.toString().substring(0, 8);
                    }
                    contraptions.add(new ContraptionEntry(uuid, name));
                }
            }
        } else if (sableContraptions.contains("contraptions", Tag.TAG_LIST)) {
            ListTag list = sableContraptions.getList("contraptions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                if (tag.hasUUID("uuid")) {
                    String name = tag.getString("name");
                    UUID uuid = tag.getUUID("uuid");
                    if (name.isEmpty()) {
                        name = "Contraption " + uuid.toString().substring(0, 8);
                    }
                    contraptions.add(new ContraptionEntry(uuid, name));
                }
            }
        }
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

        FlowLayout mainContainer = Containers.verticalFlow(
                Sizing.fill(92),
                Sizing.fill(92)
        );
        mainContainer.surface(Surface.BLANK);
        mainContainer.padding(Insets.of(2));
        mainContainer.gap(6);

        // Header
        FlowLayout header = Containers.horizontalFlow(
                        Sizing.fill(100),
                        Sizing.fixed(30)
                );
        header.surface(Surface.DARK_PANEL);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.padding(Insets.horizontal(10));

        header.child(
                Components.label(Component.literal("§b§6sableFinder"))
        );
        header.child(
                Components.label(Component.literal(" §8/ §7Contraption Browser"))
        );
        header.child(Components.spacer());
        foundCountLabel = Components.label(Component.literal("§7Found: §f" + contraptions.size()));
        header.child(foundCountLabel);

        // Main content area: Sidebar on left, Details on right
        FlowLayout content = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.expand()
        );
        content.gap(6);

        // Left sidebar
        FlowLayout sidebar = Containers.verticalFlow(
                        Sizing.fixed(180),
                        Sizing.fill(100)
                );
        sidebar.surface(Surface.DARK_PANEL);
        sidebar.padding(Insets.of(6));
        sidebar.gap(4);

        sidebar.child(
                Components.label(Component.literal("§lContraptions"))
                        .margins(Insets.of(2))
        );

        contraptionList = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        contraptionList.gap(4);
        contraptionList.padding(Insets.right(6));

        rebuildContraptionList();

        sidebar.child(
                Containers.verticalScroll(
                                Sizing.fill(100),
                                Sizing.expand(),
                                contraptionList
                        )
                        .scrollbar(ScrollContainer.Scrollbar.vanilla())
                        .scrollbarThiccness(4)
        );

        // Right details panel
        FlowLayout detailsPanel = Containers.verticalFlow(
                Sizing.expand(),
                Sizing.fill(100)
        );
        detailsPanel.surface(Surface.DARK_PANEL);
        detailsPanel.padding(Insets.of(8));

        detailsContent = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        detailsContent.gap(6);
        detailsContent.padding(Insets.right(8));

        detailsPanel.child(
                Containers.verticalScroll(
                                Sizing.fill(100),
                                Sizing.fill(100),
                                detailsContent
                        )
                        .scrollbar(ScrollContainer.Scrollbar.vanilla())
                        .scrollbarThiccness(4)
        );

        content.child(sidebar);
        content.child(detailsPanel);

        // Footer
        FlowLayout footer = Containers.horizontalFlow(
                        Sizing.fill(100),
                        Sizing.fixed(28)
                );
        footer.surface(Surface.DARK_PANEL);
        footer.verticalAlignment(VerticalAlignment.CENTER);
        footer.padding(Insets.horizontal(8));

        footer.child(
                Components.label(Component.literal("§8Sable Contraption Inspector"))
        );

        footer.child(Components.spacer());

        reloadButton = Components.button(
                Component.literal("Reload"),
                button -> reloadContraptions()
        );
        reloadButton.horizontalSizing(Sizing.fixed(60));
        footer.child(reloadButton);

        ButtonComponent closeButton = Components.button(
                Component.literal("Close"),
                button -> onClose()
        );
        closeButton.horizontalSizing(Sizing.fixed(60));
        footer.child(closeButton);

        mainContainer.child(header);
        mainContainer.child(content);
        mainContainer.child(footer);

        rootComponent.child(mainContainer);

        if (!contraptions.isEmpty()) {
            selectContraption(contraptions.get(0).uuid());
        } else {
            showNoSelection();
        }
    }

    private void rebuildContraptionList() {
        contraptionList.clearChildren();

        if (contraptions.isEmpty()) {
            FlowLayout emptyItem = (FlowLayout) Containers.verticalFlow(
                            Sizing.fill(100),
                            Sizing.content()
                    ).padding(Insets.of(6))
                    .horizontalAlignment(HorizontalAlignment.CENTER);

            emptyItem.child(Components.label(Component.literal("§7No contraptions\n§8found in world")));
            contraptionList.child(emptyItem);
            return;
        }

        for (ContraptionEntry entry : contraptions) {
            boolean isSelected = entry.uuid().equals(selectedUuid);
            FlowLayout item = Containers.verticalFlow(
                            Sizing.fill(100),
                            Sizing.content()
                    );
            item.tooltip(
                    Component.literal(entry.name)
                            .append(Component.literal("\n§7UUID: ")
                                    .append(Component.literal(entry.uuid.toString())))
            );
            item.surface(isSelected ? Surface.outline(0xFFced8cd) : Surface.PANEL);
            item.padding(Insets.of(5));
            item.gap(2);

            String displayName = entry.name().length() > 20
                    ? entry.name().substring(0, 18) + "..."
                    : entry.name();

            item.child(
                    Components.label(
                            Component.literal((isSelected ? "§l" : "§f") + displayName)
                    )
            );

            String shortUuid = entry.uuid().toString().substring(0, 8) + "...";
            item.child(
                    Components.label(
                            Component.literal("§8" + shortUuid)
                    )
            );

            item.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    UISounds.playInteractionSound();
                    selectContraption(entry.uuid());
                    return true;
                }
                return false;
            });

            contraptionList.child(item);
        }
    }

    private void selectContraption(UUID uuid) {
        this.selectedUuid = uuid;
        rebuildContraptionList();

        detailsContent.clearChildren();
        detailsContent.child(
                Components.label(Component.literal("§7Loading contraption data from server..."))
        );

        SableContraptionClient.request(uuid).whenComplete((data, error) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    if (Objects.equals(this.selectedUuid, uuid)) {
                        if (error != null || data == null) {
                            showError("Failed to load contraption: " + (error != null ? error.getMessage() : "Unknown error"));
                        } else {
                            displayContraptionDetails(data);
                        }
                    }
                });
            }
        });
    }

    private void reloadContraptions() {
        if (reloadButton != null) {
            reloadButton.active = false;
            reloadButton.setMessage(Component.literal("Reloading..."));
        }

        SableContraptionClient.requestContraptionList().whenComplete((tag, error) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    if (reloadButton != null) {
                        reloadButton.active = true;
                        reloadButton.setMessage(Component.literal("Reload"));
                    }

                    if (error != null || tag == null) {
                        showError("Failed to reload contraptions: " + (error != null ? error.getMessage() : "Unknown error"));
                        return;
                    }

                    this.sableContraptions = tag;
                    parseContraptions();
                    if (foundCountLabel != null) {
                        foundCountLabel.text(Component.literal("§7Found: §f" + contraptions.size()));
                    }
                    rebuildContraptionList();

                    if (selectedUuid != null && contraptions.stream().anyMatch(e -> e.uuid().equals(selectedUuid))) {
                        selectContraption(selectedUuid);
                    } else if (!contraptions.isEmpty()) {
                        selectContraption(contraptions.get(0).uuid());
                    } else {
                        selectedUuid = null;
                        showNoSelection();
                    }
                });
            }
        });
    }

    private void showNoSelection() {
        detailsContent.clearChildren();
        FlowLayout placeholder = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        placeholder.padding(Insets.of(20));
        placeholder.horizontalAlignment(HorizontalAlignment.CENTER);
        placeholder.gap(8);

        placeholder.child(Components.label(Component.literal("§7No contraption selected")));
        placeholder.child(Components.label(Component.literal("§8Select a contraption on the left sidebar to view details.")));
        detailsContent.child(placeholder);
    }

    private void showError(String message) {
        detailsContent.clearChildren();
        FlowLayout errBox = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        errBox.padding(Insets.of(12));
        errBox.gap(6);

        errBox.child(Components.label(Component.literal("§c§lError")));
        errBox.child(Components.label(Component.literal("§c" + message)));

        if (selectedUuid != null) {
            ButtonComponent retryBtn = Components.button(
                    Component.literal("Retry"),
                    b -> selectContraption(selectedUuid)
            );
            retryBtn.horizontalSizing(Sizing.fixed(80));
            errBox.child(retryBtn);
        }

        detailsContent.child(errBox);
    }

    private void displayContraptionDetails(SableContraptionData data) {
        detailsContent.clearChildren();

        // Title and actions card
        FlowLayout headerCard = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        headerCard.surface(Surface.PANEL);
        headerCard.padding(Insets.of(8));
        headerCard.gap(4);

        String name = data.displayName() != null && !data.displayName().isEmpty()
                ? data.displayName()
                : "Contraption (" + data.uuid().toString().substring(0, 8) + ")";

        headerCard.child(Components.label(Component.literal(name)));

        FlowLayout uuidRow = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        uuidRow.verticalAlignment(VerticalAlignment.CENTER);
        uuidRow.gap(6);

        uuidRow.child(Components.label(Component.literal("§7UUID: §f" + data.uuid())));
        ButtonComponent copyUuidBtn = Components.button(
                Component.literal("Copy"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(data.uuid().toString());
                    }
                }
        );
        copyUuidBtn.horizontalSizing(Sizing.fixed(45));
        copyUuidBtn.verticalSizing(Sizing.fixed(16));
        uuidRow.child(copyUuidBtn);
        headerCard.child(uuidRow);

        detailsContent.child(headerCard);

        // General Info Section
        FlowLayout infoCard = createSectionCard("§ePosition & Pose");
        Pose3d pose = data.pose();
        if (pose != null) {
            Vector3d pos = pose.position();
            infoCard.child(createDataRow("Position", String.format(Locale.ROOT, "X: %.2f, Y: %.2f, Z: %.2f", pos.x, pos.y, pos.z)));

            Vector3d scale = pose.scale();
            infoCard.child(createDataRow("Scale", String.format(Locale.ROOT, "%.2f × %.2f × %.2f", scale.x, scale.y, scale.z)));
        }

        BoundingBox3d bounds = data.bounds();
        if (bounds != null) {
            double sizeX = bounds.maxX - bounds.minX;
            double sizeY = bounds.maxY - bounds.minY;
            double sizeZ = bounds.maxZ - bounds.minZ;
            infoCard.child(createDataRow("Bounding Box", String.format(Locale.ROOT, "[%.1f, %.1f, %.1f] to [%.1f, %.1f, %.1f]",
                    bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)));
            infoCard.child(createDataRow("Dimensions", String.format(Locale.ROOT, "%.1f × %.1f × %.1f", sizeX, sizeY, sizeZ)));
        }

        if (data.originChunk() != null) {
            infoCard.child(createDataRow("Origin Chunk", "[" + data.originChunk().x + ", " + data.originChunk().z + "]"));
        }

        detailsContent.child(infoCard);

        // SubLevel & World Properties Section
        FlowLayout sublevelCard = createSectionCard("§eSubLevel Storage");
        sublevelCard.child(createDataRow("Plot Coordinates", "X: " + data.plotX() + ", Z: " + data.plotZ()));
        sublevelCard.child(createDataRow("Log Size", String.valueOf(data.logSize())));
        sublevelCard.child(createDataRow("Loaded Chunks", String.valueOf(data.chunkCount())));
        if (data.biome() != null) {
            sublevelCard.child(createDataRow("Biome", data.biome().location().toString()));
        }
        sublevelCard.child(createDataRow("Data Version", String.valueOf(data.dataVersion())));

        detailsContent.child(sublevelCard);

        // Velocity & Physics (if present)
        if (data.linearVelocity() != null || data.angularVelocity() != null) {
            FlowLayout motionCard = createSectionCard("§eMotion");
            if (data.linearVelocity() != null) {
                Vector3d lv = data.linearVelocity();
                motionCard.child(createDataRow("Linear Velocity", String.format(Locale.ROOT, "%.3f, %.3f, %.3f", lv.x, lv.y, lv.z)));
            }
            if (data.angularVelocity() != null) {
                Vector3d av = data.angularVelocity();
                motionCard.child(createDataRow("Angular Velocity", String.format(Locale.ROOT, "%.3f, %.3f, %.3f", av.x, av.y, av.z)));
            }
            detailsContent.child(motionCard);
        }

        // Dependencies
        if (data.dependencies() != null && !data.dependencies().isEmpty()) {
            FlowLayout depCard = createSectionCard("§eDependencies (" + data.dependencies().size() + ")");
            for (UUID depId : data.dependencies()) {
                depCard.child(createDataRow("Dependency", depId.toString()));
            }
            detailsContent.child(depCard);
        }
    }

    private FlowLayout createSectionCard(String title) {
        FlowLayout card = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        card.surface(Surface.PANEL);
        card.padding(Insets.of(6));
        card.gap(3);

        card.child(Components.label(Component.literal(title)));
        return card;
    }

    private FlowLayout createDataRow(String label, String value) {
        FlowLayout row = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        row.gap(6);

        row.child(Components.label(Component.literal("§7" + label + ":")));
        row.child(Components.label(Component.literal("§f" + value)));
        return row;
    }

    private record ContraptionEntry(UUID uuid, String name) {}
}
