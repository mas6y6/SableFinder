package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


public record BlockFilter(ResourceLocation block, BlockSearchMethod method, int minCount, int maxCount) {
    public static final StreamCodec<ByteBuf, BlockFilter> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BlockFilter::block,
            BlockSearchMethod.STREAM_CODEC, BlockFilter::method,
            ByteBufCodecs.VAR_INT, BlockFilter::minCount,
            ByteBufCodecs.VAR_INT, BlockFilter::maxCount,
            BlockFilter::new
    );

    public static @NotNull BlockFilter of(ResourceLocation block) {
        return new BlockFilter(block, BlockSearchMethod.LESS_THEN_OR_EQUAL, 1, 1);
    }

    public boolean matches(int count) {
        return SearchEngine.matchesCount(method, minCount, maxCount, count);
    }
}