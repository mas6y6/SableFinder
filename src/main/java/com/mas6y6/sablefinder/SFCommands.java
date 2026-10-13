package com.mas6y6.sablefinder;

import com.mas6y6.sablefinder.networking.CheckExistencePacket;
import com.mas6y6.sablefinder.networking.OpenGUIPacket;
import com.mas6y6.sablefinder.networking.PacketRequests;
import com.mas6y6.sablefinder.sable.SableUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.minecraft.commands.Commands.argument;

public class SFCommands {
    private static final long RESPONSE_TIMEOUT_SECONDS = 5;

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sablefinder")
                        .then(
                                Commands.literal("view_uuid")
                                        .then(
                                                argument("uuid", StringArgumentType.string())
                                                        .suggests((context,builder) -> {
                                                            SableUtils.getCachedContraptionUuids(context.getSource().getServer())
                                                                    .forEach(uuid -> builder.suggest(uuid.toString()));

                                                            return builder.buildFuture();
                                                        })
                                                        .executes(SFCommands::executeVIEWUUID)
                                        )
                        )
                        .then(
                                Commands.literal("view_name")
                        )
                        .then(Commands.literal("open").executes(SFCommands::executeOPENGUI))
        );
    }

    private static int executeVIEWUUID(@NotNull CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.not_player"));
            return 0;
        }

        if (!context.getSource().hasPermission(3)) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.no_permission"));
            return 0;
        }

        try {
            var entity = context.getSource().getEntityOrException();
            if (entity instanceof ServerPlayer player) {
                MinecraftServer server = context.getSource().getServer();
                CompletableFuture.supplyAsync(() -> awaitClientResponse(player))
                        .thenAcceptAsync(clientHasMod -> {
                            if (!clientHasMod) {
                                context.getSource().sendFailure(Component.translatable("sablefinder.message.client_mod_missing"));
                                return;
                            }

                            final Optional<UUID> selectedUuid;
                            try {
                                selectedUuid = Optional.of(UUID.fromString(context.getArgument("uuid", String.class)));
                            } catch (Exception e) {
                                context.getSource().sendFailure(Component.translatable("sablefinder.message.invalid_uuid"));
                                return;
                            }

                            SableUtils.buildContraptionsTagAsync(Objects.requireNonNull(entity.getServer()))
                                    .whenComplete((tag, error) -> {
                                        if (error != null) {
                                            SableFinder.LOGGER.error("An error occurred while loading contraptions", error);
                                            server.execute(() -> context.getSource().sendFailure(Component.translatable("sablefinder.message.load_error", error.getMessage())));
                                            return;
                                        }

                                        server.execute(() -> PacketDistributor.sendToPlayer(player, new OpenGUIPacket(tag, selectedUuid)));
                                    });
                        }, server::execute);
            } else {
                context.getSource().sendFailure(Component.translatable("sablefinder.message.not_player"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.command_error", e.getMessage()));
            SableFinder.LOGGER.error("An error occurred while executing the SableFinder command", e);
        }

        return 0;
    }

    private static int executeOPENGUI(@NotNull CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.not_player"));
            return 0;
        }

        if (!context.getSource().hasPermission(3)) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.no_permission"));
            return 0;
        }

        try {
            var entity = context.getSource().getEntityOrException();
            if (entity instanceof ServerPlayer player) {
                MinecraftServer server = context.getSource().getServer();
                CompletableFuture.supplyAsync(() -> awaitClientResponse(player))
                        .thenAcceptAsync(clientHasMod -> {
                            if (!clientHasMod) {
                                context.getSource().sendFailure(Component.translatable("sablefinder.message.client_mod_missing"));
                                return;
                            }

                            SableUtils.buildContraptionsTagAsync(Objects.requireNonNull(entity.getServer()))
                                    .whenComplete((tag, error) -> {
                                        if (error != null) {
                                            SableFinder.LOGGER.error("An error occurred while loading contraptions", error);
                                            server.execute(() -> context.getSource().sendFailure(Component.translatable("sablefinder.message.load_error", error.getMessage())));
                                            return;
                                        }

                                        server.execute(() -> PacketDistributor.sendToPlayer(player, new OpenGUIPacket(tag, Optional.empty())));
                                    });
                        }, server::execute);
            } else {
                context.getSource().sendFailure(Component.translatable("sablefinder.message.not_player"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("sablefinder.message.command_error", e.getMessage()));
            SableFinder.LOGGER.error("An error occurred while executing the SableFinder command", e);
        }

        return 0;
    }

    private static boolean awaitClientResponse(@NotNull ServerPlayer player) {
        final UUID requestId = PacketRequests.newId();
        try {
            return PacketRequests.sendToPlayer(player, new CheckExistencePacket(requestId, true, false))
                    .get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .isClientHasMod();
        } catch (TimeoutException e) {
            PacketRequests.cancel(requestId);
            return false;
        } catch (Exception e) {
            PacketRequests.cancel(requestId);
            SableFinder.LOGGER.error("An error occurred while verifying that the client has the SableFinder mod installed", e);
            return false;
        }
    }
}