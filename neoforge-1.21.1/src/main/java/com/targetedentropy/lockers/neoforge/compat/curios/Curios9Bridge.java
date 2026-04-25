package com.targetedentropy.lockers.neoforge.compat.curios;

import com.targetedentropy.lockers.common.compat.AccessoryBridge;
import com.targetedentropy.lockers.common.model.SlotId;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.nbt.ItemStackSerializer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Curios 9.x (1.21.1) implementation of {@link AccessoryBridge}.
 * <p>
 * Selected at runtime only when Curios is loaded. The Curios 9 and 10 public
 * APIs we touch ({@code CuriosApi.getCuriosInventory},
 * {@code ICuriosItemHandler.getCurios}, {@code IDynamicStackHandler}) are
 * identical for these operations, so the 1.21.4 sibling
 * {@link com.targetedentropy.lockers.neoforge.compat.curios.Curios10Bridge}
 * is structurally a copy of this class.
 * <p>
 * Slot ids encode as {@code curios:<type>/<index>}.
 */
public final class Curios9Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "curios";
    private static final String NS = "curios";
    private static final Logger LOG = LoggerFactory.getLogger(Curios9Bridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) return Map.of();

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<SlotId, byte[]> out = new LinkedHashMap<>();

        for (Map.Entry<String, ICurioStacksHandler> entry : maybe.get().getCurios().entrySet()) {
            String type = entry.getKey();
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                out.put(new SlotId(NS, type + "/" + i),
                        ItemStackSerializer.toBytes(stack, regs));
            }
        }
        return out;
    }

    @Override
    public void apply(ServerPlayer player, Map<SlotId, byte[]> stacks) {
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) {
            if (!stacks.isEmpty()) {
                LOG.debug("[{}] Curios inventory absent on player {}; skipping accessory apply",
                        LockersMod.MOD_ID, player.getName().getString());
            }
            return;
        }

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<String, ICurioStacksHandler> handlers = maybe.get().getCurios();

        // MERGE semantics: only modify slots named in the saved loadout.
        // Slots the loadout doesn't mention are left untouched. Whatever was
        // previously in a touched slot gets returned to the player's main
        // inventory (or dropped if full) so we never silently delete gear.
        for (Map.Entry<SlotId, byte[]> e : stacks.entrySet()) {
            SlotId sid = e.getKey();
            if (!NS.equals(sid.namespace())) continue;
            ParsedSlot p = parseSlotPath(sid.path());
            if (p == null) continue;

            ICurioStacksHandler handler = handlers.get(p.type);
            ItemStack newStack = ItemStackSerializer.fromBytes(e.getValue(), regs);
            if (handler == null) {
                // Slot type unknown to this player (e.g. dimension-restricted).
                returnToPlayerInventory(player, newStack);
                continue;
            }
            IDynamicStackHandler dsh = handler.getStacks();
            if (p.index < 0 || p.index >= dsh.getSlots()) {
                returnToPlayerInventory(player, newStack);
                continue;
            }
            ItemStack old = dsh.getStackInSlot(p.index);
            dsh.setStackInSlot(p.index, newStack);
            if (!old.isEmpty()) returnToPlayerInventory(player, old);
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) return Set.of();
        Set<SlotId> out = new HashSet<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : maybe.get().getCurios().entrySet()) {
            ICurioStacksHandler h = entry.getValue();
            IItemHandlerModifiable stacks = h.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                out.add(new SlotId(NS, entry.getKey() + "/" + i));
            }
        }
        return out;
    }

    @Override
    public void clear(ServerPlayer player, Set<SlotId> slotIds) {
        if (slotIds.isEmpty()) return;
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) return;
        Map<String, ICurioStacksHandler> handlers = maybe.get().getCurios();
        for (SlotId sid : slotIds) {
            if (!NS.equals(sid.namespace())) continue;
            ParsedSlot p = parseSlotPath(sid.path());
            if (p == null) continue;
            ICurioStacksHandler handler = handlers.get(p.type);
            if (handler == null) continue;
            IDynamicStackHandler dsh = handler.getStacks();
            if (p.index < 0 || p.index >= dsh.getSlots()) continue;
            dsh.setStackInSlot(p.index, ItemStack.EMPTY);
        }
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
