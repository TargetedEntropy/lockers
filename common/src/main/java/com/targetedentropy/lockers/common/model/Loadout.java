package com.targetedentropy.lockers.common.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A single saved loadout inside a Locker.
 * <p>
 * {@code equipment} maps the five {@link EquipmentSlot} entries to an
 * NBT-encoded {@code byte[]} of the item stack; absent entries = empty slot.
 * {@code accessories} is the same concept for Curios/Accessories slots keyed
 * by {@link SlotId}. The record defensively copies its maps on construction.
 */
public record Loadout(
        String name,
        Instant savedAt,
        Map<EquipmentSlot, byte[]> equipment,
        Map<SlotId, byte[]> accessories
) {

    public static final int MAX_NAME_LENGTH = 32;

    public Loadout {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(savedAt, "savedAt");
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(accessories, "accessories");

        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "loadout name exceeds " + MAX_NAME_LENGTH + " chars");
        }

        equipment = copyEquipment(equipment);
        accessories = copyAccessories(accessories);
    }

    /** Empty loadout with the given display name. */
    public static Loadout empty(String name, Instant now) {
        return new Loadout(name, now, Map.of(), Map.of());
    }

    public Loadout withName(String newName) {
        return new Loadout(newName, savedAt, equipment, accessories);
    }

    /** Value-equality comparing byte array contents (records default to identity). */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Loadout that)) return false;
        return name.equals(that.name)
                && savedAt.equals(that.savedAt)
                && equalEquipment(equipment, that.equipment)
                && equalAccessories(accessories, that.accessories);
    }

    @Override
    public int hashCode() {
        int h = Objects.hash(name, savedAt);
        for (Map.Entry<EquipmentSlot, byte[]> e : equipment.entrySet()) {
            h = 31 * h + e.getKey().hashCode() + Arrays.hashCode(e.getValue());
        }
        for (Map.Entry<SlotId, byte[]> e : accessories.entrySet()) {
            h = 31 * h + e.getKey().hashCode() + Arrays.hashCode(e.getValue());
        }
        return h;
    }

    private static Map<EquipmentSlot, byte[]> copyEquipment(Map<EquipmentSlot, byte[]> in) {
        if (in.isEmpty()) return Map.of();
        EnumMap<EquipmentSlot, byte[]> copy = new EnumMap<>(EquipmentSlot.class);
        for (Map.Entry<EquipmentSlot, byte[]> e : in.entrySet()) {
            Objects.requireNonNull(e.getKey(), "equipment key");
            Objects.requireNonNull(e.getValue(), "equipment bytes for " + e.getKey());
            copy.put(e.getKey(), e.getValue().clone());
        }
        return Map.copyOf(copy);
    }

    private static Map<SlotId, byte[]> copyAccessories(Map<SlotId, byte[]> in) {
        if (in.isEmpty()) return Map.of();
        Map<SlotId, byte[]> copy = new LinkedHashMap<>();
        for (Map.Entry<SlotId, byte[]> e : in.entrySet()) {
            Objects.requireNonNull(e.getKey(), "slot id");
            Objects.requireNonNull(e.getValue(), "accessory bytes for " + e.getKey());
            copy.put(e.getKey(), e.getValue().clone());
        }
        return Map.copyOf(copy);
    }

    private static boolean equalEquipment(Map<EquipmentSlot, byte[]> a, Map<EquipmentSlot, byte[]> b) {
        if (a.size() != b.size()) return false;
        for (EquipmentSlot k : EquipmentSlot.values()) {
            byte[] av = a.get(k);
            byte[] bv = b.get(k);
            if ((av == null) != (bv == null)) return false;
            if (av != null && !Arrays.equals(av, bv)) return false;
        }
        return true;
    }

    private static boolean equalAccessories(Map<SlotId, byte[]> a, Map<SlotId, byte[]> b) {
        if (a.size() != b.size()) return false;
        for (Map.Entry<SlotId, byte[]> e : a.entrySet()) {
            byte[] bv = b.get(e.getKey());
            if (bv == null) return false;
            if (!Arrays.equals(e.getValue(), bv)) return false;
        }
        return true;
    }
}
