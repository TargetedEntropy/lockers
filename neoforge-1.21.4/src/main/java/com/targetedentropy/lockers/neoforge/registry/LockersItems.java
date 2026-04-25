package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersItems {

    public static final DeferredRegister.Items REGISTRY =
            DeferredRegister.createItems(LockersMod.MOD_ID);

    public static final DeferredItem<BlockItem> LOCKER_ITEM = REGISTRY.registerSimpleBlockItem(
            LockersBlocks.LOCKER, new Item.Properties());

    private LockersItems() {}
}
