package com.targetedentropy.lockers.neoforge;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.neoforge.compat.accessories.AccessoriesBridge;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Minecraft 1.21.4 side of the version seam. See the 1.21.1 module's
 * {@code McCompat} for the contract; signatures match across modules.
 */
public final class McCompat {

    private McCompat() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, path);
    }

    /** Show {@code message} on the player's action bar. */
    public static void overlay(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static String profileName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    /** Vanilla permission level 2 (gamemaster commands). */
    public static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    public static Optional<CompoundTag> compound(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(tag.getCompound(key)) : Optional.empty();
    }

    /** Highest buildable Y coordinate (inclusive). 1.21.4 replaced getMaxBuildHeight() with getMaxY(). */
    public static int maxY(LevelHeightAccessor level) {
        return level.getMaxY();
    }

    /** 1.21.4 removed {@code BlockEntityType.Builder}; the constructor is public instead. */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(
            BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return new BlockEntityType<>(factory, block);
    }

    public static Optional<Supplier<AccessoryBridge<ServerPlayer, ItemStack>>> accessoriesBridge() {
        return Optional.of(() -> new AccessoriesBridge());
    }
}
