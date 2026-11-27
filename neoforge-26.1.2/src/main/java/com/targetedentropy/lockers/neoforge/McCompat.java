package com.targetedentropy.lockers.neoforge;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Minecraft 26.1.2 side of the version seam. See the 1.21.1 module's
 * {@code McCompat} for the contract; signatures match across modules.
 */
public final class McCompat {

    private McCompat() {}

    /** 26.1: {@code ResourceLocation} was renamed {@code Identifier}. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LockersMod.MOD_ID, path);
    }

    /** 26.1: {@code displayClientMessage(msg, true)} became {@code sendOverlayMessage}. */
    public static void overlay(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }

    /** 26.1: authlib 7 made {@code GameProfile} a record. */
    public static String profileName(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    /** 26.1: numeric permission levels became named permission sets. */
    public static boolean isOp(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** 26.1: {@code CompoundTag.getCompound} already returns {@code Optional}. */
    public static Optional<CompoundTag> compound(CompoundTag tag, String key) {
        return tag.getCompound(key);
    }

    /** Highest buildable Y coordinate (inclusive). */
    public static int maxY(LevelHeightAccessor level) {
        return level.getMaxY();
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(
            BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return new BlockEntityType<>(factory, block);
    }

    /** No Accessories build exists for 26.1.2 yet, so the selector never offers it. */
    public static Optional<Supplier<AccessoryBridge<ServerPlayer, ItemStack>>> accessoriesBridge() {
        return Optional.empty();
    }
}
