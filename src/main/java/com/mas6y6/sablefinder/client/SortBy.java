package com.mas6y6.sablefinder.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum SortBy {
    LOADED(1),
    HAS_NAME(2),
    IS_LOCKED(3);

    public static final StreamCodec<ByteBuf, SortBy> STREAM_CODEC =
            ByteBufCodecs.idMapper(SortBy::byId, SortBy::getId);

    private final int id;

    SortBy(int i) {
        this.id = i;
    }

    public static @NotNull SortBy byId(int id) {
        for (SortBy value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return LOADED;
    }

    public int getId() {
        return id;
    }

    /**
     * Steps to the next option that is actually usable, wrapping around.
     *
     * <p>IS_LOCKED is only meaningful when Create Aeronautics: Simulated is installed, since that is what
     * populates the locked set; without it every contraption would report unlocked.
     */
    public @NotNull SortBy next(boolean simulatedInstalled) {
        final SortBy[] options = values();

        for (int i = 1; i <= options.length; i++) {
            final SortBy candidate = options[(ordinal() + i) % options.length];
            if (candidate.isAvailable(simulatedInstalled)) {
                return candidate;
            }
        }

        return this;
    }

    public boolean isAvailable(boolean simulatedInstalled) {
        return this != IS_LOCKED || simulatedInstalled;
    }

    public @NotNull String translationKey() {
        return switch (this) {
            case LOADED -> "sablefinder.ui.sortby.loaded";
            case HAS_NAME -> "sablefinder.ui.sortby.has_name";
            case IS_LOCKED -> "sablefinder.ui.sortby.is_locked";
        };
    }
}