package com.mas6y6.sablefinder.utils;

import net.minecraft.nbt.*;

public final class TagFormatter {

    private static final String INDENT = "\t";

    private TagFormatter() {}

    public static String format(CompoundTag tag) {
        return formatTag(tag, 0);
    }

    private static String formatTag(Tag tag, int depth) {
        if (tag instanceof CompoundTag compound) {
            if (compound.isEmpty()) {
                return "{}";
            }

            StringBuilder builder = new StringBuilder("{\n");

            for (String key : compound.getAllKeys()) {
                builder.append(indent(depth + 1))
                        .append(key)
                        .append(": ")
                        .append(formatTag(compound.get(key), depth + 1))
                        .append(",\n");
            }

            // Remove the final comma and newline.
            builder.setLength(builder.length() - 2);

            builder.append("\n")
                    .append(indent(depth))
                    .append("}");

            return builder.toString();
        }

        if (tag instanceof ListTag list) {
            if (list.isEmpty()) {
                return "[]";
            }

            StringBuilder builder = new StringBuilder("[\n");

            for (Tag element : list) {
                builder.append(indent(depth + 1))
                        .append(formatTag(element, depth + 1))
                        .append(",\n");
            }

            builder.setLength(builder.length() - 2);

            builder.append("\n")
                    .append(indent(depth))
                    .append("]");

            return builder.toString();
        }

        // Numbers, strings, byte/int/long arrays, and other leaf tags.
        return tag.toString();
    }

    private static String indent(int depth) {
        return INDENT.repeat(depth);
    }
}
