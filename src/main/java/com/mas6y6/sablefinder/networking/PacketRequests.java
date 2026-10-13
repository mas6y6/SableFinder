package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public final class PacketRequests {
    private static final Map<UUID, PendingRequest> PENDING = new ConcurrentHashMap<>();

    private PacketRequests() {
    }

    public static @NotNull UUID newId() {
        return UUID.randomUUID();
    }

    public static <R extends CustomPacketPayload> @NotNull CompletableFuture<R> sendToServer(@NotNull RequestPayload<R> request) {
        return submit(request, () -> PacketDistributor.sendToServer(request));
    }

    public static <R extends CustomPacketPayload> @NotNull CompletableFuture<R> sendToPlayer(@NotNull ServerPlayer player, @NotNull RequestPayload<R> request) {
        return submit(request, () -> PacketDistributor.sendToPlayer(player, request));
    }

    private static <R extends CustomPacketPayload> @NotNull CompletableFuture<R> submit(@NotNull RequestPayload<R> request, @NotNull Runnable dispatcher) {
        final CompletableFuture<R> future = new CompletableFuture<>();
        PENDING.put(request.requestId(), new PendingRequest(future, request.responseType()));

        try {
            dispatcher.run();
        } catch (final Throwable throwable) {
            PENDING.remove(request.requestId());
            future.completeExceptionally(throwable);
        }

        return future;
    }

    @SuppressWarnings("unchecked")
    public static void complete(@NotNull ResponsePayload response) {
        final PendingRequest pending = PENDING.remove(response.requestId());

        if (pending == null) {
            SableFinder.LOGGER.debug("Received response {} for unknown request id {}", response.type().id(), response.requestId());
            return;
        }

        if (!pending.responseType().equals(response.type())) {
            SableFinder.LOGGER.warn("Received response {} but request {} expected {}", response.type().id(), response.requestId(), pending.responseType().id());
            pending.future().completeExceptionally(
                    new IllegalStateException("Unexpected response type " + response.type().id()));
            return;
        }

        ((CompletableFuture<Object>) pending.future()).complete(response);
    }

    public static void cancel(UUID requestId) {
        final PendingRequest pending = PENDING.remove(requestId);
        if (pending != null) {
            pending.future().cancel(true);
        }
    }

    public static void failAll(Throwable cause) {
        for (final UUID requestId : PENDING.keySet()) {
            final PendingRequest pending = PENDING.remove(requestId);
            if (pending != null) {
                pending.future().completeExceptionally(cause);
            }
        }
    }

    private record PendingRequest(CompletableFuture<?> future, CustomPacketPayload.Type<?> responseType) {
    }
}
