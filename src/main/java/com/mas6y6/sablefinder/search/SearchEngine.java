package com.mas6y6.sablefinder.search;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.networking.SearchForContraptionsPacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsResponsePacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.sable.SableUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchEngine {

    private static final Map<UUID, AtomicBoolean> ACTIVE_SEARCHES = new ConcurrentHashMap<>();

    public static void searchPacket(@NotNull SearchForContraptionsPacket packet, @NotNull IPayloadContext iPayloadContext) {
        final SearchFilters filters = packet.searchFilters();
        final MinecraftServer server = SableFinder.getServer();

        if (server == null) {
            SableFinder.LOGGER.error("Cannot search for contraptions without a running server");
            return;
        }

        final AtomicBoolean cancelled = new AtomicBoolean(false);
        ACTIVE_SEARCHES.put(packet.requestId(), cancelled);

        SableUtils.getAllContraptionsAsync(server, filters.saveServer(), cancelled)
                .whenComplete((contraptions, error) -> {
                    ACTIVE_SEARCHES.remove(packet.requestId());

                    if (isCancellation(error) || cancelled.get()) {
                        SableFinder.LOGGER.debug("Cancelled search {}", packet.requestId());
                        return;
                    }

                    if (error != null) {
                        SableFinder.LOGGER.error("An error occurred while searching for contraptions", error);
                        return;
                    }

                    final CompoundTag result;
                    try {
                        result = search(contraptions, filters, cancelled);
                    } catch (final CancellationException cancellation) {
                        SableFinder.LOGGER.debug("Cancelled search {}", packet.requestId());
                        return;
                    }

                    if (server.isRunning()) {
                        server.execute(() -> iPayloadContext.reply(new SearchForContraptionsResponsePacket(packet.requestId(), result)));
                    }
                });
    }

    public static void cancel(UUID requestId) {
        final AtomicBoolean cancelled = ACTIVE_SEARCHES.get(requestId);
        if (cancelled != null) {
            cancelled.set(true);
        }
    }

    private static boolean isCancellation(Throwable error) {
        return error instanceof CancellationException
                || (error instanceof CompletionException && error.getCause() instanceof CancellationException);
    }

    private static @NotNull CompoundTag search(@NotNull List<SableContraptionData> contraptions, @NotNull SearchFilters filters, @NotNull AtomicBoolean cancelled) {
        final Pattern namePattern = filters.useNameSearch() ? globToPattern(filters.nameSearch()) : null;
        final Pattern uuidPattern = filters.useUUIDSearch() ? globToPattern(filters.uuidSearch()) : null;
        final Map<ResourceLocation, Integer> requiredBlocks = filters.useBlockSearch()
                ? requiredBlockCounts(filters.blockFilters())
                : null;

        final List<SableContraptionData> matches = new ArrayList<>();

        for (final SableContraptionData contraption : contraptions) {
            if (cancelled.get()) {
                throw new CancellationException("Contraption search cancelled");
            }

            if (matches(contraption, filters, namePattern, uuidPattern, requiredBlocks)) {
                matches.add(contraption);
            }
        }

        return SableUtils.buildContraptionsTag(matches);
    }

    private static @NotNull Map<ResourceLocation, Integer> requiredBlockCounts(@NotNull List<BlockFilter> blockFilters) {
        final Map<ResourceLocation, Integer> required = new HashMap<>();

        for (final BlockFilter filter : blockFilters) {
            required.merge(filter.block(), Math.max(filter.minCount(), 1), Math::max);
        }

        return required;
    }

    private static boolean matches(@NotNull SableContraptionData contraption, @NotNull SearchFilters filters,
                                   @Nullable Pattern namePattern, @Nullable Pattern uuidPattern, @Nullable Map<ResourceLocation, Integer> requiredBlocks) {
        if (namePattern != null) {
            final String name = contraption.displayName() != null ? contraption.displayName() : "";
            if (!namePattern.matcher(name).matches()) {
                return false;
            }
        }

        if (uuidPattern != null && !uuidPattern.matcher(contraption.uuid().toString()).matches()) {
            return false;
        }

        if (filters.useBlockSize() || (requiredBlocks != null && !requiredBlocks.isEmpty())) {
            return matchesBlocks(contraption, filters, requiredBlocks);
        }

        return true;
    }

    private static boolean matchesBlocks(@NotNull SableContraptionData contraption, @NotNull SearchFilters filters,
                                         @Nullable Map<ResourceLocation, Integer> requiredBlocks) {
        final int[] total = {0};
        final Map<ResourceLocation, Integer> found = new HashMap<>();

        contraption.forEachBlock((pos, state) -> {
            total[0]++;

            if (requiredBlocks != null && !requiredBlocks.isEmpty()) {
                final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                if (requiredBlocks.containsKey(id)) {
                    found.merge(id, 1, Integer::sum);
                }
            }
        });

        if (filters.useBlockSize() && !matchesBlockSize(filters, total[0])) {
            return false;
        }

        if (requiredBlocks != null) {
            for (final Map.Entry<ResourceLocation, Integer> requirement : requiredBlocks.entrySet()) {
                if (found.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean matchesBlockSize(@NotNull SearchFilters filters, int blockCount) {
        final int requested = filters.blockSize();
        return switch (filters.blockSearchMethod()) {
            case EQUAL -> blockCount == requested;
            case LESS_THEN -> blockCount < requested;
            case GREATER_THEN -> blockCount > requested;
            case BETWEEN -> blockCount >= requested && blockCount <= filters.blockSizeMax();
        };
    }

    private static @NotNull Pattern globToPattern(@NotNull String glob) {
        final StringBuilder regex = new StringBuilder();

        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        i++;
                    }
                    regex.append(".*");
                }
                case '?' -> regex.append('.');
                case '[' -> {
                    final int end = glob.indexOf(']', i + 1);
                    if (end == -1) {
                        regex.append("\\[");
                        break;
                    }

                    final String classBody = glob.substring(i + 1, end);
                    if (classBody.startsWith("!") || classBody.startsWith("^")) {
                        regex.append("[^").append(classBody.substring(1)).append(']');
                    } else {
                        regex.append('[').append(classBody).append(']');
                    }

                    i = end;
                }
                case '\\' -> {
                    if (i + 1 < glob.length()) {
                        regex.append(Pattern.quote(String.valueOf(glob.charAt(i + 1))));
                        i++;
                    } else {
                        regex.append("\\\\");
                    }
                }
                default -> regex.append(escapeRegex(c));
            }
        }

        try {
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            SableFinder.LOGGER.warn("Invalid glob pattern '{}' ({}) - matching literally", glob, e.getMessage());
            return Pattern.compile(Pattern.quote(glob), Pattern.CASE_INSENSITIVE);
        }
    }

    private static @NotNull String escapeRegex(char c) {
        return "\\.^$|+(){}]".indexOf(c) != -1 ? "\\" + c : String.valueOf(c);
    }
}