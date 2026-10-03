package com.mas6y6.sablefinder.sable;

import com.mas6y6.sablefinder.SableFinder;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SableUtils {
    public static List<SableContraptionData> getAllContraptions(MinecraftServer server) {
        final ServerLevel level = server.overworld();

        SableFinder.LOGGER.info("Saving world...");
        level.getServer().saveEverything(false, true, true);

        final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
        var storage = container.getHoldingChunkMap().getStorage();

        var list = new ArrayList<SableContraptionData>();

        final File[] regionFiles = storage.getFolder().toFile().listFiles((dir, name) -> name.endsWith(SubLevelRegionFile.FILE_EXTENSION));

        if (regionFiles != null) {
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

                            final SableContraptionData contraption = SableContraptionData.parse(data, level);

                            list.add(contraption);
                        }
                    }
                }
            }
        }

        return list;
    }

    public static SableContraptionData getContraption(MinecraftServer server, UUID uuid) {
        return getAllContraptions(server).stream()
                .filter(contraption -> contraption.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }
}
