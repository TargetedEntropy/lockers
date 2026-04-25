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
 * Curios 10.x (1.21.4+) implementation of {@link AccessoryBridge}.
 * <p>
 * The 9.x and 10.x public APIs we touch — {@code CuriosApi.getCuriosInventory},
 * {@code ICuriosItemHandler.getCurios}, {@code IDynamicStackHandler} — are
 * functionally identical for these operations, so this class is structurally
 * a copy of {@code Curios9Bridge} in the 1.21.1 sibling module. They are
 * kept separate to isolate any future divergence (e.g. capability registration
 * differences, NBT helper signatures).
 * <p>
 * Slot ids encode as {@code curios:<type>/<index>}.
 */
public final class Curios10Bridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "curios";
    private static final String NS = "curios";
    private static final Logger LOG = LoggerFactory.getLogger(Curios10Bridge.class);

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

        // First pass: clear out current slots — the saved loadout is authoritative.
        // Items previously equipped get returned to the player's main inventory
        // (or dropped if it's full) so we never silently delete gear.
        for (ICurioStacksHandler h : handlers.values()) {
            IDynamicStackHandler s = h.getStacks();
            for (int i = 0; i < s.getSlots(); i++) {
                ItemStack old = s.getStackInSlot(i);
                if (old.isEmpty()) continue;
                s.setStackInSlot(i, ItemStack.EMPTY);
                returnToPlayerInventory(player, old);
            }
        }

        // Second pass: install the saved stacks into matching slots.
        for (Map.Entry<SlotId, byte[]> e : stacks.entrySet()) {
            SlotId sid = e.getKey();
            if (!NS.equals(sid.namespace())) continue;
            ParsedSlot p = parseSlotPath(sid.path());
            if (p == null) continue;

            ICurioStacksHandler handler = handlers.get(p.type);
            if (handler == null) {
                // Slot type unknown to this player (e.g. dimension-restricted) — return the item.
                returnToPlayerInventory(player, ItemStackSerializer.fromBytes(e.getValue(), regs));
                continue;
            }
            IDynamicStackHandler dsh = handler.getStacks();
            if (p.index < 0 || p.index >= dsh.getSlots()) {
                returnToPlayerInventory(player, ItemStackSerializer.fromBytes(e.getValue(), regs));
                continue;
            }
            dsh.setStackInSlot(p.index, ItemStackSerializer.fromBytes(e.getValue(), regs));
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
