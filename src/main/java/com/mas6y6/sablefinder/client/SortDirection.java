package com.mas6y6.sablefinder.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum SortDirection {
    ASCENDING(0),
    DESCENDING(1);

    public static final StreamCodec<ByteBuf, SortDirection> STREAM_CODEC =
            ByteBufCodecs.idMapper(SortDirection::byId, SortDirection::getId);

    private final int id;

    SortDirection(int i) {
        this.id = i;
    }

    public static @NotNull SortDirection byId(int id) {
        for (SortDirection value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return ASCENDING;
    }

    public int getId() {
        return id;
    }

    public @NotNull SortDirection toggled() {
        return this == ASCENDING ? DESCENDING : ASCENDING;
    }

    public @NotNull String translationKey() {
        return this == ASCENDING
                ? "sablefinder.ui.sortdirection.ascending"
                : "sablefinder.ui.sortdirection.descending";
    }
}
