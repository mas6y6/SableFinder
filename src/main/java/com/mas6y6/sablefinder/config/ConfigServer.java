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

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static long suggestionCacheTtlMs() {
        return clamp(SUGGESTION_CACHE_TTL_MS.get());
    }

    private static long clamp(int value) {
        return Math.max(0L, value);
    }
}