package com.targetedentropy.lockers.neoforge;

import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersCreativeTab {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LockersMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = REGISTRY.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lockers.main"))
                    .icon(() -> new ItemStack(LockersItems.LOCKER_ITEM.get()))
                    .displayItems((params, out) -> out.accept(LockersItems.LOCKER_ITEM.get()))
                    .build());

    private LockersCreativeTab() {}
}
