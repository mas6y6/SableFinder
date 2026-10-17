package com.mas6y6.sablefinder.networking;

import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public class Networking {
    public static void registerPayloadHandlers(@NotNull PayloadRegistrar payloadregister) {
        payloadregister.playBidirectional(
                CheckExistencePacket.TYPE,
                CheckExistencePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        CheckExistencePacket::handleClient,
                        CheckExistencePacket::handleServer
                )
        );

        payloadregister.playToClient(
                OpenGUIPacket.TYPE,
                OpenGUIPacket.STREAM_CODEC,
                OpenGUIPacket::handleClient
        );

        payloadregister.playToClient(
                SableContraptionResponsePacket.TYPE,
                SableContraptionResponsePacket.STREAM_CODEC,
                SableContraptionResponsePacket::handleClient
        );

        payloadregister.playToServer(
                SableContraptionRequestPacket.TYPE,
                SableContraptionRequestPacket.STREAM_CODEC,
                SableContraptionRequestPacket::handleServer
        );

        payloadregister.playToClient(
                SableContraptionListResponsePacket.TYPE,
                SableContraptionListResponsePacket.STREAM_CODEC,
                SableContraptionListResponsePacket::handleClient
        );

        payloadregister.playToServer(
                SableContraptionListRequestPacket.TYPE,
                SableContraptionListRequestPacket.STREAM_CODEC,
                SableContraptionListRequestPacket::handleServer
        );

        payloadregister.playToServer(
                DeleteContraptionPacket.TYPE,
                DeleteContraptionPacket.STREAM_CODEC,
                DeleteContraptionPacket::handleServer
        );

        payloadregister.playToClient(
                ClientToastMessagePacket.TYPE,
                ClientToastMessagePacket.STREAM_CODEC,
                ClientToastMessagePacket::handleClient
        );

        payloadregister.playToServer(
                SearchForContraptionsRequestPacket.TYPE,
                SearchForContraptionsRequestPacket.STREAM_CODEC,
                SearchForContraptionsRequestPacket::handleServer
        );

        payloadregister.playToClient(
                SearchForContraptionsResponsePacket.TYPE,
                SearchForContraptionsResponsePacket.STREAM_CODEC,
                SearchForContraptionsResponsePacket::handleClient
        );

        payloadregister.playToServer(
                CancelSearchPacket.TYPE,
                CancelSearchPacket.STREAM_CODEC,
                CancelSearchPacket::handleServer
        );

        payloadregister.playToServer(
                SetContraptionLockPacket.TYPE,
                SetContraptionLockPacket.STREAM_CODEC,
                SetContraptionLockPacket::handleServer
        );
    }
}
