package com.targetedentropy.lockers.common.loadout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.loadout.LoadoutOutcome.Feedback;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoadoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final SlotId RING = SlotId.parse("curios:ring/0");

    /** In-memory player: worn equipment, worn accessories, and a main inventory. */
    static final class FakePlayer {
        final Map<EquipmentSlot, byte[]> worn = new EnumMap<>(EquipmentSlot.class);
        final Map<SlotId, byte[]> accessories = new LinkedHashMap<>();
        final List<byte[]> inventory = new ArrayList<>();
    }

    static final class FakeEquipment implements PlayerEquipment<FakePlayer> {
        @Override public Optional<byte[]> read(FakePlayer p, EquipmentSlot s) {
            return Optional.ofNullable(p.worn.get(s));
        }
        @Override public void clear(FakePlayer p, EquipmentSlot s) { p.worn.remove(s); }
        @Override public void equip(FakePlayer p, EquipmentSlot s, byte[] stack) {
            byte[] old = p.worn.put(s, stack);
            if (old != null) p.inventory.add(old);
        }
        @Override public void give(FakePlayer p, byte[] stack) { p.inventory.add(stack); }
    }

    static final class FakeAccessories implements AccessoryBridge<FakePlayer, Object> {
        @Override public String id() { return "fake"; }
        @Override public boolean isAvailable() { return true; }
        @Override public Map<SlotId, byte[]> capture(FakePlayer p) { return Map.copyOf(p.accessories); }
        @Override public void apply(FakePlayer p, Map<SlotId, byte[]> stacks) {
            stacks.forEach((k, v) -> {
                byte[] old = p.accessories.put(k, v);
                if (old != null) p.inventory.add(old);
            });
        }
        @Override public Set<SlotId> knownSlots(FakePlayer p) { return p.accessories.keySet(); }
        @Override public void clear(FakePlayer p, Set<SlotId> ids) { ids.forEach(p.accessories::remove); }
    }

    private final LoadoutService<FakePlayer> service =
            new LoadoutService<>(new FakeEquipment(), new FakeAccessories(), () -> NOW);
    private final LockerData fresh = LockerData.fresh(UUID.randomUUID(), "owner", NOW);

    private static byte[] b(int v) { return new byte[] {(byte) v}; }

    private static Loadout loadout(String name, EquipmentSlot slot, byte[] bytes) {
        Map<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        eq.put(slot, bytes);
        return new Loadout(name, NOW, eq, Map.of());
    }

    // --- save ----------------------------------------------------------------

    @Test
    void saveMovesWornGearAndAccessoriesIntoTheSlot() {
        FakePlayer p = new FakePlayer();
        p.worn.put(EquipmentSlot.HEAD, b(1));
        p.worn.put(EquipmentSlot.OFFHAND, b(2));
        p.accessories.put(RING, b(3));

        LoadoutOutcome out = service.save(fresh, 0, "  pvp  ", p);

        Loadout saved = out.updated().orElseThrow().slot(0).orElseThrow();
        assertThat(saved.name()).isEqualTo("pvp");
        assertThat(saved.savedAt()).isEqualTo(NOW);
        assertThat(saved.equipment()).containsOnlyKeys(EquipmentSlot.HEAD, EquipmentSlot.OFFHAND);
        assertThat(saved.accessories()).containsOnlyKeys(RING);
        assertThat(p.worn).isEmpty();
        assertThat(p.accessories).isEmpty();
        assertThat(out.feedback()).contains(new Feedback(LoadoutOutcome.SAVED, List.of("pvp")));
    }

    @Test
    void saveWithBlankNameUsesSlotNumber() {
        FakePlayer p = new FakePlayer();
        p.worn.put(EquipmentSlot.CHEST, b(1));

        LoadoutOutcome out = service.save(fresh, 2, " ", p);

        assertThat(out.updated().orElseThrow().slot(2).orElseThrow().name()).isEqualTo("Loadout 3");
    }

    @Test
    void saveWithAccessoriesOnlyIsAllowed() {
        FakePlayer p = new FakePlayer();
        p.accessories.put(RING, b(3));

        assertThat(service.save(fresh, 0, null, p).updated()).isPresent();
    }

    @Test
    void saveWithNothingEquippedIsRejected() {
        LoadoutOutcome out = service.save(fresh, 0, "x", new FakePlayer());

        assertThat(out.updated()).isEmpty();
        assertThat(out.feedback()).contains(new Feedback(LoadoutOutcome.NOTHING_TO_SAVE, List.of()));
    }

    @Test
    void saveOverPopulatedSlotReturnsPreviousItemsFirst() {
        LockerData data = fresh.withSlot(0, new Loadout("old", NOW,
                Map.of(EquipmentSlot.FEET, b(9)), Map.of(RING, b(8))));
        FakePlayer p = new FakePlayer();
        p.worn.put(EquipmentSlot.HEAD, b(1));

        LoadoutOutcome out = service.save(data, 0, "new", p);

        assertThat(p.inventory).containsExactlyInAnyOrder(b(9), b(8));
        assertThat(out.updated().orElseThrow().slot(0).orElseThrow().name()).isEqualTo("new");
    }

    @Test
    void saveToInvalidSlotDoesNothing() {
        FakePlayer p = new FakePlayer();
        p.worn.put(EquipmentSlot.HEAD, b(1));

        assertThat(service.save(fresh, -1, "x", p)).isEqualTo(LoadoutOutcome.NONE);
        assertThat(service.save(fresh, LockerData.SLOT_COUNT, "x", p)).isEqualTo(LoadoutOutcome.NONE);
        assertThat(p.worn).containsKey(EquipmentSlot.HEAD);
    }

    // --- load ----------------------------------------------------------------

    @Test
    void loadEquipsMergesAndEmptiesTheSlot() {
        LockerData data = fresh.withSlot(1, new Loadout("kit", NOW,
                Map.of(EquipmentSlot.HEAD, b(1)), Map.of(RING, b(3))));
        FakePlayer p = new FakePlayer();
        p.worn.put(EquipmentSlot.HEAD, b(7));   // displaced
        p.worn.put(EquipmentSlot.LEGS, b(6));   // untouched

        LoadoutOutcome out = service.load(data, 1, p);

        assertThat(p.worn.get(EquipmentSlot.HEAD)).isEqualTo(b(1));
        assertThat(p.worn.get(EquipmentSlot.LEGS)).isEqualTo(b(6));
        assertThat(p.accessories.get(RING)).isEqualTo(b(3));
        assertThat(p.inventory).containsExactly(b(7));
        assertThat(out.updated().orElseThrow().slot(1)).isEmpty();
        assertThat(out.feedback()).contains(new Feedback(LoadoutOutcome.LOADED, List.of("kit")));
    }

    @Test
    void loadEmptyOrInvalidSlotDoesNothing() {
        assertThat(service.load(fresh, 0, new FakePlayer())).isEqualTo(LoadoutOutcome.NONE);
        assertThat(service.load(fresh, 99, new FakePlayer())).isEqualTo(LoadoutOutcome.NONE);
    }

    // --- rename / delete -----------------------------------------------------

    @Test
    void renameSanitizesAndKeepsOldNameWhenBlank() {
        LockerData data = fresh.withSlot(0, loadout("old", EquipmentSlot.HEAD, b(1)));

        assertThat(service.rename(data, 0, " new ").updated().orElseThrow()
                .slot(0).orElseThrow().name()).isEqualTo("new");
        assertThat(service.rename(data, 0, "").updated().orElseThrow()
                .slot(0).orElseThrow().name()).isEqualTo("old");
        assertThat(service.rename(data, 0, "x").feedback()).isEmpty();
    }

    @Test
    void renameEmptyOrInvalidSlotDoesNothing() {
        assertThat(service.rename(fresh, 0, "x")).isEqualTo(LoadoutOutcome.NONE);
        assertThat(service.rename(fresh, -5, "x")).isEqualTo(LoadoutOutcome.NONE);
    }

    @Test
    void deleteClearsPopulatedSlot() {
        LockerData data = fresh.withSlot(0, loadout("old", EquipmentSlot.HEAD, b(1)));

        assertThat(service.delete(data, 0).updated().orElseThrow().slot(0)).isEmpty();
    }

    @Test
    void deleteEmptyOrInvalidSlotDoesNothing() {
        assertThat(service.delete(fresh, 0)).isEqualTo(LoadoutOutcome.NONE);
        assertThat(service.delete(fresh, 6)).isEqualTo(LoadoutOutcome.NONE);
    }

    // --- construction --------------------------------------------------------

    @Test
    void rejectsNullCollaborators() {
        assertThatThrownBy(() -> new LoadoutService<>(null, new FakeAccessories(), () -> NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LoadoutService<>(new FakeEquipment(), null, () -> NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LoadoutService<>(new FakeEquipment(), new FakeAccessories(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outcomeRejectsNulls() {
        assertThatThrownBy(() -> new LoadoutOutcome(null, Optional.empty()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LoadoutOutcome(Optional.empty(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Feedback(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
