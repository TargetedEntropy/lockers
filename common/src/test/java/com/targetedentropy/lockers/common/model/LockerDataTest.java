package com.targetedentropy.lockers.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LockerDataTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000abcd");
    private static final Instant T0 = Instant.parse("2026-04-25T00:00:00Z");

    @Test
    void freshBuildsAllEmptySlotsAndDefaultAccess() {
        LockerData data = LockerData.fresh(OWNER, "alice", T0);
        assertThat(data.owner()).isEqualTo(OWNER);
        assertThat(data.ownerNameCached()).contains("alice");
        assertThat(data.access()).isEqualTo(AccessControl.OWNER_ONLY);
        assertThat(data.schemaVersion()).isEqualTo(LockerData.CURRENT_SCHEMA_VERSION);
        assertThat(data.slots()).hasSize(LockerData.SLOT_COUNT);
        assertThat(data.slots()).allMatch(Optional::isEmpty);
        assertThat(data.createdAt()).isEqualTo(T0);
    }

    @Test
    void freshAllowsNullOwnerName() {
        LockerData data = LockerData.fresh(OWNER, null, T0);
        assertThat(data.ownerNameCached()).isEmpty();
    }

    @Test
    void withSlotReplacesSpecifiedSlotImmutably() {
        LockerData a = LockerData.fresh(OWNER, "alice", T0);
        Loadout lo = Loadout.empty("pvp", T0);
        LockerData b = a.withSlot(2, lo);

        assertThat(b.slot(2)).contains(lo);
        assertThat(b.slot(0)).isEmpty();
        assertThat(a.slot(2)).isEmpty(); // original unchanged
    }

    @Test
    void withSlotPermitsNullToClearSlot() {
        LockerData a = LockerData.fresh(OWNER, null, T0)
                .withSlot(3, Loadout.empty("x", T0));
        LockerData b = a.withSlot(3, null);
        assertThat(b.slot(3)).isEmpty();
    }

    @Test
    void clearSlotRemovesLoadout() {
        LockerData a = LockerData.fresh(OWNER, null, T0)
                .withSlot(5, Loadout.empty("x", T0));
        LockerData b = a.clearSlot(5);
        assertThat(b.slot(5)).isEmpty();
    }

    @Test
    void withAccessChangesAccessControl() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        LockerData b = a.withAccess(AccessControl.PUBLIC);
        assertThat(b.access()).isEqualTo(AccessControl.PUBLIC);
        assertThat(a.access()).isEqualTo(AccessControl.OWNER_ONLY);
    }

    @Test
    void withSlotRejectsOutOfRangeIndex() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        assertThatThrownBy(() -> a.withSlot(-1, null))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> a.withSlot(LockerData.SLOT_COUNT, null))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void clearSlotRejectsOutOfRangeIndex() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        assertThatThrownBy(() -> a.clearSlot(99))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void slotAccessorRejectsOutOfRangeIndex() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        assertThatThrownBy(() -> a.slot(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void constructorRejectsWrongSlotCount() {
        List<Optional<Loadout>> tooFew = new ArrayList<>();
        for (int i = 0; i < 5; i++) tooFew.add(Optional.empty());
        assertThatThrownBy(() -> new LockerData(
                1, OWNER, Optional.empty(), AccessControl.OWNER_ONLY, tooFew, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void constructorRejectsSchemaVersionBelowOne() {
        List<Optional<Loadout>> ok = emptySlots();
        assertThatThrownBy(() -> new LockerData(
                0, OWNER, Optional.empty(), AccessControl.OWNER_ONLY, ok, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaVersion");
    }

    @Test
    void constructorRejectsNullFields() {
        List<Optional<Loadout>> ok = emptySlots();
        assertThatThrownBy(() -> new LockerData(1, null, Optional.empty(), AccessControl.OWNER_ONLY, ok, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LockerData(1, OWNER, null, AccessControl.OWNER_ONLY, ok, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LockerData(1, OWNER, Optional.empty(), null, ok, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LockerData(1, OWNER, Optional.empty(), AccessControl.OWNER_ONLY, null, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LockerData(1, OWNER, Optional.empty(), AccessControl.OWNER_ONLY, ok, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullSlotEntry() {
        List<Optional<Loadout>> bad = new ArrayList<>();
        for (int i = 0; i < LockerData.SLOT_COUNT; i++) bad.add(Optional.empty());
        bad.set(0, null);
        assertThatThrownBy(() -> new LockerData(
                1, OWNER, Optional.empty(), AccessControl.OWNER_ONLY, bad, T0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void slotsListIsImmutable() {
        LockerData a = LockerData.fresh(OWNER, null, T0);
        assertThatThrownBy(() -> a.slots().add(Optional.empty()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static List<Optional<Loadout>> emptySlots() {
        List<Optional<Loadout>> s = new ArrayList<>();
        for (int i = 0; i < LockerData.SLOT_COUNT; i++) s.add(Optional.empty());
        return Collections.unmodifiableList(s);
    }
}
