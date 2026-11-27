package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(Registries.MENU, LockersMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LockerMenu>> LOCKER =
            REGISTRY.register(
                    "locker",
                    () -> IMenuTypeExtension.create(LockerMenu::fromNetwork));

    private LockersMenus() {}

    /** Convenience to keep FeatureFlags reference live so ProGuard etc. don't strip it. */
    public static void touch() {
        assert FeatureFlags.VANILLA_SET != null;
    }
}
