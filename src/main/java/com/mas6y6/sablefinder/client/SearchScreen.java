package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.search.BlockFilter;
import com.mas6y6.sablefinder.search.BlockSearchMethod;
import com.mas6y6.sablefinder.search.SearchEngine;
import com.mas6y6.sablefinder.search.SearchFilters;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BlockComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class SearchScreen extends BaseOwoScreen<FlowLayout> {

    private static final int MAX_PICKER_RESULTS = 80;

    // Kept in memory only; this survives reopening the screen, not restarting Minecraft.
    private static SearchConfig savedConfig;

    private record SearchConfig(
            boolean useName,
            String name,
            boolean useUuid,
            String uuid,
            boolean useBlockSize,
            BlockSearchMethod blockSearchMethod,
            String blockSizeMin,
            String blockSizeMax,
            boolean useBlocks,
            LinkedHashMap<ResourceLocation, BlockFilter> selectedBlocks
    ) {}

    private final SableFinderScreen parent;

    // --- textual filters ---------------------------------------------------

    private final CheckboxComponent useNameCheckbox =
            Components.checkbox(Component.translatable("sablefinder.ui.search.name"));
    private final TextBoxComponent nameBox = Components.textBox(Sizing.fill());

    private final CheckboxComponent useUuidCheckbox =
            Components.checkbox(Component.translatable("sablefinder.ui.search.uuid"));
    private final TextBoxComponent uuidBox = Components.textBox(Sizing.fill());

    private BlockSearchMethod blockSearchMethod = BlockSearchMethod.AT_LEAST_OR_EQUAL;
    private final CheckboxComponent useBlockSizeCheckbox =
            Components.checkbox(Component.translatable("sablefinder.ui.search.block_size"));
    private final ButtonComponent blockSizeMethodButton =
            Components.button(Component.empty(), this::cycleBlockSearchMethod);
    private final TextBoxComponent blockSizeBox = Components.textBox(Sizing.fill());
    private final TextBoxComponent blockSizeMaxBox = Components.textBox(Sizing.fill());

    // Repopulated whenever the method changes, since BETWEEN is the only one with a second bound.
    private @Nullable FlowLayout blockSizeEntries;

    // --- block filters -----------------------------------------------------

    private final LinkedHashMap<ResourceLocation, BlockFilter> selectedBlocks = new LinkedHashMap<>();

    private final CheckboxComponent useBlocksCheckbox =
            Components.checkbox(Component.translatable("sablefinder.ui.search.blocks"));
    private final ButtonComponent addBlockButton =
            Components.button(Component.literal("+"), button -> openBlockPicker());

    private FlowLayout selectedBlocksList;

    private final LinkedHashSet<ResourceLocation> pickerSelection = new LinkedHashSet<>();

    private @Nullable TextBoxComponent pickerSearchBox;
    private @Nullable ButtonComponent pickerConfirmButton;
    private @Nullable FlowLayout pickerResults;

    private FlowLayout contentHost;
    private FlowLayout mainContent;

    private final ButtonComponent searchButton =
            Components.button(Component.translatable("sablefinder.ui.button.search"), button -> onSearchButtonPressed());
    private final ButtonComponent resetButton =
            Components.button(Component.translatable("sablefinder.ui.button.reset"), button -> resetFilters());
    private final ButtonComponent closeButton =
            Components.button(Component.translatable("sablefinder.ui.button.close"), button -> onClose());

    private LabelComponent statusLabel;
    private boolean searching;
    private @Nullable SearchRequest activeSearch;

    public SearchScreen(SableFinderScreen parent) {
        this.parent = parent;
        configureComponents();
        loadConfig();
    }

    private void configureComponents() {
        nameBox.setHint(Component.translatable("sablefinder.ui.search.name.hint"));
        nameBox.setMaxLength(256);

        uuidBox.setHint(Component.translatable("sablefinder.ui.search.uuid.hint"));
        uuidBox.setMaxLength(256);

        blockSizeMethodButton.tooltip(Component.translatable("sablefinder.ui.search.block_size.tooltip"));
        blockSizeMethodButton.setMessage(blockSizeMethodLabel());
        blockSizeBox.text("0");
        blockSizeMaxBox.text("0");

        addBlockButton.tooltip(Component.translatable("sablefinder.ui.search.blocks.add"));
        addBlockButton.horizontalSizing(Sizing.fixed(18));
        addBlockButton.verticalSizing(Sizing.fixed(18));

        useNameCheckbox.onChanged(checked -> onFilterToggled());
        nameBox.onChanged().subscribe(value -> onFilterToggled());
        useUuidCheckbox.onChanged(checked -> onFilterToggled());
        uuidBox.onChanged().subscribe(value -> onFilterToggled());
        useBlockSizeCheckbox.onChanged(checked -> onFilterToggled());
        blockSizeBox.onChanged().subscribe(value -> onFilterToggled());
        blockSizeMaxBox.onChanged().subscribe(value -> onFilterToggled());
        useBlocksCheckbox.onChanged(checked -> onFilterToggled());
    }

    private void loadConfig() {
        if (savedConfig == null) return;

        useNameCheckbox.checked(savedConfig.useName());
        nameBox.text(savedConfig.name());
        useUuidCheckbox.checked(savedConfig.useUuid());
        uuidBox.text(savedConfig.uuid());
        useBlockSizeCheckbox.checked(savedConfig.useBlockSize());
        setBlockSearchMethod(savedConfig.blockSearchMethod());
        blockSizeBox.text(savedConfig.blockSizeMin());
        blockSizeMaxBox.text(savedConfig.blockSizeMax());
        useBlocksCheckbox.checked(savedConfig.useBlocks());
        selectedBlocks.clear();
        selectedBlocks.putAll(savedConfig.selectedBlocks());
    }

    private void saveConfig() {
        savedConfig = new SearchConfig(
                useNameCheckbox.selected(),
                nameBox.getValue(),
                useUuidCheckbox.selected(),
                uuidBox.getValue(),
                useBlockSizeCheckbox.selected(),
                blockSearchMethod,
                blockSizeBox.getValue(),
                blockSizeMaxBox.getValue(),
                useBlocksCheckbox.selected(),
                new LinkedHashMap<>(selectedBlocks)
        );
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(@NotNull FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);

        final FlowLayout master = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        master.padding(Insets.of(5));
        master.gap(4);

        master.child(buildHeader());

        this.mainContent = buildContent();

        this.contentHost = Containers.verticalFlow(Sizing.fill(100), Sizing.expand());
        this.contentHost.child(this.mainContent);

        master.child(this.contentHost);
        rootComponent.child(master);

        onFilterToggled();
    }

    private @NotNull FlowLayout buildHeader() {
        final FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
        header.surface(Surface.DARK_PANEL);
        header.padding(Insets.horizontal(6));
        header.gap(4);
        header.verticalAlignment(VerticalAlignment.CENTER);

        header.child(Components.label(Component.translatable("sablefinder.ui.search.title")));
        header.child(horizontalSpacer());

        searchButton.horizontalSizing(Sizing.fixed(70));
        searchButton.verticalSizing(Sizing.fixed(16));
        header.child(searchButton);

        resetButton.horizontalSizing(Sizing.fixed(52));
        resetButton.verticalSizing(Sizing.fixed(16));
        header.child(resetButton);

        closeButton.horizontalSizing(Sizing.fixed(52));
        closeButton.verticalSizing(Sizing.fixed(16));
        header.child(closeButton);

        return header;
    }

    private @NotNull FlowLayout buildContent() {
        final FlowLayout content = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        content.gap(4);

        content.child(buildFilterPanel());
        content.child(buildBlockPanel());

        return content;
    }

    private @NotNull FlowLayout buildFilterPanel() {
        final FlowLayout panel = Containers.verticalFlow(Sizing.fill(44), Sizing.fill(100));
        panel.surface(Surface.DARK_PANEL);
        panel.padding(Insets.of(5));
        panel.gap(4);

        final FlowLayout filters = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        filters.gap(5);
        filters.padding(Insets.right(4));

        final FlowLayout nameCard = card();
        nameCard.child(useNameCheckbox);
        nameCard.child(nameBox);
        filters.child(nameCard);

        final FlowLayout uuidCard = card();
        uuidCard.child(useUuidCheckbox);
        uuidCard.child(uuidBox);
        filters.child(uuidCard);

        final FlowLayout sizeCard = card();
        sizeCard.child(useBlockSizeCheckbox);

        blockSizeMethodButton.horizontalSizing(Sizing.fill(100));
        blockSizeMethodButton.verticalSizing(Sizing.fixed(18));
        sizeCard.child(blockSizeMethodButton);

        blockSizeEntries = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        blockSizeEntries.gap(2);
        sizeCard.child(blockSizeEntries);
        rebuildBlockSizeEntries();
        filters.child(sizeCard);

        final ScrollContainer<FlowLayout> scroll =
                Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), filters);
        panel.child(scroll);

        statusLabel = Components.label(Component.translatable("sablefinder.ui.search.results.hint"));
        panel.child(statusLabel);

        return panel;
    }

    private @NotNull FlowLayout buildBlockPanel() {
        final FlowLayout panel = Containers.verticalFlow(Sizing.fill(53), Sizing.fill(100));
        panel.surface(Surface.DARK_PANEL);
        panel.padding(Insets.of(5));
        panel.gap(4);

        final FlowLayout header = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.gap(4);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.child(useBlocksCheckbox);
        header.child(horizontalSpacer());
        header.child(addBlockButton);
        panel.child(header);

        panel.child(Components.label(Component.translatable("sablefinder.ui.search.blocks.selected")));

        selectedBlocksList = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        selectedBlocksList.gap(2);
        selectedBlocksList.padding(Insets.right(4));

        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), selectedBlocksList));

        rebuildSelectedBlocks();

        return panel;
    }

    // --- block picking -----------------------------------------------------

    private void openBlockPicker() {
        if (this.contentHost == null) {
            return;
        }

        this.contentHost.clearChildren();
        this.contentHost.child(buildBlockPicker());
    }

    private @NotNull FlowLayout buildBlockPicker() {
        final FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.surface(Surface.DARK_PANEL);
        panel.padding(Insets.of(6));
        panel.gap(4);

        final FlowLayout titleRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        titleRow.gap(4);
        titleRow.verticalAlignment(VerticalAlignment.CENTER);
        titleRow.child(Components.label(Component.translatable("sablefinder.ui.search.block_picker.title")));
        titleRow.child(horizontalSpacer());

        final ButtonComponent cancel = Components.button(
                Component.translatable("sablefinder.ui.search.block_picker.cancel"),
                button -> closeBlockPicker()
        );
        cancel.horizontalSizing(Sizing.fixed(52));
        cancel.verticalSizing(Sizing.fixed(18));
        titleRow.child(cancel);

        pickerConfirmButton = Components.button(
                Component.translatable("sablefinder.ui.search.block_picker.confirm"),
                button -> confirmPickedBlocks()
        );
        pickerConfirmButton.horizontalSizing(Sizing.fixed(52));
        pickerConfirmButton.verticalSizing(Sizing.fixed(18));
        titleRow.child(pickerConfirmButton);
        panel.child(titleRow);

        pickerSearchBox = Components.textBox(Sizing.fill(100));
        pickerSearchBox.setHint(Component.translatable("sablefinder.ui.search.block_picker.hint"));
        panel.child(pickerSearchBox);

        pickerResults = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        pickerResults.gap(1);
        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), pickerResults));

        populateBlockPicker();
        pickerSearchBox.onChanged().subscribe(value -> populateBlockPicker());
        updatePickerConfirmButton();

        return panel;
    }

    private void populateBlockPicker() {
        if (pickerResults == null) {
            return;
        }

        pickerResults.clearChildren();

        final String raw = pickerSearchBox != null ? pickerSearchBox.getValue() : "";
        final String needle = raw.trim().toLowerCase(Locale.ROOT);
        int shown = 0;

        for (final Block block : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);

            if (ResourceLocation.fromNamespaceAndPath("minecraft","air").equals(id)) {
                continue;
            }

            final String idString = id.toString();

            if (!needle.isEmpty()
                    && !idString.toLowerCase(Locale.ROOT).contains(needle)
                    && !block.getName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }

            pickerResults.child(buildBlockPickerRow(block, id));

            if (++shown >= MAX_PICKER_RESULTS) {
                break;
            }
        }

        if (shown == 0) {
            pickerResults.child(Components.label(Component.translatable("sablefinder.ui.search.block_picker.none")));
        }
    }

    private @NotNull FlowLayout buildBlockPickerRow(@NotNull Block block, ResourceLocation id) {
        final FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(3);
        row.verticalAlignment(VerticalAlignment.CENTER);

        final BlockComponent icon = Components.block(block.defaultBlockState());
        icon.horizontalSizing(Sizing.fixed(16));
        icon.verticalSizing(Sizing.fixed(16));
        row.child(icon);

        final boolean picked = pickerSelection.contains(id);
        final boolean alreadySelected = selectedBlocks.containsKey(id);

        final StringBuilder prefix = new StringBuilder();
        if (picked) {
            prefix.append("§a✔ ");
        } else if (alreadySelected) {
            prefix.append("§8✔ ");
        }

        final ButtonComponent button = Components.button(
                Component.literal(prefix + block.getName().getString()).append(Component.literal(" §8" + id)),
                pressed -> togglePickedBlock(id)
        );
        button.horizontalSizing(Sizing.expand());
        button.verticalSizing(Sizing.fixed(16));
        button.tooltip(Component.translatable(picked
                ? "sablefinder.ui.search.block_picker.unselect"
                : alreadySelected
                ? "sablefinder.ui.search.block_picker.already"
                : "sablefinder.ui.search.block_picker.add"));
        row.child(button);

        return row;
    }

    private void togglePickedBlock(ResourceLocation id) {
        if (!this.pickerSelection.remove(id)) {
            this.pickerSelection.add(id);
        }

        populateBlockPicker();
        updatePickerConfirmButton();
    }

    private void updatePickerConfirmButton() {
        if (pickerConfirmButton != null) {
            pickerConfirmButton.active = !pickerSelection.isEmpty();
        }
    }

    private void confirmPickedBlocks() {
        if (pickerSelection.isEmpty()) {
            return;
        }

        for (final ResourceLocation id : pickerSelection) {
            selectedBlocks.putIfAbsent(id, BlockFilter.of(id));
        }

        useBlocksCheckbox.checked(true);
        rebuildSelectedBlocks();
        closeBlockPicker();
    }

    private void closeBlockPicker() {
        this.pickerResults = null;
        this.pickerSearchBox = null;
        this.pickerConfirmButton = null;
        this.pickerSelection.clear();

        if (this.contentHost == null || this.mainContent == null) {
            return;
        }

        if (!isBlockPickerOpen()) {
            return;
        }

        this.contentHost.clearChildren();
        this.contentHost.child(this.mainContent);
    }

    private boolean isBlockPickerOpen() {
        return this.contentHost != null
                && this.mainContent != null
                && !(this.contentHost.children().size() == 1 && this.contentHost.children().getFirst() == this.mainContent);
    }

    private void rebuildSelectedBlocks() {
        if (selectedBlocksList == null) {
            return;
        }

        selectedBlocksList.clearChildren();

        if (selectedBlocks.isEmpty()) {
            selectedBlocksList.child(Components.label(Component.translatable("sablefinder.ui.search.blocks.empty")));
        } else {
            for (final BlockFilter filter : selectedBlocks.values()) {
                selectedBlocksList.child(buildSelectedBlockRow(filter));
            }
        }

        updateSearchButton();
        updateStatus();
    }

    private @NotNull FlowLayout buildSelectedBlockRow(@NotNull BlockFilter filter) {
        final ResourceLocation id = filter.block();

        final FlowLayout row = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        row.surface(Surface.flat(0xFF2B2F36));
        row.padding(Insets.of(3));
        row.gap(3);

        final FlowLayout top = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        top.gap(4);
        top.verticalAlignment(VerticalAlignment.CENTER);

        final ButtonComponent remove = Components.button(Component.literal("✕"), button -> {
            selectedBlocks.remove(id);
            rebuildSelectedBlocks();
        });
        remove.tooltip(Component.translatable("sablefinder.ui.search.blocks.remove"));
        remove.horizontalSizing(Sizing.fixed(16));
        remove.verticalSizing(Sizing.fixed(16));
        top.child(remove);

        final Block block = BuiltInRegistries.BLOCK.get(id);
        final BlockComponent icon = Components.block(block.defaultBlockState());
        icon.horizontalSizing(Sizing.fixed(16));
        icon.verticalSizing(Sizing.fixed(16));
        top.child(icon);

        final LabelComponent label = Components.label(Component.literal(id.toString()));
        label.horizontalSizing(Sizing.expand());
        top.child(label);
        row.child(top);

        final ButtonComponent methodButton = Components.button(
                Component.translatable("sablefinder.ui.search.blocks.method", methodName(filter.method())),
                button -> {
                    final BlockSearchMethod[] methods = BlockSearchMethod.values();
                    final BlockSearchMethod next =
                            methods[(filter.method().ordinal() + 1) % methods.length];

                    updateBlockFilter(id, next, filter.minCount(), filter.maxCount());
                    rebuildSelectedBlocks();
                }
        );
        methodButton.tooltip(Component.translatable("sablefinder.ui.search.block_size.tooltip"));
        methodButton.horizontalSizing(Sizing.fill(100));
        methodButton.verticalSizing(Sizing.fixed(14));
        row.child(methodButton);

        final TextBoxComponent minBox = Components.textBox(
                Sizing.fill(100), String.valueOf(filter.minCount())
        );
        minBox.setMaxLength(9);
        minBox.tooltip(Component.translatable("sablefinder.ui.search.blocks.amount.tooltip"));
        minBox.onChanged().subscribe(value -> {
            final BlockFilter current = selectedBlocks.get(id);
            if (current != null) {
                updateBlockFilter(id, current.method(), Math.max(parseInteger(value), 0), current.maxCount());
            }
        });

        // Built even when BETWEEN is not selected, so switching the method keeps whatever
        // upper bound was typed last. filterEntries decides whether to show it.
        final TextBoxComponent maxBox = Components.textBox(
                Sizing.fill(100), String.valueOf(filter.maxCount())
        );
        maxBox.setMaxLength(9);
        maxBox.onChanged().subscribe(value -> {
            final BlockFilter current = selectedBlocks.get(id);
            if (current != null) {
                updateBlockFilter(id, current.method(), current.minCount(), Math.max(parseInteger(value), 0));
            }
        });

        row.child(filterEntries(
                filter.method(),
                minBox,
                maxBox,
                "sablefinder.ui.search.blocks.count",
                "sablefinder.ui.search.blocks.max"
        ));

        return row;
    }

    private void updateBlockFilter(@NotNull ResourceLocation id, @NotNull BlockSearchMethod method, int min, int max) {
        selectedBlocks.put(id, new BlockFilter(id, method, min, max));
        onFilterToggled();
    }

    // --- actions -----------------------------------------------------------

    private void onSearchButtonPressed() {
        // Nothing will be sent, so don't spin the UI waiting for a response that never arrives.
        if (!ClientPayloadHandler.allowOperation()) {
            return;
        }
        if (searching) {
            cancelSearch();
        } else {
            performSearch();
        }
    }

    private void performSearch() {
        if (searching) {
            return;
        }

        final SearchFilters filters = buildFilters();
        closeBlockPicker();
        setSearching(true);

        final SearchRequest request = SableContraptionClient.search(filters);
        activeSearch = request;

        request.future().whenComplete((tag, error) ->
                Minecraft.getInstance().execute(() -> {
                    if (activeSearch != request) {
                        return;
                    }

                    activeSearch = null;
                    setSearching(false);

                    if (isCancellation(error)) {
                        statusLabel.text(Component.translatable("sablefinder.ui.search.cancelled"));
                        return;
                    }

                    if (error != null || tag == null) {
                        final String reason = error != null && error.getMessage() != null
                                ? error.getMessage()
                                : Component.translatable("sablefinder.ui.error.unknown").getString();

                        statusLabel.text(Component.translatable("sablefinder.ui.error.search", reason));
                        return;
                    }

                    // The server reports scan/flush failures through the result tag so they
                    // reach the player instead of the future dying on the scan thread.
                    final String searchError = tag.getString(SearchEngine.ERROR_KEY);
                    if (!searchError.isEmpty()) {
                        statusLabel.text(Component.literal(searchError));
                        return;
                    }

                    parent.applySearchResults(tag);
                    onClose();
                })
        );
    }

    private void cancelSearch() {
        final SearchRequest request = activeSearch;
        activeSearch = null;

        if (request != null) {
            request.cancel();
        }

        setSearching(false);
        statusLabel.text(Component.translatable("sablefinder.ui.search.cancelled"));
    }

    private static boolean isCancellation(Throwable error) {
        return error instanceof CancellationException
                || (error instanceof CompletionException && error.getCause() instanceof CancellationException);
    }

    private void resetFilters() {
        useNameCheckbox.checked(false);
        nameBox.text("");

        useUuidCheckbox.checked(false);
        uuidBox.text("");

        useBlockSizeCheckbox.checked(false);
        setBlockSearchMethod(BlockSearchMethod.AT_LEAST_OR_EQUAL);
        blockSizeBox.text("0");
        blockSizeMaxBox.text("0");

        useBlocksCheckbox.checked(false);
        selectedBlocks.clear();
        rebuildSelectedBlocks();
        closeBlockPicker();

        onFilterToggled();
    }

    private void cycleBlockSearchMethod(@NotNull ButtonComponent button) {
        final BlockSearchMethod[] methods = BlockSearchMethod.values();
        setBlockSearchMethod(methods[(blockSearchMethod.ordinal() + 1) % methods.length]);
    }

    private void setBlockSearchMethod(@NotNull BlockSearchMethod method) {
        blockSearchMethod = method;
        blockSizeMethodButton.setMessage(blockSizeMethodLabel());
        rebuildBlockSizeEntries();
        onFilterToggled();
    }

    private void rebuildBlockSizeEntries() {
        if (blockSizeEntries == null) {
            return;
        }

        blockSizeEntries.clearChildren();
        blockSizeEntries.child(filterEntries(
                blockSearchMethod,
                blockSizeBox,
                blockSizeMaxBox,
                "sablefinder.ui.search.block_size.min",
                "sablefinder.ui.search.block_size.max"
        ));
    }

    // --- state -------------------------------------------------------------

    private @NotNull SearchFilters buildFilters() {
        return new SearchFilters(
                useNameCheckbox.selected() && !nameBox.getValue().isBlank(),
                nameBox.getValue(),

                useUuidCheckbox.selected() && !uuidBox.getValue().isBlank(),
                uuidBox.getValue(),

                useBlockSizeCheckbox.selected(),
                blockSearchMethod,
                parseInteger(blockSizeBox.getValue()),
                parseInteger(blockSizeMaxBox.getValue()),

                useBlocksCheckbox.selected() && !selectedBlocks.isEmpty(),
                buildBlockFilters()
        );
    }

    private @NotNull ArrayList<BlockFilter> buildBlockFilters() {
        return new ArrayList<>(selectedBlocks.values());
    }

    private void onFilterToggled() {
        updateSearchButton();
        updateStatus();
    }

    private void setSearching(boolean searching) {
        this.searching = searching;
        searchButton.setMessage(Component.translatable(searching
                ? "sablefinder.ui.search.cancel"
                : "sablefinder.ui.button.search"));
        updateSearchButton();
        updateStatus();
    }

    private void updateSearchButton() {
        searchButton.active = searching || activeFilterCount() > 0;
    }

    private void updateStatus() {
        if (statusLabel == null) {
            return;
        }

        if (searching) {
            statusLabel.text(Component.translatable("sablefinder.ui.search.searching"));
            return;
        }

        final int active = activeFilterCount();
        statusLabel.text(active == 0
                ? Component.translatable("sablefinder.ui.search.results.none")
                : Component.translatable("sablefinder.ui.search.results.ready", active));
    }

    private int activeFilterCount() {
        int count = 0;

        if (useNameCheckbox.selected() && !nameBox.getValue().isBlank()) count++;
        if (useUuidCheckbox.selected() && !uuidBox.getValue().isBlank()) count++;
        if (useBlockSizeCheckbox.selected()) count++;
        if (useBlocksCheckbox.selected() && !selectedBlocks.isEmpty()) count++;

        return count;
    }

    // --- helpers -----------------------------------------------------------

    private @NotNull Component blockSizeMethodLabel() {
        return Component.translatable("sablefinder.ui.search.block_size.method", methodName(blockSearchMethod));
    }

    private static @NotNull Component methodName(@NotNull BlockSearchMethod method) {
        return Component.translatable(switch (method) {
            case EQUAL -> "sablefinder.ui.search.method.equal";
            case LESS_THEN_OR_EQUAL -> "sablefinder.ui.search.method.less_then_or_equal";
            case BETWEEN -> "sablefinder.ui.search.method.between";
            case AT_LEAST_OR_EQUAL -> "sablefinder.ui.search.method.at_least_or_equal";
        });
    }

    private static @NotNull FlowLayout card() {
        final FlowLayout card = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(0xFF23262D));
        card.padding(Insets.of(4));
        card.gap(2);
        return card;
    }

    private static @NotNull FlowLayout horizontalSpacer() {
        return Containers.verticalFlow(Sizing.expand(), Sizing.fixed(0));
    }

    private static @NotNull FlowLayout filterEntries(@NotNull BlockSearchMethod method,
                                                     @NotNull TextBoxComponent first,
                                                     @NotNull TextBoxComponent second,
                                                     @NotNull String firstLabelKey,
                                                     @NotNull String secondLabelKey) {
        final FlowLayout column = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        column.gap(1);

        column.child(Components.label(Component.translatable(firstLabelKey)));
        column.child(first);

        if (method == BlockSearchMethod.BETWEEN) {
            column.child(Components.label(Component.translatable(secondLabelKey)));
            column.child(second);
        }

        return column;
    }

    private static int parseInteger(@NotNull String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && isBlockPickerOpen()) {
            closeBlockPicker();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveConfig();
        closeBlockPicker();

        final SearchRequest request = activeSearch;
        activeSearch = null;
        if (request != null) {
            request.cancel();
        }

        Minecraft.getInstance().setScreen(parent);
    }
}
