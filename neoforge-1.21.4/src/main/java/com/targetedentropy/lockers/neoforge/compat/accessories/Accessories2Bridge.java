package com.targetedentropy.lockers.neoforge.compat.accessories;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.model.SlotId;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.nbt.ItemStackSerializer;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accessories 1.2.x (1.21.4) implementation of {@link AccessoryBridge}.
 * <p>
 * The 1.x (1.21.1) and 2.x (1.21.4) Accessories APIs we touch are
 * functionally identical; class kept separate from its sibling to isolate
 * any future divergence.
 * <p>
 * Slot ids encode as {@code accessories:<container-name>/<index>}.
 */
public final class Accessories2Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "accessories";
    private static final String NS = "accessories";
    private static final Logger LOG = LoggerFactory.getLogger(Accessories2Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        Optional<AccessoriesCapability> maybe = AccessoriesCapability.getOptionally(player);
        if (maybe.isEmpty()) return Map.of();

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<SlotId, byte[]> out = new LinkedHashMap<>();

        for (Map.Entry<String, AccessoriesContainer> entry : maybe.get().getContainers().entrySet()) {
            String type = entry.getKey();
            ExpandedSimpleContainer accessories = entry.getValue().getAccessories();
            int size = accessories.getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack stack = accessories.getItem(i);
                if (stack.isEmpty()) continue;
                out.put(new SlotId(NS, type + "/" + i),
                        ItemStackSerializer.toBytes(stack, regs));
            }
        }
        return out;
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        Optional<AccessoriesCapability> maybe = AccessoriesCapability.getOptionally(player);
        if (maybe.isEmpty()) {
            if (!stacks.isEmpty()) {
                LOG.debug("[{}] Accessories capability absent on player {}; skipping apply",
                        LockersMod.MOD_ID, player.getName().getString());
            }
            return;
        }

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<String, AccessoriesContainer> containers = maybe.get().getContainers();

        // MERGE semantics: only modify slots named in the saved loadout. Slots
        // the loadout doesn't mention are left untouched. Items in touched
        // slots are returned to the player's main inventory (or dropped if
        // it's full) so equipped gear is never silently deleted.
        for (Map.Entry<SlotId, byte[]> e : stacks.entrySet()) {
            SlotId sid = e.getKey();
            if (!NS.equals(sid.namespace())) continue;
            ParsedSlot p = parseSlotPath(sid.path());
            if (p == null) continue;

            AccessoriesContainer container = containers.get(p.type);
            ItemStack newStack = ItemStackSerializer.fromBytes(e.getValue(), regs);
            if (container == null) {
                returnToPlayerInventory(player, newStack);
                continue;
            }
            ExpandedSimpleContainer accessories = container.getAccessories();
            if (p.index < 0 || p.index >= accessories.getContainerSize()) {
                returnToPlayerInventory(player, newStack);
                continue;
            }
            ItemStack old = accessories.getItem(p.index);
            accessories.setItem(p.index, newStack);
            if (!old.isEmpty()) returnToPlayerInventory(player, old);
            container.markChanged();
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        Optional<AccessoriesCapability> maybe = AccessoriesCapability.getOptionally(player);
        if (maybe.isEmpty()) return Set.of();
        Set<SlotId> out = new HashSet<>();
        for (Map.Entry<String, AccessoriesContainer> entry : maybe.get().getContainers().entrySet()) {
            int size = entry.getValue().getAccessories().getContainerSize();
            for (int i = 0; i < size; i++) {
                out.add(new SlotId(NS, entry.getKey() + "/" + i));
            }
        }
        return out;
    }

    private static void returnToPlayerInventory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private record ParsedSlot(String type, int index) {}

    private static ParsedSlot parseSlotPath(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= 0 || slash == path.length() - 1) return null;
        try {
            int idx = Integer.parseInt(path.substring(slash + 1));
            return new ParsedSlot(path.substring(0, slash), idx);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
