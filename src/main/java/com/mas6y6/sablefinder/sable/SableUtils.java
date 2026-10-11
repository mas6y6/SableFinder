package com.mas6y6.sablefinder.sable;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.interfaces.Toastable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SableUtils {
    public static List<SableContraptionData> getAllContraptions(MinecraftServer server) {
        return getAllContraptions(server, true);
    }

    public static List<SableContraptionData> getAllContraptions(MinecraftServer server, boolean saveWorld) {
        if (saveWorld) {
            SableFinder.LOGGER.info("Saving world...");
            server.saveEverything(false, true, true);
        }

        final List<SableContraptionData> list = new ArrayList<>();

        // Sub-levels are stored per dimension, so every level's container has to be scanned.
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

            if (container == null) {
                continue;
            }

            final SubLevelStorage storage = container.getHoldingChunkMap().getStorage();
            final List<UUID> loadedUUIDS = container.getAllSubLevels().stream().map(SubLevel::getUniqueId).toList();

            forEachStoredSubLevel(storage, (chunkPos, pointer, data) -> {
                list.add(SableContraptionData.parse(data, level, loadedUUIDS.contains(data.uuid())));
                return true;
            });
        }

        return list;
    }

    public static CompoundTag buildContraptionsTag(MinecraftServer server) {
        CompoundTag tag = new CompoundTag();
        var list = new net.minecraft.nbt.ListTag();

        getAllContraptions(server).forEach(contraption -> {
            var contraptionTag = new CompoundTag();
            contraptionTag.putUUID("uuid", contraption.uuid());
            contraptionTag.putString("name", contraption.displayName() != null ? contraption.displayName() : "");
            contraptionTag.putString("dimension", contraption.dimension() != null ? contraption.dimension().location().toString() : "");
            list.add(contraptionTag);
        });

        tag.put("sableContraptionsUUID", list);
        return tag;
    }

    public static SableContraptionData getContraption(MinecraftServer server, UUID uuid, boolean saveServer) {
        return getAllContraptions(server, saveServer).stream()
                .filter(contraption -> contraption.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public static boolean deleteContraption(MinecraftServer server, UUID uuid) {
        // Sub-levels live in per-dimension containers, so search every dimension for this UUID.
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

            if (container == null) {
                continue;
            }

            if (deleteFromContainer(container, uuid)) {
                showDeleteSuccess(server);
                return true;
            }
        }

        SableFinder.LOGGER.warn("Unable to delete contraption {}: it is not loaded and no stored data was found", uuid);
        showDeleteFailure(server);
        return false;
    }

    /**
     * Attempts to delete a contraption from a single dimension's container.
     *
     * @return whether the contraption was present (and was deleted) in this container
     */
    private static boolean deleteFromContainer(ServerSubLevelContainer container, UUID uuid) {
        // Fast path: the contraption is loaded, so it can be removed directly.
        SubLevel subLevel = container.getSubLevel(uuid);
        if (subLevel != null) {
            container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            return true;
        }

        final SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();

        // The contraption may be sitting in memory, waiting for its chunks to load. If so, let
        // Sable load it and remove it the normal way.
        final HoldingSubLevel holdingSubLevel = holdingChunkMap.getHoldingSubLevel(uuid);

        GlobalSavedSubLevelPointer pointer = null;
        if (holdingSubLevel != null && holdingSubLevel.pointer() != null) {
            pointer = holdingSubLevel.pointer();
        }
        if (pointer == null) {
            pointer = findSubLevelPointer(container, uuid);
        }

        if (pointer == null) {
            return false;
        }

        if (holdingSubLevel != null) {
            holdingChunkMap.snatchAndLoad(pointer, uuid);

            subLevel = container.getSubLevel(uuid);
            if (subLevel != null) {
                container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
                return true;
            }

            // The sub-level exists in memory but its data is corrupt and cannot be loaded.
            // Fall through and remove it straight from storage instead.
            SableFinder.LOGGER.warn("Contraption {} could not be loaded for deletion; removing it directly from storage", uuid);
        }

        // The contraption is not loaded and cannot be loaded (for example, its stored plot is
        // corrupt). Delete its stored data and holding-chunk pointer directly.
        deleteStoredSubLevel(holdingChunkMap, pointer, uuid);
        return true;
    }

    private static boolean deleteStoredSubLevel(SubLevelHoldingChunkMap holdingChunkMap, GlobalSavedSubLevelPointer pointer, UUID uuid) {
        final SubLevelStorage storage = holdingChunkMap.getStorage();

        // Prefer the in-memory holding chunk, since it may contain changes that haven't been
        // saved to disk yet. Fall back to the copy stored on disk.
        SubLevelHoldingChunk holdingChunk = getLoadedHoldingChunk(holdingChunkMap, pointer.chunkPos());
        if (holdingChunk == null) {
            holdingChunk = storage.attemptLoadHoldingChunk(pointer.chunkPos());
        }

        boolean removedPointer = false;
        if (holdingChunk != null) {
            removedPointer = holdingChunk.getSubLevelPointers().remove(pointer.local());
            storage.attemptSaveHoldingChunk(pointer.chunkPos(), holdingChunk);
        }

        // Writing null clears the stored data and frees the sub-level index.
        storage.attemptSaveSubLevel(pointer, null);

        try {
            storage.flush();
        } catch (final IOException e) {
            SableFinder.LOGGER.error("Failed to flush sub-level storage after deleting contraption {}", uuid, e);
        }

        if (!removedPointer) {
            SableFinder.LOGGER.warn("Contraption {} was cleared from storage, but its holding-chunk pointer was already missing", uuid);
        }

        return true;
    }

    private static SubLevelHoldingChunk getLoadedHoldingChunk(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos) {
        try {
            final Field field = SubLevelHoldingChunkMap.class.getDeclaredField("loadedHoldingChunks");
            field.setAccessible(true);

            final Object value = field.get(holdingChunkMap);
            if (value instanceof Map<?, ?> loadedChunks) {
                final Object chunk = loadedChunks.get(chunkPos.toLong());
                if (chunk instanceof SubLevelHoldingChunk holdingChunk) {
                    return holdingChunk;
                }
            }
        } catch (final ReflectiveOperationException e) {
            SableFinder.LOGGER.warn("Unable to read Sable's in-memory holding chunks while deleting a contraption", e);
        }

        return null;
    }

    private static void showDeleteSuccess(MinecraftServer server) {
        if (server instanceof Toastable toastable) {
            toastable.sableFinder$showToast(Component.literal("Contraption Deleted"), Component.literal("Contraption Successfully deleted!"));
        }
    }

    private static void showDeleteFailure(MinecraftServer server) {
        if (server instanceof Toastable toastable) {
            toastable.sableFinder$showToast(Component.literal("Failed to delete"), Component.literal("An error occurred while deleting the contraption. Check server logs for more info."));
        }
    }

    private static GlobalSavedSubLevelPointer findSubLevelPointer(ServerSubLevelContainer container, UUID uuid) {
        final SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();

        // The contraption may already be kept in an in-memory holding chunk waiting to load.
        final HoldingSubLevel holdingSubLevel = holdingChunkMap.getHoldingSubLevel(uuid);
        if (holdingSubLevel != null && holdingSubLevel.pointer() != null) {
            return holdingSubLevel.pointer();
        }

        // Otherwise scan the stored data for the pointer that owns this UUID.
        final GlobalSavedSubLevelPointer[] found = new GlobalSavedSubLevelPointer[1];
        forEachStoredSubLevel(holdingChunkMap.getStorage(), (chunkPos, pointer, data) -> {
            if (uuid.equals(data.uuid())) {
                found[0] = new GlobalSavedSubLevelPointer(chunkPos, pointer.storageIndex(), pointer.subLevelIndex());
                return false;
            }
            return true;
        });

        return found[0];
    }

    private static void forEachStoredSubLevel(SubLevelStorage storage, StoredSubLevelVisitor visitor) {
        final File[] regionFiles = storage.getFolder().toFile().listFiles((dir, name) -> name.endsWith(SubLevelRegionFile.FILE_EXTENSION));

        if (regionFiles == null) {
            return;
        }

        for (final File regionFile : regionFiles) {
            final String fileName = regionFile.getName();
            final String withoutExtension = fileName.substring(0, fileName.length() - SubLevelRegionFile.FILE_EXTENSION.length());
            final String[] parts = withoutExtension.split("\\.");
            if (parts.length != 3) continue;

            final int regionX, regionZ;
            try {
                regionX = Integer.parseInt(parts[1]);
                regionZ = Integer.parseInt(parts[2]);
            } catch (final NumberFormatException e) {
                continue;
            }

            for (int localX = 0; localX < SubLevelRegionFile.SIDE_LENGTH; localX++) {
                for (int localZ = 0; localZ < SubLevelRegionFile.SIDE_LENGTH; localZ++) {
                    final ChunkPos chunkPos = new ChunkPos(
                            regionX * SubLevelRegionFile.SIDE_LENGTH + localX,
                            regionZ * SubLevelRegionFile.SIDE_LENGTH + localZ
                    );

                    final SubLevelHoldingChunk holdingChunk = storage.attemptLoadHoldingChunk(chunkPos);
                    if (holdingChunk == null) continue;

                    for (final SavedSubLevelPointer pointer : holdingChunk.getSubLevelPointers()) {
                        final SubLevelData data = storage.attemptLoadSubLevel(chunkPos, pointer);
                        if (data == null) continue;

                        if (!visitor.visit(chunkPos, pointer, data)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface StoredSubLevelVisitor {
        boolean visit(ChunkPos chunkPos, SavedSubLevelPointer pointer, SubLevelData data);
    }
}
