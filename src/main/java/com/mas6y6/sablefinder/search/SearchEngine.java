package com.mas6y6.sablefinder.search;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.networking.SearchForContraptionsPacket;
import com.mas6y6.sablefinder.networking.SearchForContraptionsResponsePacket;
import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mas6y6.sablefinder.sable.SableUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchEngine {
    public static void searchPacket(SearchForContraptionsPacket packet, IPayloadContext iPayloadContext) {
        CompletableFuture.runAsync(() -> iPayloadContext.reply(new SearchForContraptionsResponsePacket(packet.requestId(), SearchEngine.search(packet.searchFilters()))));
    }

    private static CompoundTag search(SearchFilters filters) {
        final List<SableContraptionData> contraptions = SableUtils.getAllContraptions(SableFinder.getServer(), filters.saveServer());

        final Pattern namePattern = filters.useNameSearch() ? globToPattern(filters.nameSearch()) : null;
        final Pattern uuidPattern = filters.useUUIDSearch() ? globToPattern(filters.uuidSearch()) : null;
        final Set<ResourceLocation> wantedBlocks = filters.useBlockSearch()
                ? new HashSet<>(filters.blockSearchIDs())
                : null;

        final List<SableContraptionData> matches = contraptions.stream()
                .filter(contraption -> matches(contraption, filters, namePattern, uuidPattern, wantedBlocks))
                .toList();

        return SableUtils.buildContraptionsTag(matches);
    }

    private static boolean matches(SableContraptionData contraption, SearchFilters filters,
                                   Pattern namePattern, Pattern uuidPattern, Set<ResourceLocation> wantedBlocks) {
        if (namePattern != null) {
            final String name = contraption.displayName() != null ? contraption.displayName() : "";
            if (!namePattern.matcher(name).matches()) {
                return false;
            }
        }

        if (uuidPattern != null && !uuidPattern.matcher(contraption.uuid().toString()).matches()) {
            return false;
        }

        if (filters.useBlockSize() || (wantedBlocks != null && !wantedBlocks.isEmpty())) {
            return matchesBlocks(contraption, filters, wantedBlocks);
        }

        return true;
    }

    private static boolean matchesBlocks(SableContraptionData contraption, SearchFilters filters, Set<ResourceLocation> wantedBlocks) {
        final int[] count = {0};
        final boolean[] foundWanted = {false};

        contraption.forEachBlock((pos, state) -> {
            count[0]++;

            if (!foundWanted[0] && wantedBlocks != null
                    && wantedBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                foundWanted[0] = true;
            }
        });

        if (filters.useBlockSize() && !matchesBlockSize(filters, count[0])) {
            return false;
        }

        return wantedBlocks == null || wantedBlocks.isEmpty() || foundWanted[0];
    }

    private static boolean matchesBlockSize(SearchFilters filters, int blockCount) {
        final int requested = filters.blockSize();
        return switch (filters.blockSearchMethod()) {
            case EQUAL -> blockCount == requested;
            case LESS_THEN -> blockCount < requested;
            case GREATER_THEN -> blockCount > requested;
            case BETWEEN -> blockCount >= requested && blockCount <= filters.blockSizeMax();
        };
    }

    private static Pattern globToPattern(String glob) {
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

    private static String escapeRegex(char c) {
        return "\\.^$|+(){}]".indexOf(c) != -1 ? "\\" + c : String.valueOf(c);
    }
}