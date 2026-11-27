package com.targetedentropy.lockers.neoforge;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.compat.BridgeSelector;
import com.targetedentropy.lockers.common.compat.BridgeSelector.SelectedImpl;
import com.targetedentropy.lockers.common.compat.NoopAccessoryBridge;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.neoforge.compat.BridgeRegistry;
import com.targetedentropy.lockers.neoforge.compat.curios.CuriosBridge;
import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import com.targetedentropy.lockers.neoforge.registry.LockersBlocks;
import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import com.targetedentropy.lockers.neoforge.registry.LockersMenus;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LockersMod.MOD_ID)
public final class LockersMod {

    public static final String MOD_ID = "lockers";
    private static final Logger LOG = LoggerFactory.getLogger(LockersMod.class);

    public LockersMod(IEventBus modBus, ModContainer container) {
        LockersBlocks.REGISTRY.register(modBus);
        LockersItems.REGISTRY.register(modBus);
        LockersBlockEntities.REGISTRY.register(modBus);
        LockersMenus.REGISTRY.register(modBus);
        LockersCreativeTab.REGISTRY.register(modBus);

        AccessoryBridge<ServerPlayer, ItemStack> bridge = selectAccessoryBridge();
        BridgeRegistry.set(bridge);
        LOG.info("[{}] using accessory bridge: {}", MOD_ID, bridge.id());
    }

    private static AccessoryBridge<ServerPlayer, ItemStack> selectAccessoryBridge() {
        // An MC line with no Accessories build reports it as unavailable, so a
        // config preference for it degrades (with a warning) like any other.
        Optional<Supplier<AccessoryBridge<ServerPlayer, ItemStack>>> accessoriesImpl =
                McCompat.accessoriesBridge();
        boolean curios = ModList.get().isLoaded("curios");
        boolean accessories = accessoriesImpl.isPresent() && ModList.get().isLoaded("accessories");

        SelectedImpl sel = BridgeSelector.choose(curios, accessories, CommonConfig.defaults());
        if (sel.degraded()) {
            LOG.warn("[{}] accessory impl {} requested but not available; falling back to {}",
                    MOD_ID, sel.requested(), sel.kind());
        }
        return switch (sel.kind()) {
            case CURIOS -> new CuriosBridge();
            case ACCESSORIES -> accessoriesImpl.orElseThrow().get();
            case NOOP -> new NoopAccessoryBridge<>();
        };
    }
}
