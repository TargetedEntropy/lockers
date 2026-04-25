package com.targetedentropy.lockers.common.serialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Minecraft-free NBT facade.
 * <p>
 * {@code common} cannot import {@code net.minecraft.*}, but still needs to
 * express structured tagged data so that {@link LockerDataCodec} can be unit
 * tested without a live game. Each per-version NeoForge module adapts
 * {@code net.minecraft.nbt.CompoundTag} ↔ {@link DataTag} in a small bridge
 * class (see {@code DataTagBridge} in each {@code neoforge-*} module).
 * <p>
 * Only the tag kinds this mod needs are modelled — we deliberately avoid the
 * full NBT type zoo to keep the facade small.
 */
public sealed interface DataTag
        permits DataTag.Compound, DataTag.ListTag, DataTag.StringTag,
                DataTag.IntTag, DataTag.LongTag, DataTag.ByteArrayTag {

    /** Named map of child tags. Immutable by construction. */
    record Compound(Map<String, DataTag> entries) implements DataTag {
        public Compound {
            Objects.requireNonNull(entries, "entries");
            entries = Map.copyOf(entries);
        }

        public static Compound empty() { return new Compound(Map.of()); }

        public boolean has(String key) { return entries.containsKey(key); }

        public Optional<DataTag> get(String key) {
            return Optional.ofNullable(entries.get(key));
        }

        public String getString(String key) {
            DataTag t = entries.get(key);
            if (t instanceof StringTag s) return s.value();
            throw new IllegalStateException("expected string tag at '" + key + "', got " + t);
        }

        public int getInt(String key) {
            DataTag t = entries.get(key);
            if (t instanceof IntTag i) return i.value();
            throw new IllegalStateException("expected int tag at '" + key + "', got " + t);
        }

        public long getLong(String key) {
            DataTag t = entries.get(key);
            if (t instanceof LongTag l) return l.value();
            throw new IllegalStateException("expected long tag at '" + key + "', got " + t);
        }

        public byte[] getByteArray(String key) {
            DataTag t = entries.get(key);
            if (t instanceof ByteArrayTag b) return b.value().clone();
            throw new IllegalStateException("expected byte-array tag at '" + key + "', got " + t);
        }

        public Compound getCompound(String key) {
            DataTag t = entries.get(key);
            if (t instanceof Compound c) return c;
            throw new IllegalStateException("expected compound tag at '" + key + "', got " + t);
        }

        public ListTag getList(String key) {
            DataTag t = entries.get(key);
            if (t instanceof ListTag l) return l;
            throw new IllegalStateException("expected list tag at '" + key + "', got " + t);
        }

        public Builder toBuilder() { return new Builder(new LinkedHashMap<>(entries)); }

        public static Builder builder() { return new Builder(new LinkedHashMap<>()); }

        /** Mutable builder for ergonomic codec writing. */
        public static final class Builder {
            private final Map<String, DataTag> buf;
            private Builder(Map<String, DataTag> buf) { this.buf = buf; }

            public Builder put(String key, DataTag tag) {
                buf.put(Objects.requireNonNull(key), Objects.requireNonNull(tag));
                return this;
            }
            public Builder putString(String key, String value) {
                return put(key, new StringTag(value));
            }
            public Builder putInt(String key, int value) {
                return put(key, new IntTag(value));
            }
            public Builder putLong(String key, long value) {
                return put(key, new LongTag(value));
            }
            public Builder putByteArray(String key, byte[] value) {
                return put(key, new ByteArrayTag(value));
            }
            public Compound build() { return new Compound(buf); }
        }
    }

    /** Ordered list of child tags. */
    record ListTag(List<DataTag> items) implements DataTag {
        public ListTag {
            Objects.requireNonNull(items, "items");
            items = List.copyOf(items);
        }
        public static ListTag empty() { return new ListTag(List.of()); }
        public static ListTag of(List<? extends DataTag> items) {
            return new ListTag(new ArrayList<>(items));
        }
    }

    record StringTag(String value) implements DataTag {
        public StringTag {
            Objects.requireNonNull(value, "value");
        }
    }

    record IntTag(int value) implements DataTag {}

    record LongTag(long value) implements DataTag {}

    record ByteArrayTag(byte[] value) implements DataTag {
        public ByteArrayTag {
            Objects.requireNonNull(value, "value");
            value = value.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ByteArrayTag b && Arrays.equals(value, b.value);
        }

        @Override
        public int hashCode() { return Arrays.hashCode(value); }

        @Override
        public String toString() {
            return "ByteArrayTag[length=" + value.length + "]";
        }
    }
}
