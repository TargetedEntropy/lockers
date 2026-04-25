package com.targetedentropy.lockers.common.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.model.SlotId;
import com.targetedentropy.lockers.common.serialize.DataTag.Compound;
import com.targetedentropy.lockers.common.serialize.DataTag.IntTag;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LockerDataCodecTest {

    private static final UUID OWNER = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final Instant T0 = Instant.parse("2026-04-25T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-04-25T01:00:00Z");

    @Test
    void emptyLockerRoundtrips() {
        LockerData a = LockerData.fresh(OWNER, "alice", T0);
        DataTag.Compound tag = LockerDataCodec.encode(a);
        LockerData b = LockerDataCodec.decode(tag);
        assertThat(b).isEqualTo(a);
    }

    @Test
    void ownerWithoutNameRoundtrips() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        LockerData b = LockerDataCodec.decode(LockerDataCodec.encode(a));
        assertThat(b.ownerNameCached()).isEmpty();
        assertThat(b.owner()).isEqualTo(OWNER);
    }

    @Test
    void populatedLoadoutsRoundtrip() {
        EnumMap<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        eq.put(EquipmentSlot.CHEST, new byte[] {1, 2, 3});
        eq.put(EquipmentSlot.OFFHAND, new byte[] {7, 8});

        Map<SlotId, byte[]> acc = new LinkedHashMap<>();
        acc.put(SlotId.parse("curios:ring/0"), new byte[] {10});
        acc.put(SlotId.parse("accessories:finger/1"), new byte[] {11, 12});

        Loadout lo = new Loadout("pvp", T1, eq, acc);
        LockerData a = LockerData.fresh(OWNER, "alice", T0)
                .withSlot(0, lo)
                .withSlot(3, Loadout.empty("peace", T0));

        LockerData b = LockerDataCodec.decode(LockerDataCodec.encode(a));

        assertThat(b).isEqualTo(a);
        assertThat(b.slot(0)).contains(lo);
        assertThat(b.slot(3)).contains(Loadout.empty("peace", T0));
        assertThat(b.slot(1)).isEmpty();
    }

    @Test
    void accessControlIsPreserved() {
        LockerData a = LockerData.fresh(OWNER, null, T0).withAccess(AccessControl.PUBLIC);
        LockerData b = LockerDataCodec.decode(LockerDataCodec.encode(a));
        assertThat(b.access()).isEqualTo(AccessControl.PUBLIC);
    }

    @Test
    void teamAccessRoundtrips() {
        LockerData a = LockerData.fresh(OWNER, null, T0).withAccess(AccessControl.TEAM);
        LockerData b = LockerDataCodec.decode(LockerDataCodec.encode(a));
        assertThat(b.access()).isEqualTo(AccessControl.TEAM);
    }

    @Test
    void decodeRejectsUnsupportedSchemaVersion() {
        Compound tag = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0));
        Compound modified = tag.toBuilder().putInt("schemaVersion", 999).build();
        assertThatThrownBy(() -> LockerDataCodec.decode(modified))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schemaVersion");
    }

    @Test
    void decodeRejectsWrongSlotCount() {
        Compound original = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0));
        DataTag.ListTag shortSlots = new DataTag.ListTag(
                original.getList("slots").items().subList(0, 2));
        Compound bad = original.toBuilder().put("slots", shortSlots).build();
        assertThatThrownBy(() -> LockerDataCodec.decode(bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("slot entries");
    }

    @Test
    void decodeRejectsNonCompoundSlotEntry() {
        Compound original = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0));
        // Replace first slot with a raw IntTag instead of a compound.
        List<DataTag> items = new java.util.ArrayList<>(original.getList("slots").items());
        items.set(0, new IntTag(7));
        Compound bad = original.toBuilder()
                .put("slots", new DataTag.ListTag(items))
                .build();
        assertThatThrownBy(() -> LockerDataCodec.decode(bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("compound");
    }

    @Test
    void decodeRejectsUnknownAccessControlString() {
        Compound tag = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0));
        Compound bad = tag.toBuilder().putString("access", "NOT_A_REAL_ACCESS").build();
        assertThatThrownBy(() -> LockerDataCodec.decode(bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AccessControl");
    }

    @Test
    void decodeRejectsNonCompoundAccessoryEntry() {
        EnumMap<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        Loadout lo = new Loadout("x", T0, eq, Map.of(SlotId.parse("curios:ring/0"), new byte[]{1}));
        Compound original = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0).withSlot(0, lo));

        // Surgery: grab slot 0 compound, replace its accessories ListTag with a bad entry.
        DataTag.ListTag slots = original.getList("slots");
        Compound slot0 = (Compound) slots.items().get(0);
        Compound badSlot0 = slot0.toBuilder()
                .put("accessories", new DataTag.ListTag(List.of(new IntTag(42))))
                .build();
        List<DataTag> patched = new java.util.ArrayList<>(slots.items());
        patched.set(0, badSlot0);
        Compound bad = original.toBuilder()
                .put("slots", new DataTag.ListTag(patched))
                .build();

        assertThatThrownBy(() -> LockerDataCodec.decode(bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accessory");
    }

    @Test
    void encodedTagContainsExpectedKeys() {
        Compound tag = LockerDataCodec.encode(LockerData.fresh(OWNER, "alice", T0));
        assertThat(tag.entries()).containsKeys(
                "schemaVersion", "ownerMsb", "ownerLsb", "ownerName",
                "access", "createdAt", "slots");
        assertThat(tag.getInt("schemaVersion")).isEqualTo(LockerData.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void encodedLackOfOwnerNameOmitsKey() {
        Compound tag = LockerDataCodec.encode(LockerData.fresh(OWNER, null, T0));
        assertThat(tag.has("ownerName")).isFalse();
    }

    @Test
    void uuidPreservedByteForByte() {
        UUID u = UUID.fromString("aabbccdd-eeff-0011-2233-445566778899");
        LockerData a = LockerData.fresh(u, null, T0);
        LockerData b = LockerDataCodec.decode(LockerDataCodec.encode(a));
        assertThat(b.owner()).isEqualTo(u);
    }
}
