package com.targetedentropy.lockers.neoforge.nbt;

import com.targetedentropy.lockers.common.serialize.DataTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Bidirectional adapter between {@link DataTag} (from {@code common}) and
 * Minecraft's {@code net.minecraft.nbt.*} types. This is the only class in the
 * codebase allowed to know about both NBT worlds — it is deliberately tiny so
 * that version-specific NBT changes (e.g. 1.21.4's {@code HolderLookup.Provider}
 * requirement) stay isolated here.
 */
public final class DataTagBridge {

    private DataTagBridge() {}

    /** DataTag.Compound → Minecraft CompoundTag (recursive). */
    public static CompoundTag toCompoundTag(DataTag.Compound in) {
        CompoundTag out = new CompoundTag();
        for (Map.Entry<String, DataTag> e : in.entries().entrySet()) {
            out.put(e.getKey(), toMcTag(e.getValue()));
        }
        return out;
    }

    /** Minecraft CompoundTag → DataTag.Compound (recursive). */
    public static DataTag.Compound fromCompoundTag(CompoundTag in) {
        Map<String, DataTag> entries = new LinkedHashMap<>();
        // 26.1.2: getAllKeys() → keySet()
        for (String key : in.keySet()) {
            entries.put(key, fromMcTag(in.get(key)));
        }
        return new DataTag.Compound(entries);
    }

    private static Tag toMcTag(DataTag t) {
        if (t instanceof DataTag.Compound c) return toCompoundTag(c);
        if (t instanceof DataTag.ListTag lt) {
            ListTag out = new ListTag();
            for (DataTag item : lt.items()) out.add(toMcTag(item));
            return out;
        }
        if (t instanceof DataTag.StringTag s) return StringTag.valueOf(s.value());
        if (t instanceof DataTag.IntTag i) return IntTag.valueOf(i.value());
        if (t instanceof DataTag.LongTag l) return LongTag.valueOf(l.value());
        if (t instanceof DataTag.ByteArrayTag b) return new ByteArrayTag(b.value());
        throw new IllegalStateException("unknown DataTag: " + t);
    }

    private static DataTag fromMcTag(Tag tag) {
        if (tag instanceof CompoundTag c) return fromCompoundTag(c);
        if (tag instanceof ListTag l) {
            List<DataTag> items = new ArrayList<>(l.size());
            for (Tag child : l) items.add(fromMcTag(child));
            return new DataTag.ListTag(items);
        }
        // 26.1.2: primitive NBT classes are records; auto-generated value()
        // accessor replaces the legacy getAsXxx() methods.
        if (tag instanceof StringTag s) return new DataTag.StringTag(s.value());
        if (tag instanceof IntTag i) return new DataTag.IntTag(i.value());
        if (tag instanceof LongTag l) return new DataTag.LongTag(l.value());
        if (tag instanceof ByteArrayTag b) return new DataTag.ByteArrayTag(b.getAsByteArray());
        throw new IllegalStateException("unsupported NBT tag: "
                + (tag == null ? "null" : tag.getClass().getSimpleName()));
    }
}
