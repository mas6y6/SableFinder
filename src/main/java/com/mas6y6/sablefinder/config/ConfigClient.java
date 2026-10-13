package com.mas6y6.sablefinder.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigClient {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REFRESH_ON_OPEN = BUILDER
            .comment(
                    "Ask the server for a fresh contraption list every time the browser is opened.",
                    "When false the browser reopens instantly from the list cached last time, which may be out of date."
            )
            .translation("sablefinder.configuration.refresh_on_open")
            .define("screen.refreshOnOpen", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean refreshOnOpen() {
        return REFRESH_ON_OPEN.get();
    }
}