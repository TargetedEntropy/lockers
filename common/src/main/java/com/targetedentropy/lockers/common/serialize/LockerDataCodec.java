package com.targetedentropy.lockers.common.serialize;

import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.model.SlotId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link LockerData} ↔ {@link DataTag.Compound} serialization.
 * <p>
 * The tag layout is intentionally explicit so migrations can branch on
 * {@code schemaVersion}. Current layout (v1):
 *
 * <pre>
 * lockerData: Compound {
 *   schemaVersion: int
 *   ownerMsb: long
 *   ownerLsb: long
 *   ownerName: string?     (omitted if unknown)
 *   access: string         (AccessControl enum name)
 *   createdAt: long        (epoch millis)
 *   slots: ListTag[6] of (Compound | emptyMarker)
 * }
 * slotEntry: Compound {
 *   empty: int             (0 = populated, 1 = empty slot marker)
 *   name: string           (only if empty=0)
 *   savedAt: long          (only if empty=0)
 *   equipment: Compound { HEAD: bytes?, CHEST: bytes?, ... }
 *   accessories: ListTag of Compound { slot: string, data: bytes }
 * }
 * </pre>
 */
public final class LockerDataCodec {

    static final String KEY_SCHEMA = "schemaVersion";
    static final String KEY_OWNER_MSB = "ownerMsb";
    static final String KEY_OWNER_LSB = "ownerLsb";
    static final String KEY_OWNER_NAME = "ownerName";
    static final String KEY_ACCESS = "access";
    static final String KEY_CREATED_AT = "createdAt";
    static final String KEY_SLOTS = "slots";

    static final String SLOT_EMPTY = "empty";
    static final String SLOT_NAME = "name";
    static final String SLOT_SAVED_AT = "savedAt";
    static final String SLOT_EQUIPMENT = "equipment";
    static final String SLOT_ACCESSORIES = "accessories";
    static final String ACCESSORY_KEY = "slot";
    static final String ACCESSORY_DATA = "data";

    private LockerDataCodec() {}

    public static DataTag.Compound encode(LockerData data) {
        DataTag.Compound.Builder out = DataTag.Compound.builder()
                .putInt(KEY_SCHEMA, data.schemaVersion())
                .putLong(KEY_OWNER_MSB, data.owner().getMostSignificantBits())
                .putLong(KEY_OWNER_LSB, data.owner().getLeastSignificantBits())
                .putString(KEY_ACCESS, data.access().name())
                .putLong(KEY_CREATED_AT, data.createdAt().toEpochMilli());

        data.ownerNameCached().ifPresent(n -> out.putString(KEY_OWNER_NAME, n));

        List<DataTag> slotTags = new ArrayList<>(LockerData.SLOT_COUNT);
        for (Optional<Loadout> slot : data.slots()) {
            slotTags.add(encodeSlot(slot));
        }
        out.put(KEY_SLOTS, new DataTag.ListTag(slotTags));

        return out.build();
    }

    public static LockerData decode(DataTag.Compound tag) {
        int schemaVersion = tag.getInt(KEY_SCHEMA);
        if (schemaVersion != LockerData.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "unsupported locker schemaVersion=" + schemaVersion
                    + " (codec supports " + LockerData.CURRENT_SCHEMA_VERSION + ")");
        }

        UUID owner = new UUID(tag.getLong(KEY_OWNER_MSB), tag.getLong(KEY_OWNER_LSB));
        Optional<String> ownerName = tag.has(KEY_OWNER_NAME)
                ? Optional.of(tag.getString(KEY_OWNER_NAME))
                : Optional.empty();
        AccessControl access = parseAccess(tag.getString(KEY_ACCESS));
        Instant createdAt = Instant.ofEpochMilli(tag.getLong(KEY_CREATED_AT));

        DataTag.ListTag slotsList = tag.getList(KEY_SLOTS);
        if (slotsList.items().size() != LockerData.SLOT_COUNT) {
            throw new IllegalStateException(
                    "expected " + LockerData.SLOT_COUNT + " slot entries, got "
                    + slotsList.items().size());
        }

        List<Optional<Loadout>> slots = new ArrayList<>(LockerData.SLOT_COUNT);
        for (DataTag item : slotsList.items()) {
            if (!(item instanceof DataTag.Compound slotTag)) {
                throw new IllegalStateException("slot entry must be compound, got " + item);
            }
            slots.add(decodeSlot(slotTag));
        }

        return new LockerData(schemaVersion, owner, ownerName, access, slots, createdAt);
    }

    private static DataTag encodeSlot(Optional<Loadout> slot) {
        if (slot.isEmpty()) {
            return DataTag.Compound.builder().putInt(SLOT_EMPTY, 1).build();
        }
        Loadout lo = slot.get();
        DataTag.Compound.Builder b = DataTag.Compound.builder()
                .putInt(SLOT_EMPTY, 0)
                .putString(SLOT_NAME, lo.name())
                .putLong(SLOT_SAVED_AT, lo.savedAt().toEpochMilli());

        DataTag.Compound.Builder eq = DataTag.Compound.builder();
        for (Map.Entry<EquipmentSlot, byte[]> e : lo.equipment().entrySet()) {
            eq.putByteArray(e.getKey().name(), e.getValue());
        }
        b.put(SLOT_EQUIPMENT, eq.build());

        List<DataTag> acc = new ArrayList<>();
        for (Map.Entry<SlotId, byte[]> e : lo.accessories().entrySet()) {
            acc.add(DataTag.Compound.builder()
                    .putString(ACCESSORY_KEY, e.getKey().asString())
                    .putByteArray(ACCESSORY_DATA, e.getValue())
                    .build());
        }
        b.put(SLOT_ACCESSORIES, new DataTag.ListTag(acc));

        return b.build();
    }

    private static Optional<Loadout> decodeSlot(DataTag.Compound slotTag) {
        if (slotTag.getInt(SLOT_EMPTY) == 1) {
            return Optional.empty();
        }
        String name = slotTag.getString(SLOT_NAME);
        Instant savedAt = Instant.ofEpochMilli(slotTag.getLong(SLOT_SAVED_AT));

        Map<EquipmentSlot, byte[]> equipment = new EnumMap<>(EquipmentSlot.class);
        DataTag.Compound eq = slotTag.getCompound(SLOT_EQUIPMENT);
        for (String key : eq.entries().keySet()) {
            equipment.put(EquipmentSlot.valueOf(key), eq.getByteArray(key));
        }

        Map<SlotId, byte[]> accessories = new LinkedHashMap<>();
        for (DataTag accItem : slotTag.getList(SLOT_ACCESSORIES).items()) {
            if (!(accItem instanceof DataTag.Compound ac)) {
                throw new IllegalStateException("accessory entry must be compound");
            }
            accessories.put(SlotId.parse(ac.getString(ACCESSORY_KEY)), ac.getByteArray(ACCESSORY_DATA));
        }

        return Optional.of(new Loadout(name, savedAt, equipment, accessories));
    }

    private static AccessControl parseAccess(String name) {
        try {
            return AccessControl.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown AccessControl: " + name, e);
        }
    }
}
