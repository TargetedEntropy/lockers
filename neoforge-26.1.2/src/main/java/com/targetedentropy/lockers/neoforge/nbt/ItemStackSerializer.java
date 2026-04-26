package com.targetedentropy.lockers.neoforge.nbt;

import com.mojang.serialization.DataResult;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ItemStack} ↔ {@code byte[]} adapter. Used by the Locker to stash
 * NBT blobs of captured equipment inside {@code common}'s {@code Loadout}
 * without leaking Minecraft types into {@code common}.
 * <p>
 * MC 26.1.2 removed the imperative {@code ItemStack.save(Provider)} /
 * {@code ItemStack.parseOptional(Provider, CompoundTag)} pair. Serialization
 * now goes through {@code ItemStack.CODEC} with a {@code RegistryOps} that
 * scopes the registry lookup. Empty stacks still serialise to an empty
 * byte array.
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    public static byte[] toBytes(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new byte[0];
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        DataResult<Tag> result = ItemStack.CODEC.encodeStart(ops, stack);
        Tag raw = result.getOrThrow(s -> new IllegalStateException("ItemStack encode failed: " + s));
        if (!(raw instanceof CompoundTag tag)) {
            throw new IllegalStateException(
                    "ItemStack.CODEC produced non-compound: " + raw.getClass().getSimpleName());
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
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, tag);
            // Failed parse → empty stack (legacy parseOptional behaviour).
            return result.result().orElse(ItemStack.EMPTY);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to deserialize ItemStack", e);
        }
    }
}
