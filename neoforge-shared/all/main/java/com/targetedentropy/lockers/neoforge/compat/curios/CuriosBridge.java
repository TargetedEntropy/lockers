package com.targetedentropy.lockers.neoforge.compat.curios;

import static com.targetedentropy.lockers.neoforge.player.ServerPlayerEquipment.giveOrDrop;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Curios implementation of {@link AccessoryBridge}, compiled unchanged against
 * Curios 9 (MC 1.21.1), 10 (1.21.4) and 15 (26.1.2). The API surface it uses
 * ({@code CuriosApi.getCuriosInventory}, {@code ICuriosItemHandler.getCurios},
 * {@code IDynamicStackHandler}) is the same in all three.
 * <p>
 * Slot ids encode as {@code curios:<type>/<index>}.
 */
public final class CuriosBridge implements AccessoryBridge<ServerPlayer, ItemStack> {

    public static final String ID = "curios";
    private static final String NS = "curios";
    private static final Logger LOG = LoggerFactory.getLogger(CuriosBridge.class);

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public Map<SlotId, byte[]> capture(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) return Map.of();

        HolderLookup.Provider regs = player.level().registryAccess();
        Map<SlotId, byte[]> out = new LinkedHashMap<>();

        for (Map.Entry<String, ICurioStacksHandler> entry : maybe.get().getCurios().entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                out.put(SlotId.indexed(NS, entry.getKey(), i), ItemStackSerializer.toBytes(stack, regs));
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
            Optional<SlotId.Indexed> parsed = sid.parseIndexed();
            if (parsed.isEmpty()) continue;
            SlotId.Indexed p = parsed.get();

            ItemStack newStack = ItemStackSerializer.fromBytes(e.getValue(), regs);
            IDynamicStackHandler dsh = stacksFor(handlers, p);
            if (dsh == null) {
                // Slot unknown to this player (e.g. dimension-restricted): hand the item back.
                giveOrDrop(player, newStack);
                continue;
            }
            ItemStack old = dsh.getStackInSlot(p.index());
            dsh.setStackInSlot(p.index(), newStack);
            giveOrDrop(player, old);
        }
    }

    @Override
    public Set<SlotId> knownSlots(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybe = CuriosApi.getCuriosInventory(player);
        if (maybe.isEmpty()) return Set.of();
        Set<SlotId> out = new HashSet<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : maybe.get().getCurios().entrySet()) {
            int size = entry.getValue().getStacks().getSlots();
            for (int i = 0; i < size; i++) {
                out.add(SlotId.indexed(NS, entry.getKey(), i));
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
            sid.parseIndexed().ifPresent(p -> {
                IDynamicStackHandler dsh = stacksFor(handlers, p);
                if (dsh != null) dsh.setStackInSlot(p.index(), ItemStack.EMPTY);
            });
        }
    }

    /** The stack handler holding slot {@code p}, or {@code null} if the player has no such slot. */
    private static IDynamicStackHandler stacksFor(Map<String, ICurioStacksHandler> handlers,
                                                  SlotId.Indexed p) {
        ICurioStacksHandler handler = handlers.get(p.group());
        if (handler == null) return null;
        IDynamicStackHandler dsh = handler.getStacks();
        return p.index() >= 0 && p.index() < dsh.getSlots() ? dsh : null;
    }
}
