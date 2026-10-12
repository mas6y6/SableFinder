package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record BlockFilter(ResourceLocation block, int minCount) {
    public static final StreamCodec<ByteBuf, BlockFilter> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BlockFilter::block,
            ByteBufCodecs.VAR_INT, BlockFilter::minCount,
            BlockFilter::new
    );

    public static BlockFilter of(ResourceLocation block) {
        return new BlockFilter(block, 1);
    }
}
