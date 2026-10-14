package com.mas6y6.sablefinder.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigServer {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SUGGESTION_CACHE_TTL_MS = BUILDER
            .comment(
                    "How long (in milliseconds) the cached list of contraption UUIDs stays fresh.",
                    "This only feeds the argument suggestions for /sablefinder view_uuid.",
                    "Every refresh scans every stored sub-level on disk, so a larger value trades",
                    "stale suggestions for far less IO."
            )
            .translation("sablefinder.configuration.suggestion_cache_ttl_ms")
            .defineInRange("suggestions.cacheTtlMs", 30_000, 0, 3_600_000);

    public static final ModConfigSpec.IntValue SUB_LEVEL_SAVE_MIN_INTERVAL_MS = BUILDER
            .comment(
                    "Minimum time (in milliseconds) between two sub-level flushes.",
                    "A flush walks the live holding chunk map on the server thread, so doing it on",
                    "every refresh can stall the tick. Set to 0 to disable the throttle entirely.",
                    "Does nothing unless 'Flush sub-level data before searching' is enabled."
            )
            .translation("sablefinder.configuration.sub_level_save_min_interval_ms")
            .defineInRange("subLevelSave.minIntervalMs", 5_000, 0, 3_600_000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static long suggestionCacheTtlMs() {
        return clamp(SUGGESTION_CACHE_TTL_MS.get());
    }

    public static long subLevelSaveMinIntervalMs() {
        return clamp(SUB_LEVEL_SAVE_MIN_INTERVAL_MS.get());
    }

    private static long clamp(int value) {
        return Math.max(0L, value);
    }
}