package com.targetedentropy.lockers.neoforge.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ItemStack} ↔ {@code byte[]} adapter. Used by the Locker to stash
 * NBT blobs of captured equipment inside {@code common}'s {@code Loadout}
 * without leaking Minecraft types into {@code common}.
 * <p>
 * Empty stacks serialise to an empty byte array; callers should avoid
 * storing the empty sentinel in the loadout maps.
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    public static byte[] toBytes(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new byte[0];
        Tag raw = stack.save(registries);
        if (!(raw instanceof CompoundTag tag)) {
            throw new IllegalStateException(
                    "ItemStack.save returned non-compound tag: " + raw.getClass().getSimpleName());
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
        try (DataOutputStream out = new DataOutputStream(buf)) {
            NbtIo.write(tag, out);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to serialize ItemStack", e);
        }
        return buf.toByteArray();
    }

    public static ItemStack fromBytes(byte[] bytes, HolderLookup.Provider registries) {
        if (bytes.length == 0) return ItemStack.EMPTY;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            CompoundTag tag = NbtIo.read(in, NbtAccounter.unlimitedHeap());
            return ItemStack.parseOptional(registries, tag);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to deserialize ItemStack", e);
        }
    }
}
