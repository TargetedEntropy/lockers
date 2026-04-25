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
 * Accessories 1.x (1.21.1) implementation of {@link AccessoryBridge}.
 * <p>
 * Selected at runtime only when Accessories is loaded; safe to reference
 * Accessories classes directly. The full capture/apply implementation is
 * stubbed — wiring up io.wispforest.accessories.api.AccessoriesCapability is
 * a focused follow-up.
 */
public final class Accessories1Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "accessories";
    private static final Logger LOG = LoggerFactory.getLogger(Accessories1Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        LOG.warn("[{}] Accessories 1 capture not yet implemented — skipping accessory slots",
                LockersMod.MOD_ID);
        return Map.of();
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        if (!stacks.isEmpty()) {
            LOG.warn("[{}] Accessories 1 apply not yet implemented — {} accessory slots skipped",
                    LockersMod.MOD_ID, stacks.size());
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        return Set.of();
    }
}
