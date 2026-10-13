package com.mas6y6.sablefinder.sable;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public class SableContraptionData {
    private final UUID uuid;
    private final Pose3d pose;
    private final BoundingBox3d bounds;
    private final ChunkPos originChunk;
    private final int plotX;
    private final int plotZ;
    private final int logSize;
    private final ResourceKey<Biome> biome;
    private final int dataVersion;
    private final String displayName;
    private final Vector3d linearVelocity;
    private final Vector3d angularVelocity;
    private final List<UUID> dependencies;
    private final CompoundTag userData;
    private final CompoundTag fullTag;
    private final boolean isloaded;
    private final ResourceKey<Level> dimension;

    private static final StreamCodec<ByteBuf, Vector3d> VECTOR_3D = StreamCodec.of(
            (buf, vector) -> {
                buf.writeDouble(vector.x);
                buf.writeDouble(vector.y);
                buf.writeDouble(vector.z);
            },
            buf -> new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    private static final StreamCodec<ByteBuf, Quaterniond> QUATERNION_D = StreamCodec.of(
            (buf, quaternion) -> {
                buf.writeDouble(quaternion.x);
                buf.writeDouble(quaternion.y);
                buf.writeDouble(quaternion.z);
                buf.writeDouble(quaternion.w);
            },
            buf -> new Quaterniond(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    private static final StreamCodec<ByteBuf, Pose3d> POSE_3D = StreamCodec.composite(
            VECTOR_3D, Pose3d::position,
            QUATERNION_D, Pose3d::orientation,
            VECTOR_3D, Pose3d::rotationPoint,
            VECTOR_3D, Pose3d::scale,
            Pose3d::new
    );

    private static final StreamCodec<ByteBuf, BoundingBox3d> BOUNDING_BOX_3D = StreamCodec.of(
            (buf, bounds) -> {
                buf.writeDouble(bounds.minX);
                buf.writeDouble(bounds.minY);
                buf.writeDouble(bounds.minZ);
                buf.writeDouble(bounds.maxX);
                buf.writeDouble(bounds.maxY);
                buf.writeDouble(bounds.maxZ);
            },
            buf -> new BoundingBox3d(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble()
            )
    );

    private static final StreamCodec<ByteBuf, ChunkPos> CHUNK_POS = StreamCodec.of(
            (buf, pos) -> {
                buf.writeInt(pos.x);
                buf.writeInt(pos.z);
            },
            buf -> new ChunkPos(buf.readInt(), buf.readInt())
    );

    private static final StreamCodec<ByteBuf, String> NULLABLE_STRING = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value != null);
                if (value != null) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value);
                }
            },
            buf -> buf.readBoolean() ? ByteBufCodecs.STRING_UTF8.decode(buf) : null
    );

    private static final StreamCodec<ByteBuf, Vector3d> NULLABLE_VECTOR_3D = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value != null);
                if (value != null) {
                    VECTOR_3D.encode(buf, value);
                }
            },
            buf -> buf.readBoolean() ? VECTOR_3D.decode(buf) : null
    );

    private static final StreamCodec<ByteBuf, CompoundTag> NULLABLE_COMPOUND_TAG =
            ByteBufCodecs.OPTIONAL_COMPOUND_TAG.map(optional -> optional.orElse(null), Optional::ofNullable);

    private static final StreamCodec<ByteBuf, List<UUID>> UUID_LIST = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, ResourceKey<Biome>> BIOME_KEY = ResourceKey.streamCodec(Registries.BIOME);

    private static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION_KEY = ResourceKey.streamCodec(Registries.DIMENSION);

    public static final StreamCodec<ByteBuf, SableContraptionData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, data.uuid);
                POSE_3D.encode(buf, data.pose);
                BOUNDING_BOX_3D.encode(buf, data.bounds);
                CHUNK_POS.encode(buf, data.originChunk);
                buf.writeInt(data.plotX);
                buf.writeInt(data.plotZ);
                buf.writeInt(data.logSize);
                BIOME_KEY.encode(buf, data.biome);
                buf.writeInt(data.dataVersion);
                NULLABLE_STRING.encode(buf, data.displayName);
                NULLABLE_VECTOR_3D.encode(buf, data.linearVelocity);
                NULLABLE_VECTOR_3D.encode(buf, data.angularVelocity);
                UUID_LIST.encode(buf, data.dependencies);
                NULLABLE_COMPOUND_TAG.encode(buf, data.userData);
                ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, data.fullTag);
                buf.writeBoolean(data.isloaded);
                DIMENSION_KEY.encode(buf, data.dimension);
            },
            buf -> new SableContraptionData(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    POSE_3D.decode(buf),
                    BOUNDING_BOX_3D.decode(buf),
                    CHUNK_POS.decode(buf),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    BIOME_KEY.decode(buf),
                    buf.readInt(),
                    NULLABLE_STRING.decode(buf),
                    NULLABLE_VECTOR_3D.decode(buf),
                    NULLABLE_VECTOR_3D.decode(buf),
                    UUID_LIST.decode(buf),
                    NULLABLE_COMPOUND_TAG.decode(buf),
                    ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf),
                    buf.readBoolean(),
                    DIMENSION_KEY.decode(buf)
            )
    );

    private final Map<Long, StoredSubLevelChunk> chunks = new HashMap<>();

    private SableContraptionData(final UUID uuid, final Pose3d pose, final BoundingBox3d bounds, final ChunkPos originChunk,
                                 final int plotX, final int plotZ, final int logSize, final ResourceKey<Biome> biome,
                                 final int dataVersion, final String displayName, final Vector3d linearVelocity,
                                 final Vector3d angularVelocity, final List<UUID> dependencies, final CompoundTag userData,
                                 final CompoundTag fullTag, boolean isloaded, final ResourceKey<Level> dimension) {
        this.uuid = uuid;
        this.pose = pose;
        this.bounds = bounds;
        this.originChunk = originChunk;
        this.plotX = plotX;
        this.plotZ = plotZ;
        this.logSize = logSize;
        this.biome = biome;
        this.dataVersion = dataVersion;
        this.displayName = displayName;
        this.linearVelocity = linearVelocity;
        this.angularVelocity = angularVelocity;
        this.dependencies = dependencies;
        this.userData = userData;
        this.fullTag = fullTag;
        this.isloaded = isloaded;
        this.dimension = dimension;
    }

    private static @Nullable Vector3d readOptionalVector3d(final @NotNull CompoundTag tag, final @NotNull String key) {
        if (!tag.contains(key)) {
            return null;
        }
        final CompoundTag vector = tag.getCompound(key);
        return new Vector3d(vector.getDouble("x"), vector.getDouble("y"), vector.getDouble("z"));
    }

    public static @NotNull SableContraptionData parse(final @NotNull SubLevelData data, final @NotNull ServerLevel level, boolean isloaded) {
        final CompoundTag fullTag = data.fullTag();
        final CompoundTag plot = fullTag.getCompound("plot");
        final CompoundTag chunksTag = plot.getCompound("chunks");

        final ResourceKey<Biome> biome;
        if (plot.contains("biome")) {
            final ResourceLocation location = ResourceLocation.tryParse(plot.getString("biome"));
            biome = location != null ? ResourceKey.create(Registries.BIOME, location) : Biomes.PLAINS;
        } else {
            biome = Biomes.PLAINS;
        }

        final SableContraptionData contraption = new SableContraptionData(
                data.uuid(), data.pose(), data.bounds(), data.getOriginLoadedChunk(),
                plot.getInt("plot_x"), plot.getInt("plot_z"), plot.getInt("log_size"), biome,
                plot.getInt("data_version"),
                fullTag.contains("display_name") ? fullTag.getString("display_name") : null,
                readOptionalVector3d(fullTag, "linear_velocity"),
                readOptionalVector3d(fullTag, "angular_velocity"),
                data.dependencies(),
                fullTag.contains("user_data") ? fullTag.getCompound("user_data") : null,
                fullTag,
                isloaded,
                level.dimension()
        );

        for (final String key : chunksTag.getAllKeys()) {
            final long packed = Long.parseLong(key);
            final ChunkPos local = new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed));
            final StoredSubLevelChunk chunk = StoredSubLevelChunk.fromNbt(
                    local, chunksTag.getCompound(key), level, biome
            );
            contraption.chunks.put(packed, chunk);
        }

        return contraption;
    }

    public void ensureChunksLoaded(final Level level) {
        if (!this.chunks.isEmpty() || this.fullTag == null) {
            return;
        }
        if (!this.fullTag.contains("plot")) {
            return;
        }
        final CompoundTag plot = this.fullTag.getCompound("plot");
        if (!plot.contains("chunks")) {
            return;
        }
        final CompoundTag chunksTag = plot.getCompound("chunks");
        for (final String key : chunksTag.getAllKeys()) {
            try {
                final long packed = Long.parseLong(key);
                final ChunkPos local = new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                final StoredSubLevelChunk chunk = StoredSubLevelChunk.fromNbt(
                        local, chunksTag.getCompound(key), level, this.biome != null ? this.biome : Biomes.PLAINS
                );
                this.chunks.put(packed, chunk);
            } catch (final Exception ignored) {
            }
        }
    }

    public UUID uuid() {
        return this.uuid;
    }

    public Pose3d pose() {
        return this.pose;
    }

    public BoundingBox3d bounds() {
        return this.bounds;
    }

    public ChunkPos originChunk() {
        return this.originChunk;
    }

    public int plotX() {
        return this.plotX;
    }

    public int plotZ() {
        return this.plotZ;
    }

    public int logSize() {
        return this.logSize;
    }

    public ResourceKey<Biome> biome() {
        return this.biome;
    }

    public int dataVersion() {
        return this.dataVersion;
    }

    public String displayName() {
        return this.displayName;
    }

    public Vector3d linearVelocity() {
        return this.linearVelocity;
    }

    public Vector3d angularVelocity() {
        return this.angularVelocity;
    }

    public List<UUID> dependencies() {
        return this.dependencies;
    }

    public CompoundTag userData() {
        return this.userData;
    }

    public CompoundTag fullTag() {
        return this.fullTag;
    }

    public boolean isLoaded() {
        return this.isloaded;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public int chunkCount() {
        if (this.chunks.isEmpty()) {
            ensureChunksLoaded(null);
        }
        return this.chunks.size();
    }

    public StoredSubLevelChunk getChunk(final @NotNull ChunkPos localPos) {
        if (this.chunks.isEmpty()) {
            ensureChunksLoaded(null);
        }
        return this.chunks.get(localPos.toLong());
    }

    public @NotNull Collection<StoredSubLevelChunk> getChunks() {
        if (this.chunks.isEmpty()) {
            ensureChunksLoaded(null);
        }
        return this.chunks.values();
    }

    public BlockState getBlockState(final int x, final int y, final int z) {
        if (this.chunks.isEmpty()) {
            ensureChunksLoaded(null);
        }
        final StoredSubLevelChunk chunk = this.chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (chunk == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return chunk.getBlockState(x & 15, y, z & 15);
    }

    public BlockState getBlockState(final @NotNull BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public void forEachBlock(final @NotNull BiConsumer<BlockPos, BlockState> consumer) {
        if (this.chunks.isEmpty()) {
            ensureChunksLoaded(null);
        }
        for (final StoredSubLevelChunk chunk : this.chunks.values()) {
            final int chunkX = chunk.pos().x;
            final int chunkZ = chunk.pos().z;

            for (final Map.Entry<Integer, LevelChunkSection> entry : chunk.sections().entrySet()) {
                final int sectionY = entry.getKey();
                final LevelChunkSection section = entry.getValue();
                if (section.hasOnlyAir()) continue;

                final int minY = sectionY << 4;
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            final BlockState state = section.getBlockState(x, y, z);
                            if (!state.isAir()) {
                                consumer.accept(new BlockPos(chunkX * 16 + x, minY + y, chunkZ * 16 + z), state);
                            }
                        }
                    }
                }
            }
        }
    }
}