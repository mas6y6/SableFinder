package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public record SearchFilters(
        boolean saveServer,

        boolean useNameSearch,
        String nameSearch,

        boolean useUUIDSearch,
        String uuidSearch,

        boolean useBlockSize,
        BlockSearchMethod blockSearchMethod,
        int blockSize,
        int blockSizeMax,

        boolean useBlockSearch,
        ArrayList<ResourceLocation> blockSearchIDs
) {
    public static final StreamCodec<ByteBuf, SearchFilters> STREAM_CODEC =
            StreamCodec.of(
                    (buf, filters) -> {
                        ByteBufCodecs.BOOL.encode(buf, filters.saveServer());

                        ByteBufCodecs.BOOL.encode(buf, filters.useNameSearch());
                        ByteBufCodecs.STRING_UTF8.encode(buf, filters.nameSearch());

                        ByteBufCodecs.BOOL.encode(buf, filters.useUUIDSearch());
                        ByteBufCodecs.STRING_UTF8.encode(buf, filters.uuidSearch());

                        ByteBufCodecs.BOOL.encode(buf, filters.useBlockSize());
                        BlockSearchMethod.STREAM_CODEC.encode(buf, filters.blockSearchMethod());
                        ByteBufCodecs.VAR_INT.encode(buf, filters.blockSize());
                        ByteBufCodecs.VAR_INT.encode(buf, filters.blockSizeMax());

                        ByteBufCodecs.BOOL.encode(buf, filters.useBlockSearch());
                        ByteBufCodecs.collection(
                                ArrayList<ResourceLocation>::new,
                                ResourceLocation.STREAM_CODEC
                        ).encode(buf, filters.blockSearchIDs());
                    },
                    buf -> new SearchFilters(
                            ByteBufCodecs.BOOL.decode(buf),

                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.STRING_UTF8.decode(buf),

                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.STRING_UTF8.decode(buf),

                            ByteBufCodecs.BOOL.decode(buf),
                            BlockSearchMethod.STREAM_CODEC.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),

                            ByteBufCodecs.BOOL.decode(buf),
                            ByteBufCodecs.collection(
                                    ArrayList<ResourceLocation>::new,
                                    ResourceLocation.STREAM_CODEC
                            ).decode(buf)
                    )
            );
}