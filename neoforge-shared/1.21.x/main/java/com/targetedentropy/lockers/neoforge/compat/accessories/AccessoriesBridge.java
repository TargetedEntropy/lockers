package com.targetedentropy.lockers.neoforge.compat.accessories;

import static com.targetedentropy.lockers.neoforge.player.ServerPlayerEquipment.giveOrDrop;

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
 * Accessories implementation of {@link AccessoryBridge}, compiled unchanged
 * against Accessories 1.1 (MC 1.21.1) and 1.2 (1.21.4). Not built for 26.1.2,
 * where no Accessories release exists yet.
 * <p>
 * Slot ids encode as {@code accessories:<container-name>/<index>}.
 */
public final class AccessoriesBridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "accessories";
    private static final String NS = "accessories";
    private static final Logger LOG = LoggerFactory.getLogger(AccessoriesBridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        Optional<AccessoriesCapability> maybe = AccessoriesCapability.getOptionally(player);
        if (maybe.isEmpty()) return Map.of();

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<SlotId, byte[]> out = new LinkedHashMap<>();

        for (Map.Entry<String, AccessoriesContainer> entry : maybe.get().getContainers().entrySet()) {
            ExpandedSimpleContainer accessories = entry.getValue().getAccessories();
            for (int i = 0; i < accessories.getContainerSize(); i++) {
                ItemStack stack = accessories.getItem(i);
                if (stack.isEmpty()) continue;
                out.put(SlotId.indexed(NS, entry.getKey(), i), ItemStackSerializer.toBytes(stack, regs));
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
            Optional<SlotId.Indexed> parsed = sid.parseIndexed();
            if (parsed.isEmpty()) continue;
            SlotId.Indexed p = parsed.get();

            ItemStack newStack = ItemStackSerializer.fromBytes(e.getValue(), regs);
            AccessoriesContainer container = containerFor(containers, p);
            if (container == null) {
                giveOrDrop(player, newStack);
                continue;
            }
            ExpandedSimpleContainer accessories = container.getAccessories();
            ItemStack old = accessories.getItem(p.index());
            accessories.setItem(p.index(), newStack);
            giveOrDrop(player, old);
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
                out.add(SlotId.indexed(NS, entry.getKey(), i));
            }
        }
        return out;
    }

    @Override
    public void clear(ServerPlayer player, Set<SlotId> slotIds) {
        if (slotIds.isEmpty()) return;
        Optional<AccessoriesCapability> maybe = AccessoriesCapability.getOptionally(player);
        if (maybe.isEmpty()) return;
        Map<String, AccessoriesContainer> containers = maybe.get().getContainers();
        for (SlotId sid : slotIds) {
            if (!NS.equals(sid.namespace())) continue;
            sid.parseIndexed().ifPresent(p -> {
                AccessoriesContainer container = containerFor(containers, p);
                if (container == null) return;
                container.getAccessories().setItem(p.index(), ItemStack.EMPTY);
                container.markChanged();
            });
        }
    }

    /** The container holding slot {@code p}, or {@code null} if the player has no such slot. */
    private static AccessoriesContainer containerFor(Map<String, AccessoriesContainer> containers,
                                                     SlotId.Indexed p) {
        AccessoriesContainer container = containers.get(p.group());
        if (container == null) return null;
        int size = container.getAccessories().getContainerSize();
        return p.index() >= 0 && p.index() < size ? container : null;
    }
}
