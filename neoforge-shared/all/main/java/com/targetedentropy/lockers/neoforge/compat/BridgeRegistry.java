package com.targetedentropy.lockers.neoforge.compat;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.compat.NoopAccessoryBridge;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Process-wide holder for the selected accessory bridge. Set once at
 * mod-construction time by {@code LockersMod}; looked up everywhere else.
 * <p>
 * Plain AtomicReference rather than static field so tests can swap impls.
 */
public final class BridgeRegistry {

    private static final AtomicReference<AccessoryBridge<ServerPlayer, ItemStack>> REF =
            new AtomicReference<>(new NoopAccessoryBridge<>());

    private BridgeRegistry() {}

    public static void set(AccessoryBridge<ServerPlayer, ItemStack> bridge) {
        REF.set(Objects.requireNonNull(bridge, "bridge"));
    }

    public static AccessoryBridge<ServerPlayer, ItemStack> get() {
        return REF.get();
    }
}
