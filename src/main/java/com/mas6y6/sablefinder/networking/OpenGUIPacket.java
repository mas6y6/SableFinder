package com.mas6y6.sablefinder.networking;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.client.ui.SableFinderScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenGUIPacket(CompoundTag sableContraptions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SableFinder.MODID, "open_gui"));


    public static final StreamCodec<ByteBuf, OpenGUIPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG,
                OpenGUIPacket::sableContraptions,
                OpenGUIPacket::new
            );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(OpenGUIPacket customPacketPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new SableFinderScreen(customPacketPayload.sableContraptions()));
        });
    }
}
