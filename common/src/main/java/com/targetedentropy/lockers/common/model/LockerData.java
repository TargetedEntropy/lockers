package com.targetedentropy.lockers.common.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The full persistent state of a single Locker block.
 * <p>
 * Holds exactly {@link #SLOT_COUNT} loadout slots. A missing loadout at index
 * {@code i} is represented by {@code Optional.empty()} at that position.
 * <p>
 * {@code schemaVersion} lets us migrate the NBT format in the future without
 * breaking existing worlds — {@link com.targetedentropy.lockers.common.serialize.LockerDataCodec}
 * branches on it.
 */
public record LockerData(
        int schemaVersion,
        UUID owner,
        Optional<String> ownerNameCached,
        AccessControl access,
        List<Optional<Loadout>> slots,
        Instant createdAt
) {

    public static final int SLOT_COUNT = 6;
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public LockerData {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(ownerNameCached, "ownerNameCached");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(createdAt, "createdAt");

        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        if (slots.size() != SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "slots must be size " + SLOT_COUNT + ", got " + slots.size());
        }
        for (Optional<Loadout> slot : slots) {
            Objects.requireNonNull(slot, "slot entry (use Optional.empty() for empty)");
        }
        slots = List.copyOf(slots);
    }

    /** A freshly placed Locker with no loadouts. */
    public static LockerData fresh(UUID owner, String ownerName, Instant now) {
        List<Optional<Loadout>> empty = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) empty.add(Optional.empty());
        return new LockerData(
                CURRENT_SCHEMA_VERSION,
                owner,
                Optional.ofNullable(ownerName),
                AccessControl.OWNER_ONLY,
                Collections.unmodifiableList(empty),
                now
        );
    }

    /** Returns a new {@code LockerData} with the given slot set to the loadout. */
    public LockerData withSlot(int index, Loadout loadout) {
        requireValidSlotIndex(index);
        List<Optional<Loadout>> copy = new ArrayList<>(slots);
        copy.set(index, Optional.ofNullable(loadout));
        return new LockerData(schemaVersion, owner, ownerNameCached, access, copy, createdAt);
    }

    /** Returns a new {@code LockerData} with the given slot cleared. */
    public LockerData clearSlot(int index) {
        requireValidSlotIndex(index);
        List<Optional<Loadout>> copy = new ArrayList<>(slots);
        copy.set(index, Optional.empty());
        return new LockerData(schemaVersion, owner, ownerNameCached, access, copy, createdAt);
    }

    public LockerData withAccess(AccessControl newAccess) {
        return new LockerData(schemaVersion, owner, ownerNameCached, newAccess, slots, createdAt);
    }

    /**
     * Returns a copy with the owner replaced. Used when a Locker block is
     * broken and re-placed: the saved loadouts survive (carried via the dropped
     * item's NBT) but the new placer becomes the owner.
     */
    public LockerData withOwner(UUID newOwner, String newOwnerName) {
        Objects.requireNonNull(newOwner, "newOwner");
        return new LockerData(schemaVersion, newOwner, Optional.ofNullable(newOwnerName),
                access, slots, createdAt);
    }

    public Optional<Loadout> slot(int index) {
        requireValidSlotIndex(index);
        return slots.get(index);
    }

    private static void requireValidSlotIndex(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "slot index " + index + " out of range [0," + SLOT_COUNT + ")");
        }
    }
}
