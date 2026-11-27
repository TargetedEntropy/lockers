package com.targetedentropy.lockers.neoforge.player;

import com.targetedentropy.lockers.common.loadout.PlayerEquipment;
import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.neoforge.nbt.ItemStackSerializer;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** {@link PlayerEquipment} bound to a {@link ServerPlayer}'s armor and offhand slots. */
public final class ServerPlayerEquipment implements PlayerEquipment<ServerPlayer> {

    public static final ServerPlayerEquipment INSTANCE = new ServerPlayerEquipment();

    private ServerPlayerEquipment() {}

    @Override
    public Optional<byte[]> read(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(toMc(slot));
        return stack.isEmpty()
                ? Optional.empty()
                : Optional.of(ItemStackSerializer.toBytes(stack, registries(player)));
    }

    @Override
    public void clear(ServerPlayer player, EquipmentSlot slot) {
        player.setItemSlot(toMc(slot), ItemStack.EMPTY);
    }

    @Override
    public void equip(ServerPlayer player, EquipmentSlot slot, byte[] stack) {
        net.minecraft.world.entity.EquipmentSlot mcSlot = toMc(slot);
        ItemStack displaced = player.getItemBySlot(mcSlot);
        player.setItemSlot(mcSlot, ItemStackSerializer.fromBytes(stack, registries(player)));
        giveOrDrop(player, displaced);
    }

    @Override
    public void give(ServerPlayer player, byte[] stack) {
        giveOrDrop(player, ItemStackSerializer.fromBytes(stack, registries(player)));
    }

    /**
     * Put {@code stack} in the player's main inventory, or drop it at their
     * feet if the inventory is full. Gear is never silently deleted.
     */
    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static HolderLookup.Provider registries(ServerPlayer player) {
        return player.level().registryAccess();
    }

    private static net.minecraft.world.entity.EquipmentSlot toMc(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD    -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case CHEST   -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGS    -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case FEET    -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        };
    }
}
