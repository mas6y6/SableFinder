package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.Keybinds;
import com.mas6y6.sablefinder.SableFinderClient;
import com.mas6y6.sablefinder.config.ConfigClient;
import com.mas6y6.sablefinder.networking.DeleteContraptionPacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.simulated.SimulatedCompat;
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
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class SableFinderScreen extends BaseOwoScreen<FlowLayout> {

    private static final int COLLAPSED_SIDEBAR_WIDTH = 10;
    private static final int SCROLLBAR_RIGHT_PADDING = 8;

    private @Nullable CompoundTag sableContraptionsCompoundTag;

    // Survives closing the screen so the user can come back to the contraption they were
    // last looking at. Cleared on disconnect so it never leaks across servers.
    private static @Nullable UUID lastSelectedId = null;

    private final List<ContraptionEntry> contraptions = new ArrayList<>();

    private final List<ContraptionItem> listItems = new ArrayList<>();

    private final @Nullable UUID initiallySelectedId;

    private final boolean refreshOnOpen;

    private LabelComponent foundCountLabel;
    private LabelComponent indexString;
    private FlowLayout contraptionList;
    private FlowLayout top3dContainer;
    private FlowLayout detailsContent;
    private FlowLayout sidebar;
    private FlowLayout sidebarTop;
    private FlowLayout detailsPanel;
    private ScrollContainer<FlowLayout> listScroll;
    private ButtonComponent refreshButton;

    private boolean leftSidebarCollapsed = false;
    private final boolean rightSidebarCollapsed = false;
    private @Nullable UUID selectedUuid = null;
    private int selectedContraptionIndex = 0;
    private boolean isSearchResults = false;
    private boolean reloading = false;

    private boolean initialRefreshStarted = false;

    public SableFinderScreen(@NotNull Optional<UUID> selectedContraptionID) {
        // the refreshOnOpen() function fills the sableContraptionsCompoundTag variable so the opengui packet doesnt send it anymore.

        this.initiallySelectedId = selectedContraptionID.orElse(lastSelectedId);
        this.refreshOnOpen = ConfigClient.refreshOnOpen();
        parseContraptions();
    }

    public static void forgetSession() {
        lastSelectedId = null;
    }

    @Override
    protected void init() {
        super.init();

        if (refreshOnOpen && !initialRefreshStarted) {
            initialRefreshStarted = true;
            resetAndRefreshContraptions();
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    // Gets normal contraptions from the server
    private void resetAndRefreshContraptions() {
        if (!ClientPayloadHandler.allowOperation()) {
            return;
        }

        this.isSearchResults = false;
        setReloadingState(true);

        SableContraptionClient.requestContraptionList().whenComplete((tag, error) ->
                onMainThread(() -> {
                    setReloadingState(false);

                    if (error != null || tag == null) {
                        showRequestError("sablefinder.ui.error.reload_contraptions", error);
                    } else {
                        applyContraptions(tag);
                    }
                }));
    }

    public void applySearchResults(@Nullable CompoundTag tag) {
        this.isSearchResults = true;
        updateRefreshButton();
        applyContraptions(tag);
    }

    private void applyContraptions(@Nullable CompoundTag tag) {
        this.sableContraptionsCompoundTag = tag;
        parseContraptions();
        updateHeader();
        rebuildContraptionList();

        if (selectedUuid != null && containsContraption(selectedUuid)) {
            selectContraption(selectedUuid);
        } else if (!contraptions.isEmpty()) {
            selectContraption(contraptions.getFirst().uuid());
        } else {
            selectedUuid = null;
            updateContraptionSelection();
            showNoSelection();
        }
    }

    private void selectContraption(int index) {
        if (index < 0 || index >= contraptions.size()) {
            return;
        }

        selectedContraptionIndex = index;
        selectContraption(contraptions.get(index).uuid());
    }

    private void selectContraption(UUID uuid) {
        if (!ClientPayloadHandler.isCompatible()) {
            return;
        }

        this.selectedUuid = uuid;
        this.selectedContraptionIndex = indexOfContraption(uuid);
        // Remember it so reopening the browser lands on the same contraption.
        lastSelectedId = uuid;
        updateHeader();
        updateContraptionSelection();

        clearPreview();
        setDetails(centeredLabel("sablefinder.ui.list.loading_detail"));

        SableContraptionClient.request(uuid).whenComplete((data, error) ->
                onMainThread(() -> {
                    // The user may have clicked something else while this was in flight.
                    if (!Objects.equals(selectedUuid, uuid)) {
                        return;
                    }

                    if (error != null || data == null) {
                        showRequestError("sablefinder.ui.error.load_contraption", error);
                    } else {
                        detailsContent.clearChildren();
                        displayContraptionDetails(data);
                    }
                }));
    }

    private void onMainThread(@NotNull Runnable action) {
        if (this.minecraft != null) {
            this.minecraft.execute(action);
        }
    }

    private void showRequestError(@NotNull String translationKey, @Nullable Throwable error) {
        showError(Component.translatable(
                translationKey,
                error != null && error.getMessage() != null
                        ? Component.literal(error.getMessage())
                        : Component.translatable("sablefinder.ui.error.unknown")
        ));
    }

    private void setReloadingState(boolean reloading) {
        this.reloading = reloading;

        if (refreshButton != null) {
            refreshButton.active = !reloading;
            updateRefreshButton();
        }
    }

    private void updateRefreshButton() {
        if (refreshButton == null) {
            return;
        }

        if (reloading) {
            refreshButton.setMessage(Component.translatable("sablefinder.ui.button.refreshing"));
        } else {
            refreshButton.setMessage(Component.translatable(
                    isSearchResults ? "sablefinder.ui.button.reset" : "sablefinder.ui.button.refresh"
            ));
        }

        refreshButton.tooltip(Component.translatable(
                isSearchResults
                        ? "sablefinder.ui.tooltip.clear_filters"
                        : "sablefinder.ui.tooltip.refreshButton"
        ));
    }

    // ============================================================
    // PAYLOAD PARSING
    // ============================================================

    private void parseContraptions() {
        contraptions.clear();

        if (sableContraptionsCompoundTag == null) {
            return;
        }

        ListTag list = null;

        // The v1 payload ("contraptions") names unnamed contraptions differently.
        boolean prefixNameWithContraption = false;

        if (sableContraptionsCompoundTag.contains("sableContraptionsUUID", Tag.TAG_LIST)) {
            list = sableContraptionsCompoundTag.getList("sableContraptionsUUID", Tag.TAG_COMPOUND);
        } else if (sableContraptionsCompoundTag.contains("contraptions", Tag.TAG_LIST)) {
            list = sableContraptionsCompoundTag.getList("contraptions", Tag.TAG_COMPOUND);
            prefixNameWithContraption = true;
        }

        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            UUID uuid = readUuid(tag);

            if (uuid == null) {
                continue;
            }

            String rawName = tag.getString("name");
            boolean nameEmpty = rawName.isEmpty();

            String name = nameEmpty
                    ? (prefixNameWithContraption ? "Contraption " : "")
                    + uuid.toString().substring(0, 8)
                    : rawName;

            contraptions.add(new ContraptionEntry(uuid, name, nameEmpty, tag.getString("dimension")));
        }
    }

    private static @Nullable UUID readUuid(@NotNull CompoundTag tag) {
        if (tag.hasUUID("uuid")) {
            return tag.getUUID("uuid");
        }

        if (tag.contains("uuid", Tag.TAG_STRING)) {
            try {
                return UUID.fromString(tag.getString("uuid"));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    // ============================================================
    // LAYOUT
    // ============================================================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        if (Keybinds.REFRESH_KEY.get().matches(keyCode, scanCode)) {
            resetAndRefreshContraptions();
            return true;
        }

        if (Keybinds.GO_TO_NEXT_CONTRAPTION_KEY.get().matches(keyCode, scanCode)) {
            UISounds.playInteractionSound();
            selectContraption(selectedContraptionIndex + 1);
            return true;
        }

        if (Keybinds.GO_TO_PREVIOUS_CONTRAPTION_KEY.get().matches(keyCode, scanCode)) {
            UISounds.playInteractionSound();
            selectContraption(selectedContraptionIndex - 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void build(@NotNull FlowLayout rootComponent) {
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

        mainContainer.child(buildHeader());

        // Three columns: list | preview | details
        FlowLayout content = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.expand()
        );
        content.gap(4);

        content.child(buildSidebar());
        content.child(buildPreviewPanel());
        content.child(buildDetailsPanel());

        mainContainer.child(content);
        mainContainer.child(buildFooter());

        rootComponent.child(mainContainer);

        if (contraptions.isEmpty()) {
            // With nothing cached the list is about to be replaced by a refresh, so say
            // "loading" instead of "none found" or the empty state flashes by.
            if (refreshOnOpen) {
                showLoading();
            } else {
                showNoSelection();
            }
            return;
        }

        // Prefer whatever is selected, then the id we were asked for, then the first row.
        // initiallySelectedId already falls back to the remembered one when the server
        // didn't name a specific contraption.
        UUID initialUuid = firstPresent(selectedUuid, initiallySelectedId);
        selectContraption(initialUuid != null ? initialUuid : contraptions.getFirst().uuid());
    }

    private @NotNull FlowLayout buildHeader() {
        FlowLayout header = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(20)
        );
        header.surface(Surface.DARK_PANEL);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.padding(Insets.horizontal(6));
        header.gap(3);

        header.child(Components.label(Component.translatable("sablefinder.ui.title")));
        header.child(Components.label(Component.translatable("sablefinder.ui.subtitle")));
        header.child(Components.spacer());

        indexString = Components.label(Component.empty());
        foundCountLabel = Components.label(Component.empty());
        header.child(indexString);
        header.child(foundCountLabel);

        updateHeader();

        return header;
    }

    private @NotNull FlowLayout buildFooter() {
        FlowLayout footer = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(24)
        );
        footer.surface(Surface.DARK_PANEL);
        footer.verticalAlignment(VerticalAlignment.CENTER);
        footer.padding(Insets.horizontal(8));
        footer.gap(4);

        refreshButton = Components.button(
                Component.translatable("sablefinder.ui.button.refresh"),
                button -> resetAndRefreshContraptions()
        );
        updateRefreshButton();
        refreshButton.horizontalSizing(Sizing.fixed(60));
        refreshButton.verticalSizing(Sizing.fixed(17));
        footer.child(refreshButton);

        ButtonComponent closeButton = Components.button(
                Component.translatable("sablefinder.ui.button.close"),
                button -> onClose()
        );
        closeButton.horizontalSizing(Sizing.fixed(40));
        closeButton.verticalSizing(Sizing.fixed(17));
        footer.child(closeButton);

        ButtonComponent guideButton = Components.button(
                Component.translatable("sablefinder.ui.button.guide"),
                button -> {
                    SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SableFinderClient.SYSTEM_TOAST_ID, Component.translatable("sablefinder.ui.coming_soon"), Component.empty());
                }
        );
        guideButton.horizontalSizing(Sizing.fixed(40));
        guideButton.verticalSizing(Sizing.fixed(17));
        footer.child(guideButton);

        return footer;
    }

    private FlowLayout buildSidebar() {
        sidebar = Containers.verticalFlow(
                leftSidebarCollapsed ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH) : Sizing.fill(25),
                Sizing.fill(100)
        );
        sidebar.gap(1);

        sidebarTop = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        sidebarTop.margins(Insets.bottom(3));

        ButtonComponent collapseButton = Components.button(
                Component.translatable("sablefinder.ui.button.collapse"),
                button -> toggleLeftSidebar()
        );
        collapseButton.tooltip(Component.translatable("sablefinder.ui.tooltip.collapse_list"));
        collapseButton.horizontalSizing(Sizing.fixed(12));
        collapseButton.verticalSizing(Sizing.fill(8));

        ButtonComponent searchButton = Components.button(
                Component.translatable("sablefinder.ui.list.search"),
                button -> Minecraft.getInstance().setScreen(new SearchScreen(this))
        );
        searchButton.horizontalSizing(Sizing.expand());
        searchButton.verticalSizing(Sizing.fill(8));

        ButtonComponent clearButton = Components.button(
                Component.translatable("sablefinder.ui.button.clear"),
                button -> resetAndRefreshContraptions()
        );
        clearButton.tooltip(Component.translatable("sablefinder.ui.tooltip.clear_filters"));
        clearButton.horizontalSizing(Sizing.fixed(12));
        clearButton.verticalSizing(Sizing.fill(8));

        sidebarTop.child(collapseButton);
        sidebarTop.child(searchButton);
        sidebarTop.child(clearButton);

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

        return sidebar;
    }

    private void toggleLeftSidebar() {
        leftSidebarCollapsed = !leftSidebarCollapsed;
        updateLeftSidebar();
    }

    private void updateLeftSidebar() {
        if (sidebar == null) return;

        sidebar.clearChildren();
        sidebar.horizontalSizing(
                leftSidebarCollapsed ? Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH) : Sizing.fill(25)
        );

        // The details column absorbs the space the list just gave up.
        if (detailsPanel != null) {
            detailsPanel.horizontalSizing(detailsSizing());
        }

        if (leftSidebarCollapsed) {
            ButtonComponent expandButton = Components.button(
                    Component.translatable("sablefinder.ui.button.expand"),
                    button -> toggleLeftSidebar()
            );
            expandButton.tooltip(Component.translatable("sablefinder.ui.tooltip.expand_list"));
            expandButton.horizontalSizing(Sizing.fixed(10));
            expandButton.verticalSizing(Sizing.fill(100));
            sidebar.child(expandButton);
        } else {
            sidebar.child(sidebarTop);
            sidebar.child(listScroll);
        }
    }

    private @NotNull FlowLayout buildPreviewPanel() {
        FlowLayout previewPanel = Containers.verticalFlow(
                Sizing.expand(),
                Sizing.fill(100)
        );
        previewPanel.surface(Surface.DARK_PANEL);
        previewPanel.padding(Insets.of(4));
        previewPanel.gap(3);

        top3dContainer = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.expand()
        );
        top3dContainer.gap(3);

        previewPanel.child(top3dContainer);

        return previewPanel;
    }

    private FlowLayout buildDetailsPanel() {
        detailsPanel = Containers.verticalFlow(
                detailsSizing(),
                Sizing.fill(100)
        );
        detailsPanel.gap(3);

        detailsContent = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        detailsContent.gap(4);
        detailsContent.padding(Insets.right(SCROLLBAR_RIGHT_PADDING));

        ScrollContainer<FlowLayout> detailsScroll = Containers.verticalScroll(
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

        return detailsPanel;
    }

    private @NotNull Sizing detailsSizing() {
        if (rightSidebarCollapsed) {
            return Sizing.fixed(COLLAPSED_SIDEBAR_WIDTH);
        }

        return leftSidebarCollapsed ? Sizing.fill(53) : Sizing.fill(38);
    }

    private void updateHeader() {
        if (indexString != null) {
            indexString.text(Component.translatable(
                    "sablefinder.ui.index",
                    contraptions.isEmpty() ? 0 : selectedContraptionIndex + 1,
                    contraptions.size()
            ));
        }

        if (foundCountLabel != null) {
            foundCountLabel.text(Component.translatable(
                    "sablefinder.ui.found",
                    contraptions.size()
            ));
        }
    }

    // ============================================================
    // CONTRAPTION LIST
    // ============================================================

    private void rebuildContraptionList() {
        if (contraptionList == null) {
            return;
        }

        contraptionList.clearChildren();
        listItems.clear();

        if (contraptions.isEmpty()) {
            contraptionList.child(centeredLabel("sablefinder.ui.list.empty"));
            return;
        }

        for (ContraptionEntry entry : contraptions) {
            contraptionList.child(buildListRow(entry));
        }
    }

    private @NotNull FlowLayout buildListRow(@NotNull ContraptionEntry entry) {
        FlowLayout item = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        item.padding(Insets.of(3));
        item.gap(1);

        item.tooltip(Component.translatable(
                "sablefinder.ui.tooltip.contraption",
                Component.literal(entry.name()),
                Component.literal(entry.uuid().toString()),
                entry.dimension() != null && !entry.dimension().isEmpty()
                        ? Component.literal(entry.dimension())
                        : Component.translatable("sablefinder.ui.list.unknown_dimension")
        ));

        String displayName = entry.name().length() > 19
                ? entry.name().substring(0, 16) + "..."
                : entry.name();

        LabelComponent nameLabel = Components.label(Component.literal(
                (entry.nameEmpty() ? "§f" : "§6") + displayName
        ));

        item.child(nameLabel);
        item.child(Components.label(Component.literal(
                "§8" + entry.uuid().toString().substring(0, 15) + "..."
        )));

        if (entry.dimension() != null && !entry.dimension().isEmpty()) {
            item.child(Components.label(Component.literal("§8" + entry.dimension())));
        }

        item.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button != 0) {
                return false;
            }

            UISounds.playInteractionSound();
            selectContraption(entry.uuid());
            return true;
        });

        ContraptionItem listItem = new ContraptionItem(
                entry.uuid(), displayName, entry.nameEmpty(), item, nameLabel
        );

        listItems.add(listItem);
        applyItemStyle(listItem);

        return item;
    }

    private void updateContraptionSelection() {
        for (ContraptionItem listItem : listItems) {
            applyItemStyle(listItem);
        }
    }

    private void applyItemStyle(@NotNull ContraptionItem listItem) {
        boolean isSelected = listItem.uuid().equals(selectedUuid);

        listItem.root().surface(
                isSelected
                        ? Surface.outline(0xFF8AB4F8)
                        : Surface.outline(0xFF5F6368).and(Surface.flat(0xFF30343B))
        );

        String nameColor = listItem.nameEmpty() ? "§f" : "§6";

        listItem.nameLabel().text(Component.literal(
                (isSelected ? "§l" : "") + nameColor + listItem.displayName()
        ));
    }

    // ============================================================
    // EMPTY AND ERROR STATES
    // ============================================================

    private void clearPreview() {
        if (top3dContainer != null) {
            top3dContainer.clearChildren();
        }
    }

    private void setDetails(FlowLayout content) {
        if (detailsContent == null) {
            detailsContent.clearChildren();
            return;
        }

        detailsContent.clearChildren();
        detailsContent.child(content);
    }

    private void showLoading() {
        clearPreview();

        if (detailsContent == null) {
            return;
        }

        setDetails(centeredLabel("sablefinder.ui.list.loading_list"));
    }

    private void showNoSelection() {
        clearPreview();

        if (detailsContent == null) {
            return;
        }

        FlowLayout placeholder = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        placeholder.padding(Insets.of(6));
        placeholder.gap(4);
        placeholder.child(OwoUtils.wrappingLabel(Component.translatable("sablefinder.ui.details.none")));
        placeholder.child(OwoUtils.wrappingLabel(Component.translatable("sablefinder.ui.details.none_hint")));

        setDetails(placeholder);
    }

    private void showError(@NotNull Component message) {
        clearPreview();

        if (detailsContent == null) {
            return;
        }

        FlowLayout errorBox = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        errorBox.padding(Insets.of(6));
        errorBox.gap(4);
        errorBox.child(OwoUtils.wrappingLabel(Component.translatable("sablefinder.ui.error.title")));
        errorBox.child(OwoUtils.wrappingLabel(Component.literal("§c").append(message)));

        if (selectedUuid != null) {
            ButtonComponent retryButton = Components.button(
                    Component.translatable("sablefinder.ui.button.retry"),
                    button -> selectContraption(selectedUuid)
            );
            retryButton.horizontalSizing(Sizing.fixed(64));
            retryButton.verticalSizing(Sizing.fixed(18));
            errorBox.child(retryButton);
        }

        setDetails(errorBox);
    }

    private @NotNull FlowLayout centeredLabel(@NotNull String translationKey) {
        FlowLayout wrapper = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        wrapper.padding(Insets.of(4));
        wrapper.horizontalAlignment(HorizontalAlignment.CENTER);

        // fill(100) so the text can actually wrap inside the wrapper - a content-sized label
        // would sit at its full width and overflow instead.
        LabelComponent label = OwoUtils.wrappingLabel(Component.translatable(translationKey));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        wrapper.child(label);

        return wrapper;
    }

    // ============================================================
    // DETAILS VIEW
    // ============================================================

    private void displayContraptionDetails(@NotNull SableContraptionData data) {
        clearPreview();

        Contraption3DViewComponent preview3d = new Contraption3DViewComponent(data);
        buildPreview(preview3d);

        detailsContent.child(buildIdentityCard(data));
        detailsContent.child(buildActionsCard(data));
        detailsContent.child(buildPoseCard(data));
        detailsContent.child(buildStorageCard(data));
        buildMotionCard(data).ifPresent(detailsContent::child);
        buildDependenciesCard(data).ifPresent(detailsContent::child);
        detailsContent.child(buildDebugCard(data));
    }

    private void buildPreview(@NotNull Contraption3DViewComponent preview3d) {
        FlowLayout toolbar = Containers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(18)
        );
        toolbar.verticalAlignment(VerticalAlignment.CENTER);
        toolbar.gap(2);

        ButtonComponent autoRotateButton = Components.button(
                OwoUtils.rotateLabel(preview3d.isAutoRotate()),
                button -> {
                    preview3d.toggleAutoRotate();
                    button.setMessage(OwoUtils.rotateLabel(preview3d.isAutoRotate()));
                }
        );
        autoRotateButton.horizontalSizing(Sizing.fixed(45));
        autoRotateButton.verticalSizing(Sizing.fixed(18));

        preview3d.setOnAutoRotateChanged(
                enabled -> autoRotateButton.setMessage(OwoUtils.rotateLabel(enabled))
        );

        ButtonComponent wireframeButton = Components.button(
                OwoUtils.wireframeLabel(preview3d.isShowBoundingBox()),
                button -> {
                    preview3d.toggleBoundingBox();
                    button.setMessage(OwoUtils.wireframeLabel(preview3d.isShowBoundingBox()));
                }
        );
        wireframeButton.horizontalSizing(Sizing.fixed(45));
        wireframeButton.verticalSizing(Sizing.fixed(18));

        // resetView() clears auto-rotate itself and fires the listener above, so the
        // button label updates without any extra work here.
        ButtonComponent resetButton = Components.button(
                Component.translatable("sablefinder.ui.button.reset"),
                button -> preview3d.resetView()
        );
        resetButton.horizontalSizing(Sizing.fixed(40));
        resetButton.verticalSizing(Sizing.fixed(18));

        toolbar.child(autoRotateButton);
        toolbar.child(wireframeButton);
        toolbar.child(resetButton);

        preview3d.horizontalSizing(Sizing.fill(100));
        preview3d.verticalSizing(Sizing.expand());

        top3dContainer.child(toolbar);
        top3dContainer.child(preview3d);
    }

    private @NotNull FlowLayout buildIdentityCard(@NotNull SableContraptionData data) {
        Component name = data.displayName() != null && !data.displayName().isEmpty()
                ? Component.literal(data.displayName())
                : Component.translatable("sablefinder.ui.untitled", data.uuid().toString().substring(0, 8));

        FlowLayout card = OwoUtils.createSectionCard(Component.literal("§e").append(name));
        card.child(OwoUtils.createCopyButton(name));

        FlowLayout row = Containers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
        );
        row.gap(2);

        row.child(Components.label(Component.translatable("sablefinder.ui.uuid")));
        row.child(Components.label(Component.literal("§f" + data.uuid())));
        row.child(OwoUtils.createCopyButton(Component.literal(data.uuid().toString())));

        row.gap(2);
        row.child(Components.label(Component.translatable("sablefinder.ui.dimension")));

        Component dimensionValue = data.dimension() != null
                ? Component.literal(data.dimension().location().toString())
                : Component.translatable("sablefinder.ui.list.unknown_dimension");

        row.child(Components.label(Component.literal("§f").append(dimensionValue)));

        if (data.dimension() != null) {
            row.child(OwoUtils.createCopyButton(Component.literal(data.dimension().location().toString())));
        }

        row.gap(2);
        row.child(Components.label(Component.translatable("sablefinder.ui.loaded")));
        row.child(Components.label(Component.translatable(
                data.isLoaded() ? "sablefinder.ui.true" : "sablefinder.ui.false"
        )));

        if (SimulatedCompat.isSimulatedInstalled()) {
            row.child(Components.spacer(5));
            row.child(Components.label(Component.translatable("sablefinder.ui.simulated")));
            row.child(Components.label(Component.translatable("sablefinder.ui.islocked")));
            row.child(Components.label(Component.translatable(
                    data.isLocked() ? "sablefinder.ui.true" : "sablefinder.ui.false"
            )));
        }

        card.child(row);

        return card;
    }

    private @NotNull FlowLayout buildActionsCard(@NotNull SableContraptionData data) {
        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.actions")
        );

        card.child(OwoUtils.fullWidthButton(
                Component.translatable("sablefinder.ui.button.teleport"),
                button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(null);

                    Vector3d position = data.pose().position();

                    if (minecraft.getConnection() != null) {
                        minecraft.getConnection().sendCommand(
                                "execute in " + data.dimension().location()
                                        + " run tp @s " + formatCoords(position)
                        );
                    }
                }
        ));

        card.child(OwoUtils.fullWidthButton(
                Component.translatable("sablefinder.ui.button.copy_coords"),
                button -> {
                    button.setMessage(Component.translatable("sablefinder.ui.copied_exclaim"));
                    Minecraft.getInstance().keyboardHandler.setClipboard(
                            formatCoords(data.pose().position())
                    );
                }
        ));

        // Two-step delete: the first click arms it, the second actually deletes.
        AtomicBoolean confirmDelete = new AtomicBoolean(false);

        card.child(OwoUtils.fullWidthButton(
                Component.translatable("sablefinder.ui.button.delete"),
                button -> {
                    if (!confirmDelete.get()) {
                        confirmDelete.set(true);
                        button.setMessage(Component.translatable("sablefinder.ui.delete_confirm"));
                        return;
                    }

                    if (!ClientPayloadHandler.allowOperation()) {
                        confirmDelete.set(false);
                        button.setMessage(Component.translatable("sablefinder.ui.button.delete"));
                        return;
                    }

                    PacketDistributor.sendToServer(new DeleteContraptionPacket(data.uuid()));
                    resetAndRefreshContraptions();
                }
        ));

        if (SimulatedCompat.isSimulatedInstalled()) {
            card.child(Components.spacer(5));
            card.child(OwoUtils.wrappingLabel(Component.translatable("sablefinder.ui.simulated_actions")));

            var lockbutton = OwoUtils.fullWidthButton(
                    Component.translatable(data.isLocked()
                            ? "sablefinder.ui.button.unlock_contraption"
                            : "sablefinder.ui.button.lock_contraption"),
                    button -> {
                        // The server owns the physics staff; ask it to flip the lock and redraw
                        // from the payload it sends back.
                        button.active = false;

                        SableContraptionClient.setLocked(data.uuid(), !data.isLocked())
                                .whenComplete((updated, error) -> onMainThread(() -> {
                                    if (error != null || !Objects.equals(selectedUuid, data.uuid())) {
                                        return;
                                    }

                                    selectContraption(data.uuid()); // reload selected contraption
                                }));
                    }
            );
            card.child(lockbutton);
        }

        return card;
    }

    private @NotNull FlowLayout buildPoseCard(@NotNull SableContraptionData data) {
        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.position_pose")
        );

        Pose3d pose = data.pose();

        if (pose != null) {
            Vector3d position = pose.position();
            Vector3d scale = pose.scale();

            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.position"),
                    Component.literal(String.format(
                            Locale.ROOT, "§c%.2f, §a%.2f, §b%.2f",
                            position.x, position.y, position.z
                    ))
            ));

            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.scale"),
                    Component.literal(String.format(
                            Locale.ROOT, "%.2f × %.2f × %.2f",
                            scale.x, scale.y, scale.z
                    ))
            ));
        }

        BoundingBox3d bounds = data.bounds();

        if (bounds != null) {
            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.bounds_min"),
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f, %.1f, %.1f",
                            bounds.minX, bounds.minY, bounds.minZ
                    ))
            ));

            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.bounds_max"),
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f, %.1f, %.1f",
                            bounds.maxX, bounds.maxY, bounds.maxZ
                    ))
            ));

            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.dimensions"),
                    Component.literal(String.format(
                            Locale.ROOT, "%.1f × %.1f × %.1f",
                            bounds.maxX - bounds.minX,
                            bounds.maxY - bounds.minY,
                            bounds.maxZ - bounds.minZ
                    ))
            ));
        }

        if (data.originChunk() != null) {
            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.origin_chunk"),
                    Component.literal(data.originChunk().x + ", " + data.originChunk().z)
            ));
        }

        return card;
    }

    private @NotNull FlowLayout buildStorageCard(@NotNull SableContraptionData data) {
        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.sublevel_storage")
        );

        card.child(OwoUtils.createDataRow(
                Component.translatable("sablefinder.ui.data.plot_coordinates"),
                Component.translatable("sablefinder.ui.data.plot_coordinates_value", data.plotX(), data.plotZ())
        ));

        card.child(OwoUtils.createDataRow(
                Component.translatable("sablefinder.ui.data.log_size"),
                Component.literal(String.valueOf(data.logSize()))
        ));

        card.child(OwoUtils.createDataRow(
                Component.translatable("sablefinder.ui.data.loaded_chunks"),
                Component.literal(String.valueOf(data.chunkCount()))
        ));

        if (data.biome() != null) {
            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.biome"),
                    Component.literal(data.biome().location().toString())
            ));
        }

        card.child(OwoUtils.createDataRow(
                Component.translatable("sablefinder.ui.data.data_version"),
                Component.literal(String.valueOf(data.dataVersion()))
        ));

        return card;
    }

    private @NotNull Optional<FlowLayout> buildMotionCard(@NotNull SableContraptionData data) {
        if (data.linearVelocity() == null && data.angularVelocity() == null) {
            return Optional.empty();
        }

        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.motion")
        );

        if (data.linearVelocity() != null) {
            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.linear_velocity"),
                    formatVelocity(data.linearVelocity())
            ));
        }

        if (data.angularVelocity() != null) {
            card.child(OwoUtils.createDataRow(
                    Component.translatable("sablefinder.ui.data.angular_velocity"),
                    formatVelocity(data.angularVelocity())
            ));
        }

        return Optional.of(card);
    }

    private @NotNull Optional<FlowLayout> buildDependenciesCard(@NotNull SableContraptionData data) {
        if (data.dependencies() == null || data.dependencies().isEmpty()) {
            return Optional.empty();
        }

        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.dependencies", data.dependencies().size())
        );

        for (UUID dependency : data.dependencies()) {
            FlowLayout dependencyRow = Containers.verticalFlow(
                    Sizing.fill(100),
                    Sizing.content()
            );
            dependencyRow.padding(Insets.of(2));
            dependencyRow.child(Components.label(Component.literal("§f" + dependency)));

            ButtonComponent openButton = Components.button(
                    Component.translatable("sablefinder.ui.button.open"),
                    button -> selectContraption(dependency)
            );
            openButton.horizontalSizing(Sizing.fixed(30));
            openButton.verticalSizing(Sizing.fixed(10));
            dependencyRow.child(openButton);

            card.child(dependencyRow);
        }

        return Optional.of(card);
    }

    private @NotNull FlowLayout buildDebugCard(@NotNull SableContraptionData data) {
        FlowLayout card = OwoUtils.createSectionCard(
                Component.translatable("sablefinder.ui.section.debug_actions")
        );

        card.child(OwoUtils.fullWidthButton(
                Component.translatable("sablefinder.ui.button.copy_raw"),
                button -> {
                    button.setMessage(Component.translatable("sablefinder.ui.copied_exclaim"));
                    Minecraft.getInstance().keyboardHandler.setClipboard(
                            TagFormatter.format(data.fullTag())
                    );
                }
        ));

        return card;
    }

    // ============================================================
    // WIDGET BUILDERS
    // ============================================================


    private static @NotNull String formatCoords(@NotNull Vector3d position) {
        return String.format(
                Locale.ROOT, "%.2f %.2f %.2f",
                position.x, position.y, position.z
        );
    }

    private static @NotNull Component formatVelocity(@NotNull Vector3d velocity) {
        return Component.literal(String.format(
                Locale.ROOT, "%.3f, %.3f, %.3f",
                velocity.x, velocity.y, velocity.z
        ));
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

    private boolean containsContraption(UUID uuid) {
        return contraptions.stream().anyMatch(entry -> entry.uuid().equals(uuid));
    }

    private @Nullable UUID firstPresent(UUID @NotNull ... candidates) {
        for (UUID candidate : candidates) {
            if (candidate != null && containsContraption(candidate)) {
                return candidate;
            }
        }

        return null;
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