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
 * Minecraft 1.21.1 side of the version seam. Every API call that differs
 * between the MC lines we ship goes through a method here, so the code under
 * {@code neoforge-shared/} compiles unchanged against each version.
 * <p>
 * Each {@code neoforge-<ver>} module has its own {@code McCompat} with the
 * same method signatures (return types may name version-specific classes).
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

    /** Highest buildable Y coordinate (inclusive). */
    public static int maxY(LevelHeightAccessor level) {
        return level.getMaxBuildHeight() - 1;
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(
            BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return BlockEntityType.Builder.of(factory, block).build(null);
    }

    /**
     * Factory for this version's Accessories bridge, or empty when no
     * Accessories build exists for this MC line. The supplier keeps the bridge
     * class (and the Accessories API it links against) unloaded until chosen.
     */
    public static Optional<Supplier<AccessoryBridge<ServerPlayer, ItemStack>>> accessoriesBridge() {
        return Optional.of(() -> new AccessoriesBridge());
    }
}
