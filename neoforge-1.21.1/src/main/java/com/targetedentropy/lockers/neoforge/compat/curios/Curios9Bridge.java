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
 * Curios 9.x (1.21.1) implementation of {@link AccessoryBridge}.
 * <p>
 * Selected at runtime only when Curios is loaded. Therefore it is safe for
 * this class to reference Curios API types directly — they are guaranteed to
 * be on the classpath by the time an instance exists.
 * <p>
 * The full capture/apply implementation is intentionally left stubbed in this
 * initial scaffold; wiring up the Curios 9 inventory API is a focused
 * follow-up task tracked in the project roadmap.
 */
public final class Curios9Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "curios";
    private static final Logger LOG = LoggerFactory.getLogger(Curios9Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        // TODO: implement via CuriosApi.getCuriosInventory(player) on Curios 9.x
        LOG.warn("[{}] Curios 9 capture not yet implemented — skipping accessory slots",
                LockersMod.MOD_ID);
        return Map.of();
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        // TODO: implement via CuriosApi.getCuriosInventory(player) on Curios 9.x
        if (!stacks.isEmpty()) {
            LOG.warn("[{}] Curios 9 apply not yet implemented — {} accessory slots skipped",
                    LockersMod.MOD_ID, stacks.size());
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        return Set.of();
    }
}
