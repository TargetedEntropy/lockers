package com.targetedentropy.lockers.neoforge.compat.accessories;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.model.SlotId;
import com.targetedentropy.lockers.neoforge.LockersMod;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accessories 1.2.x (1.21.4) implementation of {@link AccessoryBridge}.
 * Selected at runtime only when Accessories is loaded. Concrete capture/apply
 * via {@code io.wispforest.accessories.api.AccessoriesCapability} is stubbed
 * in this scaffold.
 */
public final class Accessories2Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "accessories";
    private static final Logger LOG = LoggerFactory.getLogger(Accessories2Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        LOG.warn("[{}] Accessories 2 capture not yet implemented — skipping accessory slots",
                LockersMod.MOD_ID);
        return Map.of();
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        if (!stacks.isEmpty()) {
            LOG.warn("[{}] Accessories 2 apply not yet implemented — {} accessory slots skipped",
                    LockersMod.MOD_ID, stacks.size());
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        return Set.of();
    }
}
