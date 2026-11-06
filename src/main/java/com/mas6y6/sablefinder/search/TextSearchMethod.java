package com.mas6y6.sablefinder.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public enum TextSearchMethod {
    CONTAINS(1),
    STARTS_WITH(2),
    ENDS_WITH(3),
    EQUALS(4),
    CONTAINS_ALL_WORDS(5);

    public static final StreamCodec<ByteBuf, TextSearchMethod> STREAM_CODEC =
            ByteBufCodecs.idMapper(TextSearchMethod::byId, TextSearchMethod::getId);

    private final int id;

    TextSearchMethod(int i) {
        this.id = i;
    }

    public static @NotNull TextSearchMethod byId(int id) {
        for (TextSearchMethod value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return CONTAINS;
    }

    public int getId() {
        return id;
    }

    public @NotNull TextSearchMethod next() {
        final TextSearchMethod[] methods = values();
        return methods[(ordinal() + 1) % methods.length];
    }

    public @NotNull String translationKey() {
        return switch (this) {
            case CONTAINS -> "sablefinder.ui.search.text_method.contains";
            case STARTS_WITH -> "sablefinder.ui.search.text_method.starts_with";
            case ENDS_WITH -> "sablefinder.ui.search.text_method.ends_with";
            case EQUALS -> "sablefinder.ui.search.text_method.equals";
            case CONTAINS_ALL_WORDS -> "sablefinder.ui.search.text_method.contains_all_words";
        };
    }


    public boolean matches(@Nullable String query, @NotNull String candidate) {
        if (query == null) {
            return true;
        }

        final String needle = query.trim().toLowerCase(Locale.ROOT);
        final String haystack = candidate.toLowerCase(Locale.ROOT);

        return switch (this) {
            case CONTAINS -> haystack.contains(needle);
            case STARTS_WITH -> haystack.startsWith(needle);
            case ENDS_WITH -> haystack.endsWith(needle);
            case EQUALS -> haystack.equals(needle);
            case CONTAINS_ALL_WORDS -> containsAllWords(needle, haystack);
        };
    }

    private static boolean containsAllWords(@NotNull String needle, @NotNull String haystack) {
        for (final String word : needle.split("\\s+")) {
            if (!word.isEmpty() && !haystack.contains(word)) {
                return false;
            }
        }

        return true;
    }
}