package com.targetedentropy.lockers.common.loadout;

import com.targetedentropy.lockers.common.model.EquipmentSlot;
import java.util.Optional;

/**
 * Port onto a player's five vanilla equipment slots (armor + offhand), in
 * terms of serialized item stacks so {@code common} never sees a Minecraft
 * type. Each NeoForge module binds it to {@code ServerPlayer}.
 *
 * @param <P> the platform player type
 */
public interface PlayerEquipment<P> {

    /** The serialized stack in {@code slot}, or empty when the slot is empty. */
    Optional<byte[]> read(P player, EquipmentSlot slot);

    /** Empty {@code slot}. The removed item is discarded: the caller already holds a copy. */
    void clear(P player, EquipmentSlot slot);

    /**
     * Put {@code stack} into {@code slot}. Whatever the slot held before goes
     * to the player's inventory, or onto the floor if the inventory is full.
     */
    void equip(P player, EquipmentSlot slot, byte[] stack);

    /** Give {@code stack} to the player's inventory, dropping it if the inventory is full. */
    void give(P player, byte[] stack);
}
