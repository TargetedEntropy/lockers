package com.targetedentropy.lockers.neoforge.compat.curios;

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
 * Curios 10.x (1.21.4+) implementation of {@link AccessoryBridge}.
 * <p>
 * Major API differences from the 9.x version: {@code CuriosApi.getCuriosHelper()}
 * is gone, replaced by {@code CuriosApi.getCuriosInventory(Player)} which
 * returns an {@code Optional<ICuriosItemHandler>}; capability-style
 * registration via {@code RegisterCapabilitiesEvent} replaces direct events.
 * <p>
 * Selected at runtime only when Curios is loaded. Concrete capture/apply
 * implementation is stubbed in this scaffold.
 */
public final class Curios10Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "curios";
    private static final Logger LOG = LoggerFactory.getLogger(Curios10Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        // TODO: implement via CuriosApi.getCuriosInventory(player) on Curios 10.x
        LOG.warn("[{}] Curios 10 capture not yet implemented — skipping accessory slots",
                LockersMod.MOD_ID);
        return Map.of();
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        if (!stacks.isEmpty()) {
            LOG.warn("[{}] Curios 10 apply not yet implemented — {} accessory slots skipped",
                    LockersMod.MOD_ID, stacks.size());
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        return Set.of();
    }
}
