package com.targetedentropy.lockers.common.compat;

import com.targetedentropy.lockers.common.model.SlotId;
import java.util.Map;
import java.util.Set;

/**
 * Version- and API-agnostic interface for capturing and restoring accessory
 * slot contents on a player.
 * <p>
 * Parameterized on opaque {@code PLAYER} and {@code STACK} types so that
 * {@code common} never imports {@code net.minecraft.*}. Each per-version
 * NeoForge module provides a bound implementation
 * ({@code Curios9Bridge}, {@code Curios10Bridge}, {@code Accessories1Bridge},
 * {@code Accessories2Bridge}) and a {@code NoopAccessoryBridge} fallback for
 * servers where neither mod is installed.
 *
 * @param <P> the platform Player type (e.g. {@code net.minecraft.server.level.ServerPlayer})
 * @param <S> the platform ItemStack type (e.g. {@code net.minecraft.world.item.ItemStack})
 */
public interface AccessoryBridge<P, S> {

    /**
     * A short human-readable identifier ({@code "curios"}, {@code "accessories"},
     * {@code "noop"}) for logging and config surfaces.
     */
    String id();

    /**
     * Whether this bridge is currently usable (e.g. the target mod is loaded).
     * Bridges that can't execute for any reason should return {@code false}
     * and their methods should be no-ops.
     */
    boolean isAvailable();

    /**
     * Capture the current accessory-slot contents of {@code player} as a
     * map of {@link SlotId} → serialized NBT {@code byte[]} blobs.
     * <p>
     * Empty slots should be omitted from the returned map.
     */
    Map<SlotId, byte[]> capture(P player);

    /**
     * Apply the given map of accessory-slot contents to {@code player}.
     * Slots present in {@code stacks} but not on the player should be
     * silently skipped. Slots on the player but not in {@code stacks}
     * should be left untouched.
     */
    void apply(P player, Map<SlotId, byte[]> stacks);

    /**
     * The set of slot ids this bridge knows about for the given player
     * (relevant because Curios and Accessories can report different slot sets
     * per-dimension / per-config). Used by the GUI to render slot previews.
     */
    Set<SlotId> knownSlots(P player);

    /**
     * Empty (set to {@code ItemStack.EMPTY}) every slot in {@code slotIds}.
     * Used by the Locker after a Save captured items off the player — items
     * physically move into the locker, so the player's slots must be cleared.
     * <p>
     * Slot ids in a different namespace, or pointing at slots that don't exist
     * on this player, are silently ignored. Items in cleared slots are NOT
     * returned to the player's inventory — the caller already has the saved
     * snapshot in hand.
     */
    void clear(P player, Set<SlotId> slotIds);
}
