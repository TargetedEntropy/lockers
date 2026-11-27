package com.targetedentropy.lockers.common.loadout;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.model.SlotId;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The loadout rules of a Locker, independent of Minecraft version.
 * <ul>
 *   <li><b>Save moves gear in.</b> Armor, offhand and accessories are copied
 *       into the slot and then cleared off the player. Saving with nothing
 *       equipped is rejected. Saving over a populated slot first returns that
 *       slot's items to the player.</li>
 *   <li><b>Load moves gear out, merging.</b> Only slots named in the loadout
 *       are touched; whatever the player wore there goes to their inventory.
 *       The locker slot is emptied afterwards.</li>
 *   <li><b>Rename and delete</b> only edit locker state.</li>
 * </ul>
 * Slot indices arrive from client packets, so an out-of-range index is
 * treated as "nothing to do" rather than an error.
 * <p>
 * Every method is pure with respect to {@link LockerData}: it returns the new
 * state in a {@link LoadoutOutcome} and the caller persists it.
 *
 * @param <P> the platform player type
 */
public final class LoadoutService<P> {

    private final PlayerEquipment<P> equipment;
    private final AccessoryBridge<P, ?> accessories;
    private final Supplier<Instant> clock;

    public LoadoutService(PlayerEquipment<P> equipment, AccessoryBridge<P, ?> accessories,
                          Supplier<Instant> clock) {
        this.equipment = Objects.requireNonNull(equipment, "equipment");
        this.accessories = Objects.requireNonNull(accessories, "accessories");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public LoadoutOutcome save(LockerData data, int slot, String rawName, P player) {
        if (!LockerData.isValidSlot(slot)) return LoadoutOutcome.NONE;

        Map<EquipmentSlot, byte[]> worn = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot s : EquipmentSlot.values()) {
            equipment.read(player, s).ifPresent(bytes -> worn.put(s, bytes));
        }
        Map<SlotId, byte[]> accessoryStacks = accessories.capture(player);
        if (worn.isEmpty() && accessoryStacks.isEmpty()) {
            return LoadoutOutcome.message(LoadoutOutcome.NOTHING_TO_SAVE);
        }

        data.slot(slot).ifPresent(previous -> giveBack(player, previous));

        String name = Loadout.sanitizeName(rawName, "Loadout " + (slot + 1));
        Loadout saved = new Loadout(name, clock.get(), worn, accessoryStacks);
        worn.keySet().forEach(s -> equipment.clear(player, s));
        accessories.clear(player, accessoryStacks.keySet());
        return LoadoutOutcome.updated(data.withSlot(slot, saved), LoadoutOutcome.SAVED, name);
    }

    public LoadoutOutcome load(LockerData data, int slot, P player) {
        Optional<Loadout> stored = slotOf(data, slot);
        if (stored.isEmpty()) return LoadoutOutcome.NONE;
        Loadout loadout = stored.get();

        for (EquipmentSlot s : EquipmentSlot.values()) {
            byte[] bytes = loadout.equipment().get(s);
            if (bytes != null) equipment.equip(player, s, bytes);
        }
        accessories.apply(player, loadout.accessories());
        return LoadoutOutcome.updated(data.clearSlot(slot), LoadoutOutcome.LOADED, loadout.name());
    }

    public LoadoutOutcome rename(LockerData data, int slot, String rawName) {
        return slotOf(data, slot)
                .map(l -> LoadoutOutcome.updated(
                        data.withSlot(slot, l.withName(Loadout.sanitizeName(rawName, l.name())))))
                .orElse(LoadoutOutcome.NONE);
    }

    public LoadoutOutcome delete(LockerData data, int slot) {
        return slotOf(data, slot)
                .map(l -> LoadoutOutcome.updated(data.clearSlot(slot)))
                .orElse(LoadoutOutcome.NONE);
    }

    private static Optional<Loadout> slotOf(LockerData data, int slot) {
        return LockerData.isValidSlot(slot) ? data.slot(slot) : Optional.empty();
    }

    private void giveBack(P player, Loadout loadout) {
        loadout.equipment().values().forEach(bytes -> equipment.give(player, bytes));
        loadout.accessories().values().forEach(bytes -> equipment.give(player, bytes));
    }
}
