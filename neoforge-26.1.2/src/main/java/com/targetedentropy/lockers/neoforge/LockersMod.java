package com.targetedentropy.lockers.neoforge;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.compat.BridgeSelector;
import com.targetedentropy.lockers.common.compat.BridgeSelector.SelectedImpl;
import com.targetedentropy.lockers.common.compat.NoopAccessoryBridge;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.neoforge.compat.BridgeRegistry;
import com.targetedentropy.lockers.neoforge.compat.curios.Curios15Bridge;
import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import com.targetedentropy.lockers.neoforge.registry.LockersBlocks;
import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import com.targetedentropy.lockers.neoforge.registry.LockersMenus;
import net.minecraft.resources.Identifier;
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

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

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
        CommonConfig cfg = CommonConfig.defaults();
        boolean curios = ModList.get().isLoaded("curios");
        boolean accessories = ModList.get().isLoaded("accessories");
        SelectedImpl sel = BridgeSelector.choose(curios, accessories, cfg);
        if (sel.degraded()) {
            LOG.warn("[{}] accessory impl {} requested but not available; falling back to {}",
                    MOD_ID, sel.requested(), sel.kind());
        }
        return switch (sel.kind()) {
            case CURIOS -> new Curios15Bridge();
            // Accessories not yet published for MC 26.1.2 — Wisp Forest's
            // latest is +1.21.10. Fall through to the no-op bridge so the
            // mod loads cleanly when (and if) a player has Accessories
            // installed against an older MC. Add a real Accessories bridge
            // here once the lib publishes a 26.1.2 build.
            case ACCESSORIES -> new NoopAccessoryBridge<>();
            case NOOP -> new NoopAccessoryBridge<>();
        };
    }
}
