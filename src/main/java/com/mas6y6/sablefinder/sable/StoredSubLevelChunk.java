package com.mas6y6.sablefinder.sable;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.HashMap;
import java.util.Map;

public class StoredSubLevelChunk {
    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY, BlockState.CODEC,
            PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState()
    );

    public record SectionLightData(byte[] blockLight, byte[] skyLight) {}

    private final ChunkPos pos;
    private final Map<Integer, LevelChunkSection> sections = new HashMap<>();
    private final Map<Integer, SectionLightData> sectionLightData = new HashMap<>();
    private final boolean isLightOn;
    private final ListTag blockEntities;
    private final ListTag blockTicks;
    private final ListTag fluidTicks;
    private final CompoundTag heightmaps;
    private final CompoundTag tag;

    private StoredSubLevelChunk(final ChunkPos pos, final boolean isLightOn, final ListTag blockEntities,
                                final ListTag blockTicks, final ListTag fluidTicks, final CompoundTag heightmaps,
                                final CompoundTag tag) {
        this.pos = pos;
        this.isLightOn = isLightOn;
        this.blockEntities = blockEntities;
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
        this.heightmaps = heightmaps;
        this.tag = tag;
    }

    public static StoredSubLevelChunk fromNbt(final ChunkPos pos, final CompoundTag chunkTag, final ServerLevel level, final ResourceKey<Biome> biome) {
        final boolean isLightOn = chunkTag.getBoolean("isLightOn");
        final ListTag blockEntities = chunkTag.getList("block_entities", Tag.TAG_COMPOUND);
        final ListTag blockTicks = chunkTag.getList("block_ticks", Tag.TAG_COMPOUND);
        final ListTag fluidTicks = chunkTag.getList("fluid_ticks", Tag.TAG_COMPOUND);
        final CompoundTag heightmaps = chunkTag.getCompound("heightmaps");

        final StoredSubLevelChunk chunk = new StoredSubLevelChunk(
                pos, isLightOn, blockEntities, blockTicks, fluidTicks, heightmaps, chunkTag
        );

        final CompoundTag sectionsTag = chunkTag.getCompound("sections");
        final Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        final PalettedContainer<Holder<Biome>> biomeContainer = new PalettedContainer<>(
                biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(biome), PalettedContainer.Strategy.SECTION_BIOMES
        );

        for (final String sectionKey : sectionsTag.getAllKeys()) {
            final int index = Integer.parseInt(sectionKey);
            final CompoundTag sectionTag = sectionsTag.getCompound(sectionKey);
            final CompoundTag blockStatesTag = sectionTag.getCompound("block_states");

            final byte[] blockLight = sectionTag.contains("BlockLight", Tag.TAG_BYTE_ARRAY) ? sectionTag.getByteArray("BlockLight") : null;
            final byte[] skyLight = sectionTag.contains("SkyLight", Tag.TAG_BYTE_ARRAY) ? sectionTag.getByteArray("SkyLight") : null;
            chunk.sectionLightData.put(index, new SectionLightData(blockLight, skyLight));

            final PalettedContainer<BlockState> states = BLOCK_STATE_CODEC
                    .parse(NbtOps.INSTANCE, blockStatesTag)
                    .getOrThrow();

            final int sectionY = level.getSectionYFromSectionIndex(index);
            chunk.sections.put(sectionY, new LevelChunkSection(states, biomeContainer));
        }

        return chunk;
    }

    public ChunkPos pos() {
        return this.pos;
    }

    public boolean isLightOn() {
        return this.isLightOn;
    }

    public ListTag blockEntities() {
        return this.blockEntities;
    }

    public ListTag blockTicks() {
        return this.blockTicks;
    }

    public ListTag fluidTicks() {
        return this.fluidTicks;
    }

    public CompoundTag heightmaps() {
        return this.heightmaps;
    }

    public CompoundTag tag() {
        return this.tag;
    }

    public SectionLightData getLightData(final int sectionIndex) {
        return this.sectionLightData.get(sectionIndex);
    }

    public Map<Integer, LevelChunkSection> sections() {
        return this.sections;
    }

    public LevelChunkSection getSection(final int sectionY) {
        return this.sections.get(sectionY);
    }

    public BlockState getBlockState(final int localX, final int localY, final int localZ) {
        final LevelChunkSection section = this.sections.get(Math.floorDiv(localY, 16));
        if (section == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return section.getBlockState(localX & 15, localY & 15, localZ & 15);
    }
}