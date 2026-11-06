package com.mas6y6.sablefinder.search;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.networking.ClientToastMessagePacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsRequestPacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsResponsePacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.sable.SableUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SearchEngine {

    private static final Map<UUID, AtomicBoolean> ACTIVE_SEARCHES = new ConcurrentHashMap<>();

    private static final class SearchProgress {
        private static final long MIN_INTERVAL_MS = 400L;

        private final @Nullable ServerPlayer player;
        private final AtomicLong lastSentAt = new AtomicLong(0L);

        SearchProgress(@Nullable ServerPlayer player) {
            this.player = player;
        }

        void stage(@NotNull Component message) {
            send(message);
            lastSentAt.set(System.currentTimeMillis());
        }

        void scanned(int count) {
            final long now = System.currentTimeMillis();
            final long last = lastSentAt.get();

            if (now - last < MIN_INTERVAL_MS || !lastSentAt.compareAndSet(last, now)) {
                return;
            }

            send(Component.translatable("sablefinder.message.search.scanning", count));
        }

        private void send(@NotNull Component message) {
            final ServerPlayer target = player;

            if (target == null) {
                return;
            }

            final MinecraftServer server = target.getServer();

            if (server == null) {
                return;
            }

            server.execute(() -> PacketDistributor.sendToPlayer(target, new ClientToastMessagePacket(
                    ClientToastMessagePacket.SLOT_PROGRESS,
                    Component.translatable("sablefinder.message.search.progress_title"),
                    message
            )));
        }
    }

    public static void searchPacket(@NotNull SearchForContraptionsRequestPacket packet, @NotNull IPayloadContext iPayloadContext) {
        final SearchFilters filters = packet.searchFilters();
        final MinecraftServer server = SableFinder.getServer();

        if (server == null) {
            SableFinder.LOGGER.error("Cannot search for contraptions without a running server");
            return;
        }

        // Present so scan failures can be surfaced to whoever asked; empty for non-player sources.
        final Optional<ServerPlayer> requester = iPayloadContext.player() instanceof ServerPlayer searcher
                ? Optional.of(searcher)
                : Optional.empty();

        final AtomicBoolean cancelled = new AtomicBoolean(false);
        ACTIVE_SEARCHES.put(packet.requestId(), cancelled);

        final SearchProgress progress = new SearchProgress(requester.orElse(null));

        final CompletableFuture<List<SableContraptionData>> scan;
        try {
            progress.stage(Component.translatable("sablefinder.message.search.starting"));
            scan = SableUtils.getAllContraptionsAsync(server, cancelled, requester, progress::scanned);
        } catch (Exception exception) {
            ACTIVE_SEARCHES.remove(packet.requestId());
            SableFinder.LOGGER.error("Failed to start a contraption search", exception);
            progress.stage(Component.translatable("sablefinder.message.search.failed"));

            iPayloadContext.reply(new SearchForContraptionsResponsePacket(
                    packet.requestId(),
                    errorTag(errorReason(exception))
            ));

            return;
        }

        scan
                .whenComplete((contraptions, error) -> {
                    ACTIVE_SEARCHES.remove(packet.requestId());

                    if (isCancellation(error) || cancelled.get()) {
                        SableFinder.LOGGER.debug("Cancelled search {}", packet.requestId());
                        progress.stage(Component.translatable("sablefinder.message.search.cancelled"));
                        return;
                    }

                    if (error != null) {
                        SableFinder.LOGGER.error("An error occurred while searching for contraptions", error);
                        progress.stage(Component.translatable("sablefinder.message.search.failed"));
                        return;
                    }

                    progress.stage(Component.translatable("sablefinder.message.search.filtering", contraptions.size()));

                    final CompoundTag result;
                    try {
                        result = search(contraptions, filters, cancelled, requester);
                    } catch (final CancellationException cancellation) {
                        SableFinder.LOGGER.debug("Cancelled search {}", packet.requestId());
                        progress.stage(Component.translatable("sablefinder.message.search.cancelled"));
                        return;
                    }

                    progress.stage(Component.translatable(
                            "sablefinder.message.search.done",
                            result.getList("sableContraptionsUUID", Tag.TAG_COMPOUND).size()
                    ));

                    if (server.isRunning()) {
                        server.execute(() -> iPayloadContext.reply(new SearchForContraptionsResponsePacket(packet.requestId(), result)));
                    }
                }).exceptionally(throwable -> {
                    SableFinder.LOGGER.error("An error occurred while searching for contraptions", throwable);
                    progress.stage(Component.translatable("sablefinder.message.search.failed"));

                    if (server.isRunning()) {
                        server.execute(() -> iPayloadContext.reply(new SearchForContraptionsResponsePacket(
                                packet.requestId(),
                                errorTag(errorReason(throwable))
                        )));
                    }

                    return null;
                });
    }

    private static @NotNull Component errorReason(@NotNull Throwable throwable) {
        final Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

        return cause.getMessage() != null
                ? Component.literal(cause.getMessage())
                : Component.translatable("sablefinder.ui.error.unknown");
    }

    public static void cancel(UUID requestId) {
        final AtomicBoolean cancelled = ACTIVE_SEARCHES.get(requestId);
        if (cancelled != null) {
            cancelled.set(true);
        }
    }

    public static final String ERROR_KEY = "sablefinderSearchError";

    private static @NotNull CompoundTag errorTag(@NotNull Component reason) {
        final CompoundTag tag = new CompoundTag();
        tag.putString(ERROR_KEY, Component.translatable("sablefinder.ui.error.search", reason).getString());
        return tag;
    }

    private static boolean isCancellation(Throwable error) {
        return error instanceof CancellationException
                || (error instanceof CompletionException && error.getCause() instanceof CancellationException);
    }

    private static @NotNull CompoundTag search(@NotNull List<SableContraptionData> contraptions, @NotNull SearchFilters filters,
                                            @NotNull AtomicBoolean cancelled, @NotNull Optional<ServerPlayer> requester) {
        final String nameQuery = filters.useNameSearch() ? filters.nameSearch() : null;
        final String uuidQuery = filters.useUUIDSearch() ? filters.uuidSearch() : null;
        final Map<ResourceLocation, BlockFilter> requiredBlocks = filters.useBlockSearch()
                ? requiredBlockCounts(filters.blockFilters())
                : null;

        final List<SableContraptionData> matches = new ArrayList<>();

        for (final SableContraptionData contraption : contraptions) {
            if (cancelled.get()) {
                throw new CancellationException("Contraption search cancelled");
            }

            if (matches(contraption, filters, nameQuery, uuidQuery, requiredBlocks)) {
                matches.add(contraption);
            }
        }

        return SableUtils.buildContraptionsTag(matches, requester);
    }

    private static @NotNull Map<ResourceLocation, BlockFilter> requiredBlockCounts(@NotNull List<BlockFilter> blockFilters) {
        final Map<ResourceLocation, BlockFilter> required = new HashMap<>();

        for (final BlockFilter filter : blockFilters) {
            required.put(filter.block(), filter);
        }

        return required;
    }

    private static boolean matches(@NotNull SableContraptionData contraption, @NotNull SearchFilters filters,
                                   @Nullable String nameQuery, @Nullable String uuidQuery, @Nullable Map<ResourceLocation, BlockFilter> requiredBlocks) {
        final TextSearchMethod method = filters.textMethod();

        if (nameQuery != null) {
            final String name = contraption.displayName() != null ? contraption.displayName() : "";
            if (!method.matches(nameQuery, name)) {
                return false;
            }
        }

        if (uuidQuery != null && !method.matches(uuidQuery, contraption.uuid().toString())) {
            return false;
        }

        if (filters.useBlockSize() || (requiredBlocks != null && !requiredBlocks.isEmpty())) {
            return matchesBlocks(contraption, filters, requiredBlocks);
        }

        return true;
    }

    private static boolean matchesBlocks(@NotNull SableContraptionData contraption, @NotNull SearchFilters filters,
                                         @Nullable Map<ResourceLocation, BlockFilter> requiredBlocks) {
        final Map<ResourceLocation, BlockFilter> required = requiredBlocks != null && !requiredBlocks.isEmpty()
                ? requiredBlocks
                : Map.of();

        // Resolved once up front: matching on Block identity avoids a reverse registry lookup per block.
        final Map<Block, ResourceLocation> wanted = new HashMap<>();
        for (final ResourceLocation id : required.keySet()) {
            wanted.put(BuiltInRegistries.BLOCK.get(id), id);
        }

        final int[] total = {0};
        final Map<ResourceLocation, Integer> found = new HashMap<>();

        contraption.forEachBlockCount((block, count) -> {
            total[0] += count;

            final ResourceLocation id = wanted.get(block);
            if (id != null) {
                found.merge(id, count, Integer::sum);
            }
        });

        if (filters.useBlockSize() && !matchesBlockSize(filters, total[0])) {
            return false;
        }

        for (final BlockFilter requirement : required.values()) {
            if (!requirement.matches(found.getOrDefault(requirement.block(), 0))) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesBlockSize(@NotNull SearchFilters filters, int blockCount) {
        return matchesCount(filters.blockSearchMethod(), filters.blockSize(), filters.blockSizeMax(), blockCount);
    }

    public static boolean matchesCount(@NotNull BlockSearchMethod method, int min, int max, int actual) {
        return switch (method) {
            case EQUAL -> actual == min;
            case LESS_THEN_OR_EQUAL -> actual <= min;
            case AT_LEAST_OR_EQUAL -> actual >= min;
            case BETWEEN -> actual >= min && actual <= max;
        };
    }
}
