package com.targetedentropy.lockers.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoadoutTest {

    private static final Instant T0 = Instant.parse("2026-04-25T00:00:00Z");

    @Test
    void emptyLoadoutHasNoItems() {
        Loadout lo = Loadout.empty("pvp", T0);
        assertThat(lo.name()).isEqualTo("pvp");
        assertThat(lo.savedAt()).isEqualTo(T0);
        assertThat(lo.equipment()).isEmpty();
        assertThat(lo.accessories()).isEmpty();
    }

    @Test
    void constructorDefensivelyCopiesEquipment() {
        EnumMap<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        byte[] chestBytes = {1, 2, 3};
        eq.put(EquipmentSlot.CHEST, chestBytes);

        Loadout lo = new Loadout("x", T0, eq, Map.of());

        // Mutate source: loadout must not change
        chestBytes[0] = 99;
        eq.put(EquipmentSlot.HEAD, new byte[] {7});

        assertThat(lo.equipment().get(EquipmentSlot.CHEST)).containsExactly(1, 2, 3);
        assertThat(lo.equipment()).doesNotContainKey(EquipmentSlot.HEAD);
    }

    @Test
    void constructorDefensivelyCopiesAccessories() {
        LinkedHashMap<SlotId, byte[]> acc = new LinkedHashMap<>();
        byte[] ringBytes = {4, 5, 6};
        acc.put(SlotId.parse("curios:ring/0"), ringBytes);

        Loadout lo = new Loadout("x", T0, Map.of(), acc);
        ringBytes[0] = 88;
        acc.put(SlotId.parse("curios:ring/1"), new byte[] {9});

        assertThat(lo.accessories().get(SlotId.parse("curios:ring/0")))
                .containsExactly(4, 5, 6);
        assertThat(lo.accessories()).hasSize(1);
    }

    @Test
    void equalsIsValueBasedOnByteArrayContents() {
        EnumMap<EquipmentSlot, byte[]> eq1 = new EnumMap<>(EquipmentSlot.class);
        eq1.put(EquipmentSlot.OFFHAND, new byte[] {1, 2, 3});
        EnumMap<EquipmentSlot, byte[]> eq2 = new EnumMap<>(EquipmentSlot.class);
        eq2.put(EquipmentSlot.OFFHAND, new byte[] {1, 2, 3});  // different array, same content

        Loadout a = new Loadout("a", T0, eq1, Map.of());
        Loadout b = new Loadout("a", T0, eq2, Map.of());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void notEqualWhenEquipmentBytesDiffer() {
        EnumMap<EquipmentSlot, byte[]> eq1 = new EnumMap<>(EquipmentSlot.class);
        eq1.put(EquipmentSlot.HEAD, new byte[] {1});
        EnumMap<EquipmentSlot, byte[]> eq2 = new EnumMap<>(EquipmentSlot.class);
        eq2.put(EquipmentSlot.HEAD, new byte[] {2});

        assertThat(new Loadout("a", T0, eq1, Map.of()))
                .isNotEqualTo(new Loadout("a", T0, eq2, Map.of()));
    }

    @Test
    void notEqualWhenEquipmentSlotSetDiffers() {
        EnumMap<EquipmentSlot, byte[]> eq1 = new EnumMap<>(EquipmentSlot.class);
        eq1.put(EquipmentSlot.HEAD, new byte[] {1});
        EnumMap<EquipmentSlot, byte[]> eq2 = new EnumMap<>(EquipmentSlot.class);
        eq2.put(EquipmentSlot.CHEST, new byte[] {1});

        assertThat(new Loadout("a", T0, eq1, Map.of()))
                .isNotEqualTo(new Loadout("a", T0, eq2, Map.of()));
    }

    @Test
    void notEqualWhenAccessorySetDiffers() {
        Map<SlotId, byte[]> a = Map.of(SlotId.parse("curios:ring/0"), new byte[] {1});
        Map<SlotId, byte[]> b = Map.of(SlotId.parse("curios:ring/1"), new byte[] {1});
        assertThat(new Loadout("a", T0, Map.of(), a))
                .isNotEqualTo(new Loadout("a", T0, Map.of(), b));
    }

    @Test
    void notEqualWhenAccessoryBytesDiffer() {
        Map<SlotId, byte[]> a = Map.of(SlotId.parse("curios:ring/0"), new byte[] {1});
        Map<SlotId, byte[]> b = Map.of(SlotId.parse("curios:ring/0"), new byte[] {2});
        assertThat(new Loadout("a", T0, Map.of(), a))
                .isNotEqualTo(new Loadout("a", T0, Map.of(), b));
    }

    @Test
    void notEqualToNonLoadout() {
        assertThat(Loadout.empty("x", T0)).isNotEqualTo("not a loadout");
    }

    @Test
    void withNameReturnsNewLoadoutAndPreservesOthers() {
        EnumMap<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        eq.put(EquipmentSlot.HEAD, new byte[] {1});
        Loadout a = new Loadout("orig", T0, eq, Map.of());
        Loadout b = a.withName("renamed");

        assertThat(b.name()).isEqualTo("renamed");
        assertThat(b.savedAt()).isEqualTo(a.savedAt());
        // Compare via Loadout.equals which does value-based byte[] comparison,
        // not via raw Map.equals which only does array-identity comparison.
        assertThat(b).isEqualTo(a.withName("renamed"));
        assertThat(a.name()).isEqualTo("orig");
    }

    @Test
    void nameExceedingMaxLengthIsRejected() {
        String tooLong = "x".repeat(Loadout.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> Loadout.empty(tooLong, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void nameAtMaxLengthIsAllowed() {
        String atLimit = "x".repeat(Loadout.MAX_NAME_LENGTH);
        Loadout lo = Loadout.empty(atLimit, T0);
        assertThat(lo.name()).hasSize(Loadout.MAX_NAME_LENGTH);
    }

    @Test
    void constructorRejectsNullName() {
        assertThatThrownBy(() -> new Loadout(null, T0, Map.of(), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullSavedAt() {
        assertThatThrownBy(() -> new Loadout("x", null, Map.of(), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullEquipment() {
        assertThatThrownBy(() -> new Loadout("x", T0, null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullAccessories() {
        assertThatThrownBy(() -> new Loadout("x", T0, Map.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullEquipmentValue() {
        Map<EquipmentSlot, byte[]> bad = new HashMap<>();
        bad.put(EquipmentSlot.HEAD, null);
        assertThatThrownBy(() -> new Loadout("x", T0, bad, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullAccessoryValue() {
        Map<SlotId, byte[]> bad = new HashMap<>();
        bad.put(SlotId.parse("curios:ring/0"), null);
        assertThatThrownBy(() -> new Loadout("x", T0, Map.of(), bad))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sameReferenceEqualsItself() {
        Loadout lo = Loadout.empty("x", T0);
        assertThat(lo).isEqualTo(lo);
    }
}
