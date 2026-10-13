package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum BlockSearchMethod {
    EQUAL(1),
    LESS_THEN_OR_EQUAL(2),
    BETWEEN(3),
    AT_LEAST_OR_EQUAL(4);

    public static final StreamCodec<ByteBuf, BlockSearchMethod> STREAM_CODEC =
            ByteBufCodecs.idMapper(BlockSearchMethod::byId, BlockSearchMethod::getId);

    private final int id;

    BlockSearchMethod(int i) {
        this.id = i;
    }

    public static @NotNull BlockSearchMethod byId(int id) {
        for (BlockSearchMethod value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return EQUAL;
    }

    public int getId() {
        return id;
    }
}
