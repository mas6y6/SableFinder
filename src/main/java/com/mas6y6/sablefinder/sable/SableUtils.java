package com.mas6y6.sablefinder.sable;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.config.ConfigServer;
import com.mas6y6.sablefinder.simulated.SimulatedCompat;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SableUtils {

    private static final ExecutorService SCAN_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "sablefinder-scan");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicBoolean SUGGESTION_CACHE_REFRESH_IN_FLIGHT = new AtomicBoolean(false);

    private static volatile @NotNull List<UUID> cachedContraptionUuids = List.of();
    private static volatile long cachedContraptionUuidsAt = 0L;

    public static @NotNull List<SableContraptionData> getAllContraptions(@NotNull MinecraftServer server) {
        return scanContraptions(buildScanPlan(server));
    }

    public static @NotNull CompletableFuture<List<SableContraptionData>> getAllContraptionsAsync(@NotNull MinecraftServer server, AtomicBoolean cancelled) {
        final List<LevelScan> plan = buildScanPlan(server);

        return CompletableFuture.supplyAsync(() -> scanContraptions(plan, cancelled), SCAN_EXECUTOR);
    }

    public static List<UUID> getCachedContraptionUuids(@NotNull MinecraftServer server) {
        if (System.currentTimeMillis() - cachedContraptionUuidsAt > ConfigServer.suggestionCacheTtlMs()) {
            refreshContraptionUuidCache(server);
        }

        return cachedContraptionUuids;
    }

    private static void refreshContraptionUuidCache(@NotNull MinecraftServer server) {
        if (!SUGGESTION_CACHE_REFRESH_IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }

        final CompletableFuture<List<SableContraptionData>> refresh;
        try {
            refresh = getAllContraptionsAsync(server, null);
        } catch (Exception e) {
            SUGGESTION_CACHE_REFRESH_IN_FLIGHT.set(false);
            SableFinder.LOGGER.error("Failed to start contraption UUID cache refresh", e);
            return;
        }

        refresh.whenComplete((contraptions, error) -> {
            try {
                if (error != null) {
                    SableFinder.LOGGER.error("Failed to refresh contraption UUID cache", error);
                    return;
                }

                cachedContraptionUuids = contraptions.stream()
                        .map(SableContraptionData::uuid)
                        .toList();
                cachedContraptionUuidsAt = System.currentTimeMillis();
            } finally {
                SUGGESTION_CACHE_REFRESH_IN_FLIGHT.set(false);
            }
        });
    }

    private static @NotNull List<LevelScan> buildScanPlan(@NotNull MinecraftServer server) {
        final List<LevelScan> plan = new ArrayList<>();

        final Set<UUID> lockedUuids = SimulatedCompat.isSimulatedInstalled()
                ? SimulatedCompat.getInstance().orElseThrow().getLockedContraptions(server)
                : Set.of();

        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

            if (container == null) {
                continue;
            }

            final Set<UUID> loadedUuids = container.getAllSubLevels().stream()
                    .map(SubLevel::getUniqueId)
                    .collect(Collectors.toSet());

            final SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();

            plan.add(new LevelScan(
                    level,
                    holdingChunkMap.getStorage(),
                    loadedUuids,
                    lockedUuids,
                    captureLiveSubLevels(container, holdingChunkMap)
            ));
        }

        return plan;
    }

    private static @NotNull Map<UUID, SubLevelData> captureLiveSubLevels(
            @NotNull ServerSubLevelContainer container,
            @NotNull SubLevelHoldingChunkMap holdingChunkMap
    ) {
        final Map<UUID, SubLevelData> live = new LinkedHashMap<>();

        for (final HoldingSubLevel holdingSubLevel : readHoldingSubLevels(holdingChunkMap)) {
            final SubLevelData data = holdingSubLevel.data();
            if (data.getOriginLoadedChunk() == null) {
                data.setOriginLoadedChunk(holdingSubLevel.pointer() != null
                        ? holdingSubLevel.pointer().chunkPos()
                        : chunkPosOf(data));
            }

            live.put(data.uuid(), data);
        }

        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            final List<UUID> dependencies = SubLevelHelper.getLoadingDependencyChain(subLevel).stream()
                    .map(SubLevel::getUniqueId)
                    .toList();

            final SubLevelData data = SubLevelSerializer.toData(subLevel, dependencies);

            final Vector3d position = subLevel.logicalPose().position();
            data.setOriginLoadedChunk(new ChunkPos(BlockPos.containing(position.x, position.y, position.z)));

            live.put(data.uuid(), data);
        }

        return live;
    }

    private static @NotNull Collection<HoldingSubLevel> readHoldingSubLevels(@NotNull SubLevelHoldingChunkMap holdingChunkMap) {
        try {
            final Field field = SubLevelHoldingChunkMap.class.getDeclaredField("allHoldingSubLevels");
            field.setAccessible(true);

            final Object value = field.get(holdingChunkMap);
            if (value instanceof Map<?, ?> holding) {
                final List<HoldingSubLevel> collected = new ArrayList<>(holding.size());
                for (final Object entry : holding.values()) {
                    if (entry instanceof HoldingSubLevel holdingSubLevel) {
                        collected.add(holdingSubLevel);
                    }
                }
                return collected;
            }
        } catch (final ReflectiveOperationException | RuntimeException e) {
            SableFinder.LOGGER.warn("Unable to read Sable's in-memory holding sub-levels; scans may show stale data for recently unloaded contraptions", e);
        }

        return List.of();
    }

    private static @NotNull ChunkPos chunkPosOf(@NotNull SubLevelData data) {
        final Vector3d position = data.pose().position();
        return new ChunkPos(BlockPos.containing(position.x, position.y, position.z));
    }


    private static @NotNull List<SableContraptionData> scanContraptions(@NotNull List<LevelScan> plan) {
        return scanContraptions(plan, null);
    }

    private static @NotNull List<SableContraptionData> scanContraptions(@NotNull List<LevelScan> plan, AtomicBoolean cancelled) {
        final List<SableContraptionData> list = new ArrayList<>();

        for (final LevelScan scan : plan) {
            checkCancelled(cancelled);

            final Map<UUID, SubLevelData> live = new LinkedHashMap<>(scan.liveSubLevels());

            forEachStoredSubLevel(scan.storage(), (chunkPos, pointer, data) -> {
                checkCancelled(cancelled);

                final SubLevelData effective = live.remove(data.uuid());

                if (effective != null) {
                    effective.setOriginLoadedChunk(chunkPos);
                }

                final SubLevelData chosen = effective != null ? effective : data;

                list.add(SableContraptionData.parse(
                        chosen,
                        scan.level(),
                        scan.loadedUuids().contains(chosen.uuid()),
                        scan.lockedUuids().contains(chosen.uuid())
                ));
                return true;
            });

            // Loaded before its first save, so the disk walk never saw it. It still belongs in the list.
            for (final SubLevelData data : live.values()) {
                checkCancelled(cancelled);

                list.add(SableContraptionData.parse(
                        data,
                        scan.level(),
                        scan.loadedUuids().contains(data.uuid()),
                        scan.lockedUuids().contains(data.uuid())
                ));
            }
        }

        return list;
    }

    private static void checkCancelled(@Nullable AtomicBoolean cancelled) {
        if (cancelled != null && cancelled.get()) {
            throw new CancellationException("Contraption scan cancelled");
        }
    }

    public static @NotNull CompoundTag buildContraptionsTag(@NotNull MinecraftServer server) {
        return buildContraptionsTag(getAllContraptions(server));
    }

    public static @NotNull CompletableFuture<CompoundTag> buildContraptionsTagAsync(@NotNull MinecraftServer server) {
        return getAllContraptionsAsync(server, null).thenApply(SableUtils::buildContraptionsTag);
    }

    public static @NotNull CompoundTag buildContraptionsTag(@NotNull List<SableContraptionData> contraptions) {
        CompoundTag tag = new CompoundTag();
        var list = new net.minecraft.nbt.ListTag();

        contraptions.forEach(contraption -> {
            var contraptionTag = new CompoundTag();
            contraptionTag.putUUID("uuid", contraption.uuid());
            contraptionTag.putString("name", contraption.displayName() != null ? contraption.displayName() : "");
            contraptionTag.putString("dimension", contraption.dimension() != null ? contraption.dimension().location().toString() : "");
            contraptionTag.putBoolean("is_loaded", contraption.isLoaded());
            contraptionTag.putBoolean("is_locked", contraption.isLocked());

            list.add(contraptionTag);
        });

        tag.put("sableContraptionsUUID", list);
        return tag;
    }

    public static @Nullable SableContraptionData getContraption(@NotNull MinecraftServer server, UUID uuid) {
        return getAllContraptions(server).stream()
                .filter(contraption -> contraption.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public static @NotNull CompletableFuture<SableContraptionData> getContraptionAsync(@NotNull MinecraftServer server, UUID uuid) {
        return getAllContraptionsAsync(server, null)
                .thenApply(list -> list.stream()
                .filter(contraption -> contraption.uuid().equals(uuid))
                .findFirst()
                .orElse(null));
    }

    private record LevelScan(
            ServerLevel level,
            SubLevelStorage storage,
            Set<UUID> loadedUuids,
            Set<UUID> lockedUuids,
            Map<UUID, SubLevelData> liveSubLevels
    ) {}

    public static boolean deleteContraption(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

            if (container == null) {
                continue;
            }

            if (deleteFromContainer(container, uuid)) {
                return true;
            }
        }

        SableFinder.LOGGER.warn("Unable to delete contraption {}: it is not loaded and no stored data was found", uuid);
        return false;
    }

    private static boolean deleteFromContainer(@NotNull ServerSubLevelContainer container, @NotNull UUID uuid) {
        SubLevel subLevel = container.getSubLevel(uuid);
        if (subLevel != null) {
            container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            return true;
        }

        final SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();

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

            SableFinder.LOGGER.warn("Contraption {} could not be loaded for deletion; removing it directly from storage", uuid);
        }

        deleteStoredSubLevel(holdingChunkMap, pointer, uuid);
        return true;
    }

    private static boolean deleteStoredSubLevel(@NotNull SubLevelHoldingChunkMap holdingChunkMap, @NotNull GlobalSavedSubLevelPointer pointer, UUID uuid) {
        final SubLevelStorage storage = holdingChunkMap.getStorage();

        SubLevelHoldingChunk holdingChunk = getLoadedHoldingChunk(holdingChunkMap, pointer.chunkPos());
        if (holdingChunk == null) {
            holdingChunk = storage.attemptLoadHoldingChunk(pointer.chunkPos());
        }

        boolean removedPointer = false;
        if (holdingChunk != null) {
            removedPointer = holdingChunk.getSubLevelPointers().remove(pointer.local());
            storage.attemptSaveHoldingChunk(pointer.chunkPos(), holdingChunk);
        }

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

    private static @Nullable SubLevelHoldingChunk getLoadedHoldingChunk(SubLevelHoldingChunkMap holdingChunkMap, @NotNull ChunkPos chunkPos) {
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

    private static GlobalSavedSubLevelPointer findSubLevelPointer(@NotNull ServerSubLevelContainer container, @NotNull UUID uuid) {
        final SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();

        final HoldingSubLevel holdingSubLevel = holdingChunkMap.getHoldingSubLevel(uuid);
        if (holdingSubLevel != null && holdingSubLevel.pointer() != null) {
            return holdingSubLevel.pointer();
        }
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

    private static void forEachStoredSubLevel(@NotNull SubLevelStorage storage, @NotNull StoredSubLevelVisitor visitor) {
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

    public static boolean isSublevelLoaded(@NotNull MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            if (Objects.requireNonNull(ServerSubLevelContainer.getContainer(level))
                    .getAllSubLevels()
                    .contains(uuid)) {
                return true;
            }
        }
        return false;
    }
}
