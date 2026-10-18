package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public record SearchFilters(
        TextSearchMethod textMethod,

        boolean useNameSearch,
        String nameSearch,

        boolean useUUIDSearch,
        String uuidSearch,

        boolean useBlockSize,
        BlockSearchMethod blockSearchMethod,
        int blockSize,
        int blockSizeMax,

        boolean useBlockSearch,
        ArrayList<BlockFilter> blockFilters
) {
    public static final SearchFilters DEFAULT = new SearchFilters();

    public static final StreamCodec<ByteBuf, SearchFilters> STREAM_CODEC =
            StreamCodec.of(
                    (buf, filters) -> {
                        TextSearchMethod.STREAM_CODEC.encode(buf, filters.textMethod());

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
                                ArrayList::new,
                                BlockFilter.STREAM_CODEC
                        ).encode(buf, filters.blockFilters());
                    },
                    buf -> new SearchFilters(
                            TextSearchMethod.STREAM_CODEC.decode(buf),

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
                                    ArrayList::new,
                                    BlockFilter.STREAM_CODEC
                            ).decode(buf)
                    )
            );

    private SearchFilters() {
        this(
                TextSearchMethod.CONTAINS,
                false, "",
                false, "",
                false, BlockSearchMethod.LESS_THEN_OR_EQUAL, 0, 0,
                false, new ArrayList<>()
        );
    }

    public @NotNull SearchFilters withTextMethod(TextSearchMethod textMethod) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withUseNameSearch(boolean useNameSearch) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withNameSearch(String nameSearch) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withUseUUIDSearch(boolean useUUIDSearch) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withUuidSearch(String uuidSearch) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withUseBlockSize(boolean useBlockSize) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withBlockSearchMethod(BlockSearchMethod blockSearchMethod) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withBlockSize(int blockSize) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withBlockSizeMax(int blockSizeMax) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withUseBlockSearch(boolean useBlockSearch) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

    public @NotNull SearchFilters withBlockFilters(ArrayList<BlockFilter> blockFilters) {
        return new SearchFilters(
                textMethod,
                useNameSearch, nameSearch,
                useUUIDSearch, uuidSearch,
                useBlockSize, blockSearchMethod, blockSize, blockSizeMax,
                useBlockSearch, blockFilters
        );
    }

}