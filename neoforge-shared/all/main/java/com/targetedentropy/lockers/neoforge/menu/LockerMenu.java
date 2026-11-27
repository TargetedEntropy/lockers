package com.targetedentropy.lockers.neoforge.menu;

import com.targetedentropy.lockers.neoforge.block.LockerBlockEntity;
import com.targetedentropy.lockers.neoforge.registry.LockersBlocks;
import com.targetedentropy.lockers.neoforge.registry.LockersMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Locker. Intentionally minimal — the GUI is a stateless
 * control surface: it sends save/load/rename/delete packets rather than
 * carrying real item slots. Slot management happens server-side in the
 * BlockEntity + AccessoryBridge, not here.
 */
public class LockerMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public LockerMenu(int containerId, Inventory inv, BlockPos pos) {
        super(LockersMenus.LOCKER.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
    }

    /** Network factory used by {@link net.neoforged.neoforge.common.extensions.IMenuTypeExtension}. */
    public static LockerMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new LockerMenu(id, inv, buf.readBlockPos());
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No real slots; shift-click does nothing.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, LockersBlocks.LOCKER.get())
                && player instanceof net.minecraft.server.level.ServerPlayer sp
                && resolveBlockEntity().map(be -> be.canAccess(sp)).orElse(false);
    }

    private java.util.Optional<LockerBlockEntity> resolveBlockEntity() {
        return access.evaluate((level, p) -> level.getBlockEntity(p) instanceof LockerBlockEntity be
                ? be
                : null);
    }
}
