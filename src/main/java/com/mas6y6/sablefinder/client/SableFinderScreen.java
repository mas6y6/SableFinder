package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.networking.DeleteContraptionPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.utils.TagFormatter;
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
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SableFinderScreen extends BaseOwoScreen<FlowLayout> {

    private static final int COLLAPSED_SIDEBAR_WIDTH = 10;
    private static final int SCROLLBAR_RIGHT_PADDING = 8;

    private CompoundTag sableContraptions;
    private final List<ContraptionEntry> contraptions = new ArrayList<>();
    private final List<ContraptionItem> listItems = new ArrayList<>();
    private final @Nullable UUID selectedContraptionID;

    private LabelComponent foundCountLabel;
    private LabelComponent indexString;
    private String searchQuery = "";
    private FlowLayout contraptionList;
    private FlowLayout top3dContainer;
    private FlowLayout detailsContent;
    private FlowLayout sidebar;
    private FlowLayout sidebarTop;
    private FlowLayout detailsPanel;
    private ScrollContainer<FlowLayout> listScroll;
    private ButtonComponent reloadButton;
    private boolean leftSidebarCollapsed = false;
    private boolean rightSidebarCollapsed = false;
    private UUID selectedUuid = null;
    private int selectedContraptionIndex = 0;

    public SableFinderScreen(
            @Nullable CompoundTag sableContraptions,
            Optional<UUID> selectedContraptionID
    ) {
        this.sableContraptions = sableContraptions;
        this.selectedContraptionID = selectedContraptionID.orElse(null);
        parseContraptions();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.onClose();
                return true;
            }

            case GLFW.GLFW_KEY_R -> {
                reloadContraptions();
                return true;
            }

            case GLFW.GLFW_KEY_UP -> {
                selectContraption(selectedContraptionIndex - 1);
                return true;
            }

            case GLFW.GLFW_KEY_DOWN -> {
                selectContraption(selectedContraptionIndex + 1);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
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

        if (sableContraptions.contains(
                "sableContraptionsUUID", Tag.TAG_LIST
        )) {
            ListTag list = sableContraptions.getList(
                    "sableContraptionsUUID", Tag.TAG_COMPOUND
            );

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
                    String dimension = tag.getString("dimension");
                    boolean nameEmpty = name.isEmpty();

                    if (name.isEmpty()) {
                        name = uuid.toString().substring(0, 8);
                    }

                    contraptions.add(new ContraptionEntry(uuid, name, nameEmpty, dimension));
                }
            }
        } else if (sableContraptions.contains(
                "contraptions", Tag.TAG_LIST
        )) {
            ListTag list = sableContraptions.getList(
                    "contraptions", Tag.TAG_COMPOUND
            );

            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);

                if (tag.hasUUID("uuid")) {
                    UUID uuid = tag.getUUID("uuid");
                    String name = tag.getString("name");
                    String dimension = tag.getString("dimension");
                    boolean nameEmpty = name.isEmpty();

                    if (name.isEmpty()) {
                        name = "Contraption "
                                + uuid.toString().substring(0, 8);
                    }

                    contraptions.add(new ContraptionEntry(uuid, name, nameEmpty, dimension));
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

        // Main window
        FlowLayout mainContainer = Containers.verticalFlow(
                Sizing.fill(96),
                Sizing.fill(94)
        );
        mainContainer.surface(Surface.BLANK);
        mainContainer.gap(3);

        // Header
        FlowLayout header = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(20)
        );
        header.surface(Surface.DARK_PANEL);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.padding(Insets.horizontal(6));
        header.gap(3);

        header.child(Components.label(
                Component.literal("§6sableFinder")
        ));

        header.child(Components.label(
                Component.literal("§8/ §7Contraption Browser")
        ));

        header.child(Components.spacer());

        indexString = Components.label(

                Component.literal( selectedContraptionIndex + 1 + "/" +contraptions.size())
        );
        header.child(indexString);

        foundCountLabel = Components.label(
                Component.literal("§7Found: §f" + contraptions.size())
        );
        header.child(foundCountLabel);

        // Three-column content area
        FlowLayout content = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.expand()
        );
        content.gap(4);

        // ---------------------------------------------------------
        // LEFT COLUMN: Contraption list
        // ---------------------------------------------------------

        sidebar = Containers.verticalFlow(
                leftSidebarCollapsed
                        ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH)
                        : Sizing.fill(25),
                Sizing.fill(100)
        );
        sidebar.gap(3);

        ButtonComponent searchButton = Components.button(
                Component.literal("§7Search..."),
                button -> Minecraft.getInstance().setScreen(new SearchScreen(this))
        );
        searchButton.horizontalSizing(Sizing.expand());

        sidebarTop = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        sidebarTop.gap(2);
        sidebarTop.margins(Insets.bottom(3));

        ButtonComponent collapseButton = Components.button(
                Component.literal("<"),
                button -> toggleLeftSidebar()
        );
        collapseButton.horizontalSizing(Sizing.fixed(10));
        collapseButton.verticalSizing(Sizing.fill(11));
        sidebarTop.child(collapseButton);
        sidebarTop.child(searchButton);

        contraptionList = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        contraptionList.padding(Insets.right(SCROLLBAR_RIGHT_PADDING));

        rebuildContraptionList();

        listScroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.expand(),
                contraptionList
        );

        listScroll
                .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
                .scrollbarThiccness(4);

        updateLeftSidebar();

        // ---------------------------------------------------------
        // MIDDLE COLUMN: 3D preview
        // ---------------------------------------------------------

        FlowLayout previewPanel = Containers.verticalFlow(
                Sizing.expand(),
                Sizing.fill(100)
        );
        previewPanel.surface(Surface.DARK_PANEL);
        previewPanel.padding(Insets.of(4));
        previewPanel.gap(3);

        // This container holds the selected contraption's preview.
        top3dContainer = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.expand()
        );
        top3dContainer.gap(3);

        previewPanel.child(top3dContainer);

        // ---------------------------------------------------------
        // RIGHT COLUMN: Scrollable details
        // ---------------------------------------------------------

        detailsPanel = Containers.verticalFlow(
                rightSidebarCollapsed
                        ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH)
                        : (leftSidebarCollapsed ? Sizing.fill(53) : Sizing.fill(38)),
                Sizing.fill(100)
        );
        detailsPanel.gap(3);

        detailsContent = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        detailsContent.gap(4);
        detailsContent.padding(Insets.right(SCROLLBAR_RIGHT_PADDING));

        ScrollContainer<FlowLayout> detailsScroll =
                Containers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.expand(),
                        detailsContent
                );

        detailsScroll
                .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
                .scrollbarThiccness(4);

        if (!rightSidebarCollapsed) {
            detailsPanel.child(detailsScroll);
        }

        // Assemble columns in the correct order.
        content.child(sidebar);
        content.child(previewPanel);
        content.child(detailsPanel);

        // ---------------------------------------------------------
        // FOOTER
        // ---------------------------------------------------------

        FlowLayout footer = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(24)
        );
        footer.surface(Surface.DARK_PANEL);
        footer.verticalAlignment(VerticalAlignment.CENTER);
        footer.padding(Insets.horizontal(8));
        footer.gap(4);

        footer.child(Components.label(
                Component.literal("§8Use UP/DOWN arrows to quick browse contraptions")
        ));

        footer.child(Components.spacer());

        reloadButton = Components.button(
                Component.literal("Reload"),
                button -> reloadContraptions()
        );
        reloadButton.horizontalSizing(Sizing.fixed(65));
        reloadButton.verticalSizing(Sizing.fixed(20));
        footer.child(reloadButton);

        ButtonComponent closeButton = Components.button(
                Component.literal("Close"),
                button -> onClose()
        );
        closeButton.horizontalSizing(Sizing.fixed(65));
        closeButton.verticalSizing(Sizing.fixed(20));
        footer.child(closeButton);

        mainContainer.child(header);
        mainContainer.child(content);
        mainContainer.child(footer);

        rootComponent.child(mainContainer);

        if (!contraptions.isEmpty()) {
            UUID initialUuid = selectedUuid != null
                    && contraptions.stream().anyMatch(
                    entry -> entry.uuid().equals(selectedUuid)
            )
                    ? selectedUuid
                    : selectedContraptionID != null
                    && contraptions.stream().anyMatch(
                    entry -> entry.uuid().equals(selectedContraptionID)
            )
                    ? selectedContraptionID
                    : contraptions.getFirst().uuid();

            selectContraption(initialUuid);
        } else {
            showNoSelection();
        }
    }

    private void toggleLeftSidebar() {
        leftSidebarCollapsed = !leftSidebarCollapsed;
        updateLeftSidebar();
    }

    private void updateLeftSidebar() {
        if (sidebar == null) return;

        sidebar.clearChildren();
        sidebar.horizontalSizing(
                leftSidebarCollapsed
                        ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH)
                        : Sizing.fill(25)
        );

        if (detailsPanel != null) {
            detailsPanel.horizontalSizing(
                    rightSidebarCollapsed
                            ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH)
                            : (leftSidebarCollapsed ? Sizing.fill(53) : Sizing.fill(38))
            );
        }

        if (leftSidebarCollapsed) {
            ButtonComponent expandButton = Components.button(
                    Component.literal(">"),
                    button -> toggleLeftSidebar()
            );
            expandButton.horizontalSizing(Sizing.fixed(10));
            expandButton.verticalSizing(Sizing.fill(100));
            sidebar.child(expandButton);
        } else {
            sidebar.child(sidebarTop);
            sidebar.child(listScroll);
        }
    }

    // -------------------------------------------------------------
    // CONTRAPTION LIST
    // -------------------------------------------------------------

    private void rebuildContraptionList() {
        if (contraptionList == null) {
            return;
        }

        contraptionList.clearChildren();
        listItems.clear();

        List<ContraptionEntry> visible = contraptions;

        if (visible.isEmpty()) {
            FlowLayout emptyItem = Containers.verticalFlow(
                    Sizing.fill(100),
                    Sizing.content()
            );
            emptyItem.padding(Insets.of(4));
            emptyItem.horizontalAlignment(HorizontalAlignment.CENTER);

            emptyItem.child(Components.label(
                    Component.literal(contraptions.isEmpty()
                            ? "§7No contraptions found"
                            : "§7No matching contraptions")
            ));

            contraptionList.child(emptyItem);
            return;
        }

        for (ContraptionEntry entry : visible) {
            FlowLayout item = Containers.verticalFlow(
                    Sizing.fill(100),
                    Sizing.content()
            );

            item.padding(Insets.of(3));
            item.gap(1);

            item.tooltip(
                    Component.literal(entry.name())
                            .append(Component.literal(
                                    "\n§7UUID: " + entry.uuid()
                            ))
                            .append(Component.literal(
                                    "\n§7Dimension: "
                                            + (entry.dimension() != null && !entry.dimension().isEmpty()
                                            ? entry.dimension()
                                            : "unknown")
                            ))
            );

            String displayName = entry.name().length() > 19
                    ? entry.name().substring(0, 16) + "..."
                    : entry.name();

            LabelComponent nameLabel = Components.label(
                    Component.literal((entry.nameEmpty() ? "§f" : "§6") + displayName)
            );

            item.child(nameLabel);

            item.child(Components.label(
                    Component.literal(
                            "§8" + entry.uuid().toString().substring(0, 15) + "..."
                    )
            ));

            if (entry.dimension() != null && !entry.dimension().isEmpty()) {
                item.child(Components.label(
                        Component.literal("§8" + entry.dimension())
                ));
            }

            item.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    UISounds.playInteractionSound();
                    selectContraption(entry.uuid());
                    return true;
                }

                return false;
            });

            ContraptionItem listItem = new ContraptionItem(
                    entry.uuid(), displayName, entry.nameEmpty(), item, nameLabel
            );

            listItems.add(listItem);
            applyItemStyle(listItem);

            contraptionList.child(item);
        }
    }

    private void updateContraptionSelection() {
        for (ContraptionItem listItem : listItems) {
            applyItemStyle(listItem);
        }
    }

    private void applyItemStyle(ContraptionItem listItem) {
        boolean isSelected = listItem.uuid().equals(selectedUuid);

        listItem.root().surface(
                isSelected
                        ? Surface.outline(0xFF8AB4F8)
                        : Surface.outline(0xFF5F6368)
                        .and(Surface.flat(0xFF30343B))
        );

        String nameColor = listItem.nameEmpty() ? "§f" : "§6";

        listItem.nameLabel().text(Component.literal(
                (isSelected ? "§l" : "") + nameColor + listItem.displayName()
        ));
    }

    // -------------------------------------------------------------
    // SELECTION AND NETWORK REQUESTS
    // -------------------------------------------------------------

    private void selectContraption(int index) {
        if (index < 0 || index >= contraptions.size()) {
            return;
        }

        selectedContraptionIndex = index;

        selectContraption(contraptions.get(index).uuid());
    }

    private int indexOfContraption(@Nullable UUID uuid) {
        if (uuid == null) {
            return -1;
        }

        for (int i = 0; i < contraptions.size(); i++) {
            if (contraptions.get(i).uuid().equals(uuid)) {
                return i;
            }
        }

        return -1;
    }

    private void selectContraption(UUID uuid) {
        selectedUuid = uuid;
        selectedContraptionIndex = indexOfContraption(uuid);

        indexString.text(Component.literal( selectedContraptionIndex + 1 + "/" +contraptions.size()));

        updateContraptionSelection();

        if (top3dContainer != null) {
            top3dContainer.clearChildren();
        }

        if (detailsContent != null) {
            detailsContent.clearChildren();
            detailsContent.child(Components.label(
                    Component.literal("§7Loading contraption data...")
            ));
        }

        SableContraptionClient.request(uuid).whenComplete((data, error) -> {
            if (this.minecraft == null) {
                return;
            }

            this.minecraft.execute(() -> {
                if (!Objects.equals(selectedUuid, uuid)) {
                    return;
                }

                if (error != null || data == null) {
                    showError(
                            "Failed to load contraption: "
                                    + (error != null
                                    ? error.getMessage()
                                    : "Unknown error")
                    );
                } else {
                    displayContraptionDetails(data);
                }
            });
        });
    }

    private void reloadContraptions() {
        if (reloadButton != null) {
            reloadButton.active = false;
            reloadButton.setMessage(Component.literal("Reloading..."));
        }

        SableContraptionClient.requestContraptionList()
                .whenComplete((tag, error) -> {
                    if (this.minecraft == null) {
                        return;
                    }

                    this.minecraft.execute(() -> {
                        if (reloadButton != null) {
                            reloadButton.active = true;
                            reloadButton.setMessage(
                                    Component.literal("Reload")
                            );
                        }

                        if (error != null || tag == null) {
                            showError(
                                    "Failed to reload contraptions: "
                                            + (error != null
                                            ? error.getMessage()
                                            : "Unknown error")
                            );
                            return;
                        }

                        sableContraptions = tag;
                        parseContraptions();

                        if (foundCountLabel != null) {
                            foundCountLabel.text(Component.literal(
                                    "§7Found: §f" + contraptions.size()
                            ));
                        }

                        rebuildContraptionList();

                        if (selectedUuid != null
                                && contraptions.stream().anyMatch(
                                entry -> entry.uuid().equals(selectedUuid)
                        )) {
                            selectContraption(selectedUuid);
                        } else if (!contraptions.isEmpty()) {
                            selectContraption(contraptions.getFirst().uuid());
                        } else {
                            selectedUuid = null;
                            updateContraptionSelection();
                            showNoSelection();
                        }
                    });
                });
    }

    // -------------------------------------------------------------
    // EMPTY AND ERROR STATES
    // -------------------------------------------------------------

    private void showNoSelection() {
        if (top3dContainer != null) {
            top3dContainer.clearChildren();
        }

        if (detailsContent == null) {
            return;
        }

        detailsContent.clearChildren();

        FlowLayout placeholder = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        placeholder.padding(Insets.of(6));
        placeholder.gap(4);

        placeholder.child(Components.label(
                Component.literal("§7No contraption selected")
        ));
        placeholder.child(Components.label(
                Component.literal("§8Select a contraption from the left.")
        ));

        detailsContent.child(placeholder);
    }

    private void showError(String message) {
        if (top3dContainer != null) {
            top3dContainer.clearChildren();
        }

        if (detailsContent == null) {
            return;
        }

        detailsContent.clearChildren();

        FlowLayout errorBox = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        errorBox.padding(Insets.of(6));
        errorBox.gap(4);

        errorBox.child(Components.label(
                Component.literal("§c§lError")
        ));
        errorBox.child(Components.label(
                Component.literal("§c" + message)
        ));

        if (selectedUuid != null) {
            ButtonComponent retryButton = Components.button(
                    Component.literal("Retry"),
                    button -> selectContraption(selectedUuid)
            );

            retryButton.horizontalSizing(Sizing.fixed(64));
            retryButton.verticalSizing(Sizing.fixed(18));
            errorBox.child(retryButton);
        }

        detailsContent.child(errorBox);
    }

    // -------------------------------------------------------------
    // 3D PREVIEW
    // -------------------------------------------------------------

    private void displayContraptionDetails(SableContraptionData data) {
        if (top3dContainer != null) {
            top3dContainer.clearChildren();
        }

        detailsContent.clearChildren();

        Contraption3DViewComponent preview3d =
                new Contraption3DViewComponent(data);
        preview3d.horizontalSizing(Sizing.fill(100));
        preview3d.verticalSizing(Sizing.expand());

        // Preview toolbar
        FlowLayout toolbar = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(18)
        );
        toolbar.verticalAlignment(VerticalAlignment.CENTER);
        toolbar.gap(2);

        ButtonComponent autoRotateButton = Components.button(
                Component.literal(
                        "Rot: " + (preview3d.isAutoRotate() ? "ON" : "OFF")
                ),
                button -> {
                    preview3d.toggleAutoRotate();
                    button.setMessage(Component.literal(
                            "Rot: "
                                    + (preview3d.isAutoRotate() ? "ON" : "OFF")
                    ));
                }
        );
        autoRotateButton.horizontalSizing(Sizing.fixed(45));
        autoRotateButton.verticalSizing(Sizing.fixed(18));

        preview3d.setOnAutoRotateChanged(enabled ->
                autoRotateButton.setMessage(Component.literal(
                        "Rot: " + (enabled ? "ON" : "OFF")
                ))
        );

        ButtonComponent wireframeButton = Components.button(
                Component.literal(
                        "WF: "
                                + (preview3d.isShowBoundingBox() ? "ON" : "OFF")
                ),
                button -> {
                    preview3d.toggleBoundingBox();
                    button.setMessage(Component.literal(
                            "WF: "
                                    + (preview3d.isShowBoundingBox() ? "ON" : "OFF")
                    ));
                }
        );
        wireframeButton.horizontalSizing(Sizing.fixed(45));
        wireframeButton.verticalSizing(Sizing.fixed(18));

        ButtonComponent resetButton = Components.button(
                Component.literal("Reset"),
                button -> {
                    preview3d.resetView();
                    autoRotateButton.setMessage(
                            Component.literal("Rot: OFF")
                    );
                }
        );
        resetButton.horizontalSizing(Sizing.fixed(40));
        resetButton.verticalSizing(Sizing.fixed(18));

        toolbar.child(autoRotateButton);
        toolbar.child(wireframeButton);
        toolbar.child(resetButton);

        if (top3dContainer != null) {
            top3dContainer.child(toolbar);

            // Let the preview occupy the remaining middle-column space.
            preview3d.horizontalSizing(Sizing.fill(100));
            preview3d.verticalSizing(Sizing.expand());

            top3dContainer.child(preview3d);
        }


        String name = data.displayName() != null
                && !data.displayName().isEmpty()
                ? data.displayName()
                : "Untitled Contraption (" + data.uuid().toString().substring(0, 8) + ")";

        FlowLayout headerCard = createSectionCard("§e" + name);

        headerCard.child(createCopyButton(name));

        FlowLayout uuidRow = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        uuidRow.gap(2);

        uuidRow.child(Components.label(
                Component.literal("§7UUID:")
        ));

        uuidRow.child(
                Components.label(
                    Component.literal("§f" + data.uuid())
                )
        );

        uuidRow.child(createCopyButton(data.uuid().toString()));

        uuidRow.gap(2);

        uuidRow.child(Components.label(
                Component.literal("§7Dimension:")
        ));

        uuidRow.child(
                Components.label(
                        Component.literal("§f" + (data.dimension() != null
                                ? data.dimension().location().toString()
                                : "unknown"))
                )
        );

        uuidRow.child(createCopyButton(data.dimension().location().toString()));

        uuidRow.gap(2);

        uuidRow.child(Components.label(
                Component.literal("§7Loaded:")
        ));

        uuidRow.child(
                Components.label(
                        Component.literal(data.isLoaded() ? "§aTrue" : "§cFalse")
                )
        );

        headerCard.child(uuidRow);

        detailsContent.child(headerCard);

        // Actions

        FlowLayout actionsCard = createSectionCard("§eActions");

        ButtonComponent teleport = Components.button(
                Component.literal("Teleport to"),
                button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(null);

                    Vector3d position = data.pose().position();

                    if (minecraft.getConnection() != null) {
                        minecraft.getConnection().sendCommand("execute in " + data.dimension().location() + " run tp @s " + String.format(
                                Locale.ROOT, "%.2f %.2f %.2f",
                                position.x, position.y, position.z
                        ));
                    }
                }
        );
        teleport.horizontalSizing(Sizing.fill(100));
        teleport.verticalSizing(Sizing.fixed(20));
        actionsCard.child(teleport);

        ButtonComponent copyCoords = Components.button(
                Component.literal("Copy Coords"),
                button -> {
                    button.setMessage(Component.literal("Copied!"));

                    Vector3d position = data.pose().position();

                    Minecraft.getInstance().keyboardHandler
                            .setClipboard(
                                    String.format(
                                            Locale.ROOT, "%.2f %.2f %.2f",
                                            position.x, position.y, position.z
                                    )
                            );
                }
        );
        copyCoords.horizontalSizing(Sizing.fill(100));
        copyCoords.verticalSizing(Sizing.fixed(20));

        actionsCard.child(copyCoords);

        AtomicBoolean confirmDelete = new AtomicBoolean(false);

        ButtonComponent deleteContraption = Components.button(
                Component.literal("§cDelete"),
                button -> {
                    if (confirmDelete.get()) {
                        PacketDistributor.sendToServer(new DeleteContraptionPacket(data.uuid()));
                        reloadContraptions();
                    } else {
                        confirmDelete.set(true);
                        button.setMessage(Component.literal("§cYou Sure?"));
                    }
                }
        );
        deleteContraption.horizontalSizing(Sizing.fill(100));
        deleteContraption.verticalSizing(Sizing.fixed(20));
        actionsCard.child(deleteContraption);

        detailsContent.child(actionsCard);

        // Position and pose
        FlowLayout infoCard = createSectionCard("§ePosition & Pose");

        Pose3d pose = data.pose();

        if (pose != null) {
            Vector3d position = pose.position();
            Vector3d scale = pose.scale();

            infoCard.child(createDataRow(
                    "Position",
                    Component.literal(String.format(
                            Locale.ROOT, "§c%.2f, §a%.2f, §b%.2f",
                            position.x, position.y, position.z
                    ))
            ));

            infoCard.child(createDataRow(
                    "Scale",
                    Component.literal(String.format(
                            Locale.ROOT, "%.2f × %.2f × %.2f",
                            scale.x, scale.y, scale.z
                    ))
            ));
        }

        BoundingBox3d bounds = data.bounds();

        if (bounds != null) {
            double sizeX = bounds.maxX - bounds.minX;
            double sizeY = bounds.maxY - bounds.minY;
            double sizeZ = bounds.maxZ - bounds.minZ;

            infoCard.child(createDataRow(
                    "Bounds min",
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f, %.1f, %.1f",
                            bounds.minX, bounds.minY, bounds.minZ
                    ))
            ));

            infoCard.child(createDataRow(
                    "Bounds max",
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f, %.1f, %.1f",
                            bounds.maxX, bounds.maxY, bounds.maxZ
                    ))
            ));

            infoCard.child(createDataRow(
                    "Dimensions",
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f × %.1f × %.1f",
                            sizeX, sizeY, sizeZ
                    ))
            ));
        }

        if (data.originChunk() != null) {
            infoCard.child(createDataRow(
                    "Origin Chunk",
                    Component.literal(data.originChunk().x + ", " + data.originChunk().z)
            ));
        }

        detailsContent.child(infoCard);

        // SubLevel storage and world properties
        FlowLayout storageCard = createSectionCard("§eSubLevel Storage");

        storageCard.child(createDataRow(
                "Plot Coordinates",
                Component.literal("X: " + data.plotX() + ", Z: " + data.plotZ())
        ));
        storageCard.child(createDataRow(
                "Log Size",
                Component.literal(String.valueOf(data.logSize()))
        ));
        storageCard.child(createDataRow(
                "Loaded Chunks",
                Component.literal(String.valueOf(data.chunkCount()))
        ));

        if (data.biome() != null) {
            storageCard.child(createDataRow(
                    "Biome",
                    Component.literal(data.biome().location().toString())
            ));
        }

        storageCard.child(createDataRow(
                "Data Version",
                Component.literal(String.valueOf(data.dataVersion()))
        ));

        detailsContent.child(storageCard);

        // Motion
        if (data.linearVelocity() != null
                || data.angularVelocity() != null) {
            FlowLayout motionCard = createSectionCard("§eMotion");

            if (data.linearVelocity() != null) {
                Vector3d velocity = data.linearVelocity();

                motionCard.child(createDataRow(
                        "Linear Velocity",
                        Component.literal(String.format(
                                Locale.ROOT, "%.3f, %.3f, %.3f",
                                velocity.x, velocity.y, velocity.z
                        ))
                ));
            }

            if (data.angularVelocity() != null) {
                Vector3d velocity = data.angularVelocity();

                motionCard.child(createDataRow(
                        "Angular Velocity",
                        Component.literal(String.format(
                                Locale.ROOT, "%.3f, %.3f, %.3f",
                                velocity.x, velocity.y, velocity.z
                        ))
                ));
            }

            detailsContent.child(motionCard);
        }

        // Dependencies
        if (data.dependencies() != null
                && !data.dependencies().isEmpty()) {
            FlowLayout dependenciesCard = createSectionCard(
                    "§eDependencies (" + data.dependencies().size() + ")"
            );

            for (UUID dependency : data.dependencies()) {
                FlowLayout dependencyRow = Containers.verticalFlow(
                        Sizing.fill(100),
                        Sizing.content()
                );
                dependencyRow.padding(Insets.of(2));
                dependencyRow.child(Components.label(
                        Component.literal("§f" + dependency)
                ));
                ButtonComponent button = Components.button(
                        Component.literal("Open"),
                        button1 -> selectContraption(dependency)
                );
                button.horizontalSizing(Sizing.fixed(30));
                button.verticalSizing(Sizing.fixed(10));
                dependencyRow.child(button);

                dependenciesCard.child(dependencyRow);
            }

            detailsContent.child(dependenciesCard);
        }

        // Debugging

        FlowLayout debugActionsCard = createSectionCard("§eDebugging Actions");

        ButtonComponent copyRawSableData = Components.button(
                Component.literal("Copy Raw Sable Data"),
                button -> {
                    button.setMessage(Component.literal("Copied!"));

                    Minecraft.getInstance().keyboardHandler
                            .setClipboard(TagFormatter.format(data.fullTag()));
                }
        );
        copyRawSableData.horizontalSizing(Sizing.fill(100));
        copyRawSableData.verticalSizing(Sizing.fixed(20));
        debugActionsCard.child(copyRawSableData);

        detailsContent.child(debugActionsCard);
    }

    // -------------------------------------------------------------
    // DETAIL COMPONENT HELPERS
    // -------------------------------------------------------------

    private FlowLayout createSectionCard(String title) {
        FlowLayout card = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        card.surface(Surface.DARK_PANEL);
        card.padding(Insets.of(4));
        card.gap(2);

        card.child(Components.label(Component.literal(title)));

        return card;
    }

    private FlowLayout createDataRow(String label, Component ...components) {
        FlowLayout row = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        row.gap(1);

        row.child(Components.label(
                Component.literal("§7" + label)
        ));

        Arrays.stream(components).forEach(component ->
                row.child(Components.label(
                        component
                ))
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
                    copyText.toString().replaceAll("§.", "")
            ));
        }

        return row;
    }

    private ButtonComponent createCopyButton(String text) {
        ButtonComponent copy = (ButtonComponent) Components.button(
                Component.literal("Copy"),
                button -> {
                    button.setMessage(Component.literal("Copied"));

                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(text);
                    }
                }
        ).tooltip(Component.literal(text));

        copy.horizontalSizing(Sizing.fixed(30));
        copy.verticalSizing(Sizing.fixed(10));

        return copy;
    }

    public record ContraptionEntry(UUID uuid, String name, boolean nameEmpty, String dimension) {}

    public record ContraptionItem(
            UUID uuid,
            String displayName,
            boolean nameEmpty,
            FlowLayout root,
            LabelComponent nameLabel
    ) {}
}
