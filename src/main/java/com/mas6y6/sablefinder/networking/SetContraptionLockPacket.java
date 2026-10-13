package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.mixininterface.ISFPhysicsStaffServerHandler;
import com.mas6y6.sablefinder.sable.SableUtils;
import com.mas6y6.sablefinder.simulated.SimulatedCompat;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SetContraptionLockPacket(UUID requestId, UUID contraptionId, boolean locked)
        implements RequestPayload<GetSableContraptionPacket> {

    public static final CustomPacketPayload.Type<SetContraptionLockPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "set_contraption_lock"));

    public static final StreamCodec<ByteBuf, SetContraptionLockPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    SetContraptionLockPacket::requestId,
                    UUIDUtil.STREAM_CODEC,
                    SetContraptionLockPacket::contraptionId,
                    ByteBufCodecs.BOOL,
                    SetContraptionLockPacket::locked,
                    SetContraptionLockPacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<GetSableContraptionPacket> responseType() {
        return GetSableContraptionPacket.TYPE;
    }

    public static void handleServer(@NotNull SetContraptionLockPacket packet, @NotNull IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            if (!(iPayloadContext.player() instanceof ServerPlayer player)) {
                return;
            }

            final MinecraftServer server = player.serverLevel().getServer();

            if (!SimulatedCompat.isSimulatedInstalled()) {
                SableFinder.LOGGER.warn("Cannot {} contraption {}: Simulated is not installed on this server",
                        packet.locked() ? "lock" : "unlock", packet.contraptionId());
            } else {
                final ISFPhysicsStaffServerHandler handler = SimulatedCompat.getInstance()
                        .orElseThrow()
                        .getPhysicsStaffServerHandler(server);

                if (handler == null) {
                    SableFinder.LOGGER.warn("Cannot {} contraption {}: no physics staff handler for this server",
                            packet.locked() ? "lock" : "unlock", packet.contraptionId());
                } else if (packet.locked()) {
                    handler.lockContraption(packet.contraptionId());
                } else {
                    handler.unlockContraption(packet.contraptionId());
                }
            }

            SableUtils.getContraptionAsync(server, packet.contraptionId(), false).whenComplete((contraption, error) -> {
                if (error != null) {
                    SableFinder.LOGGER.error("An error occurred while loading contraption {} after a lock change for player {}",
                            packet.contraptionId(), player.getName().getString(), error);
                    return;
                }

                if (contraption != null && server.isRunning()) {
                    server.execute(() -> iPayloadContext.reply(new GetSableContraptionPacket(packet.requestId(), contraption)));
                }
            });
        });
    }
}