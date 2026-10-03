package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.networking.CheckExistencePacket;
import com.mas6y6.sablefinder.networking.OpenGUIPacket;
import com.mas6y6.sablefinder.sable.SableUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SFCommands {
    private static final long RESPONSE_TIMEOUT_SECONDS = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sablefinder").executes(SFCommands::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.literal("You must be a player to use this command."));
            return 0;
        }

        try {
            var entity = context.getSource().getEntityOrException();
            if (entity instanceof ServerPlayer player) {
                MinecraftServer server = context.getSource().getServer();
                CompletableFuture.supplyAsync(() -> awaitClientResponse(player))
                        .thenAcceptAsync(clientHasMod -> {
                            if (!clientHasMod) {
                                context.getSource().sendFailure(Component.literal("The client does not have the SableFinder mod installed."));
                                return;
                            }

                            try {
                                CompoundTag tag = new CompoundTag();
                                var list = new ListTag();

                                SableUtils.getAllContraptions(Objects.requireNonNull(entity.getServer())).stream().forEach(contraption -> {
                                    var contraptionTag = new CompoundTag();

                                    contraptionTag.putUUID("uuid", contraption.uuid());
                                    contraptionTag.putString("name", contraption.displayName() != null ? contraption.displayName() : "");

                                    list.add(contraptionTag);
                                });

                                tag.put("sableContraptionsUUID", list);

                                PacketDistributor.sendToPlayer(player, new OpenGUIPacket(tag));
                            } catch (Exception e) {
                                SableFinder.LOGGER.error("An error occurred while loading contraptions", e);
                                context.getSource().sendFailure(Component.literal("An error occurred while loading contraptions: " + e.getMessage()));
                            }
                        }, server::execute);
            } else {
                context.getSource().sendFailure(Component.literal("This command can only be executed by a player."));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("An error occurred while executing the SableFinder command: " + e.getMessage()));
            SableFinder.LOGGER.error("An error occurred while executing the SableFinder command", e);
        }

        return 0;
    }

    private static boolean awaitClientResponse(ServerPlayer player) {
        CompletableFuture<Boolean> response = CheckExistencePacket.request(player.getUUID());
        try {
            PacketDistributor.sendToPlayer(player, new CheckExistencePacket(true, false));
            return response.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            SableFinder.LOGGER.error("An error occurred while verifying that the client has the SableFinder mod installed", e);
            return false;
        } finally {
            CheckExistencePacket.removePending(player.getUUID());
            response.cancel(true);
        }
    }
}

/*
    final ServerLevel level = context.getSource().getLevel();

        context.getSource().sendSuccess(() -> Component.literal("Saving world..."), true);
        level.getServer().saveEverything(false, true, true);

        final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
        var storage = container.getHoldingChunkMap().getStorage();

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

                            final var component = Component.literal("UUID: " + contraption.uuid());
                            component.append(Component.literal("\nPose: " + contraption.pose()));
                            component.append(Component.literal("\nChunks: " + contraption.chunkCount()));

                            final int[] nonAirCount = {0};
                            contraption.forEachBlock((pos, state) -> {
                                nonAirCount[0]++;
                                component.append(Component.literal("\nBlock " + pos + ": " + state));
                            });

                            component.append(Component.literal("\nTotal non-air blocks: " + nonAirCount[0]));

                            context.getSource().sendSuccess(() -> component, true);
                        }
                    }
                }
            }
        }

        return 0;
*/