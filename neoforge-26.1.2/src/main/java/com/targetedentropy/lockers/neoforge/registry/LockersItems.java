package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersItems {

    public static final DeferredRegister.Items REGISTRY =
            DeferredRegister.createItems(LockersMod.MOD_ID);

    // 26.1.2 changed registerSimpleBlockItem to take a Supplier<Item.Properties>
    // instead of a constructed Item.Properties — pass a method reference.
    public static final DeferredItem<BlockItem> LOCKER_ITEM = REGISTRY.registerSimpleBlockItem(
            LockersBlocks.LOCKER, (java.util.function.Supplier<Item.Properties>) Item.Properties::new);

    private LockersItems() {}
}
