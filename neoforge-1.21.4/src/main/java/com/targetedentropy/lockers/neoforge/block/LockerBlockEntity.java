package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.common.access.AccessPolicy;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.model.SlotId;
import com.targetedentropy.lockers.common.serialize.DataTag;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import com.targetedentropy.lockers.neoforge.compat.BridgeRegistry;
import com.targetedentropy.lockers.neoforge.nbt.DataTagBridge;
import com.targetedentropy.lockers.neoforge.nbt.ItemStackSerializer;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class LockerBlockEntity extends BlockEntity {

    private static final String TAG_ROOT = "lockerData";

    private LockerData data;

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(LockersBlockEntities.LOCKER.get(), pos, state);
    }

    // --- placement & accessors -------------------------------------------------

    public void initOnPlace(ServerPlayer placer) {
        this.data = LockerData.fresh(
                placer.getUUID(),
                placer.getGameProfile().getName(),
                Instant.now());
        setChanged();
    }

    /**
     * Returns the persisted {@link LockerData} if present.
     * <p>
     * Empty during the brief window between block placement and the first
     * {@link #initOnPlace} call (loaded chunks always have data because
     * {@link #loadAdditional} populates it). Callers MUST handle the empty
     * case rather than {@code .get()}-ing.
     */
    public Optional<LockerData> data() {
        return Optional.ofNullable(data);
    }

    /**
     * Test-only: swap in a {@link LockerData} directly. Production code paths
     * should call {@link #initOnPlace} / {@link #saveLoadoutFromPlayer} etc.
     * Named to avoid colliding with the inherited
     * {@code BlockEntity.setData(AttachmentType, T)} overload.
     */
    public void replaceData(LockerData newData) {
        this.data = newData;
        setChanged();
    }

    public boolean canAccess(ServerPlayer player) {
        if (data == null) return false;
        boolean isOp = player.hasPermissions(2);
        return AccessPolicy.canAccess(player.getUUID(), isOp, data, CommonConfig.defaults())
                .allowed();
    }

    // --- loadout operations ----------------------------------------------------

    /** Capture player's current armor + offhand (+ accessories via bridge) into {@code slot}. */
    public void saveLoadoutFromPlayer(int slot, String rawName, ServerPlayer sp) {
        if (data == null) return;
        String name = sanitizeName(rawName, "Loadout " + (slot + 1));
        HolderLookup.Provider regs = sp.level().registryAccess();

        Map<EquipmentSlot, byte[]> equipment = new EnumMap<>(EquipmentSlot.class);
        putIfNotEmpty(equipment, EquipmentSlot.HEAD, sp.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.HEAD), regs);
        putIfNotEmpty(equipment, EquipmentSlot.CHEST, sp.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.CHEST), regs);
        putIfNotEmpty(equipment, EquipmentSlot.LEGS, sp.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.LEGS), regs);
        putIfNotEmpty(equipment, EquipmentSlot.FEET, sp.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.FEET), regs);
        putIfNotEmpty(equipment, EquipmentSlot.OFFHAND, sp.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.OFFHAND), regs);

        Map<SlotId, byte[]> accessories = new LinkedHashMap<>(BridgeRegistry.get().capture(sp));

        // Reject empty saves: nothing equipped, no accessories. Avoids accidental
        // wipe of an existing loadout when the player meant to load it.
        if (equipment.isEmpty() && accessories.isEmpty()) {
            sp.displayClientMessage(
                    Component.translatable("message.lockers.nothing_to_save"), true);
            return;
        }

        Loadout lo = new Loadout(name, Instant.now(), equipment, accessories);
        this.data = data.withSlot(slot, lo);
        setChanged();
        syncTo(sp);
        sp.displayClientMessage(
                Component.translatable("message.lockers.saved", name), true);
    }

    /** Apply loadout in {@code slot} to the player (swapping in whatever was equipped). */
    public void loadLoadoutToPlayer(int slot, ServerPlayer sp) {
        if (data == null) return;
        Optional<Loadout> maybe = data.slot(slot);
        if (maybe.isEmpty()) return;
        Loadout lo = maybe.get();
        HolderLookup.Provider regs = sp.level().registryAccess();

        applySlot(sp, net.minecraft.world.entity.EquipmentSlot.HEAD,    lo.equipment().get(EquipmentSlot.HEAD),    regs);
        applySlot(sp, net.minecraft.world.entity.EquipmentSlot.CHEST,   lo.equipment().get(EquipmentSlot.CHEST),   regs);
        applySlot(sp, net.minecraft.world.entity.EquipmentSlot.LEGS,    lo.equipment().get(EquipmentSlot.LEGS),    regs);
        applySlot(sp, net.minecraft.world.entity.EquipmentSlot.FEET,    lo.equipment().get(EquipmentSlot.FEET),    regs);
        applySlot(sp, net.minecraft.world.entity.EquipmentSlot.OFFHAND, lo.equipment().get(EquipmentSlot.OFFHAND), regs);

        BridgeRegistry.get().apply(sp, lo.accessories());

        sp.displayClientMessage(
                Component.translatable("message.lockers.loaded", lo.name()), true);
    }

    public void renameLoadout(int slot, String rawName, ServerPlayer sp) {
        if (data == null) return;
        Optional<Loadout> maybe = data.slot(slot);
        if (maybe.isEmpty()) return;
        String name = sanitizeName(rawName, maybe.get().name());
        this.data = data.withSlot(slot, maybe.get().withName(name));
        setChanged();
        syncTo(sp);
    }

    public void deleteLoadout(int slot, ServerPlayer sp) {
        if (data == null) return;
        if (data.slot(slot).isEmpty()) return;
        this.data = data.clearSlot(slot);
        setChanged();
        syncTo(sp);
    }

    /**
     * Owner-only access-control toggle. Validated against
     * {@link AccessPolicy#canModifyAccess} which restricts to the owner (or
     * ops with {@code opsBypassOwnership}); silently dropped otherwise.
     */
    public void changeAccess(com.targetedentropy.lockers.common.model.AccessControl newAccess,
                             ServerPlayer sp) {
        if (data == null) return;
        boolean isOp = sp.hasPermissions(2);
        if (!AccessPolicy.canModifyAccess(sp.getUUID(), isOp, data, CommonConfig.defaults())
                .allowed()) {
            return;  // silently drop — never leak that the locker exists
        }
        this.data = data.withAccess(newAccess);
        setChanged();
        syncTo(sp);
    }

    // --- sync ------------------------------------------------------------------

    public void syncTo(ServerPlayer sp) {
        if (data == null) return;
        PacketDistributor.sendToPlayer(sp, new SyncLockerPacket(getBlockPos(), data));
    }

    // --- NBT persistence -------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (data != null) {
            DataTag.Compound encoded = LockerDataCodec.encode(data);
            tag.put(TAG_ROOT, DataTagBridge.toCompoundTag(encoded));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ROOT)) {
            DataTag.Compound encoded = DataTagBridge.fromCompoundTag(tag.getCompound(TAG_ROOT));
            this.data = LockerDataCodec.decode(encoded);
        }
    }

    // --- helpers ---------------------------------------------------------------

    private static void putIfNotEmpty(Map<EquipmentSlot, byte[]> out, EquipmentSlot key,
                                      ItemStack stack, HolderLookup.Provider regs) {
        if (stack.isEmpty()) return;
        out.put(key, ItemStackSerializer.toBytes(stack, regs));
    }

    /**
     * Set the equipment slot. If the slot in the loadout is absent, we leave
     * the player's current item in place (do not clear). This is a deliberate
     * "merge" semantics — lets a player save chest+legs only and apply without
     * stripping head/feet.
     */
    private static void applySlot(ServerPlayer sp,
                                  net.minecraft.world.entity.EquipmentSlot vanillaSlot,
                                  byte[] bytes,
                                  HolderLookup.Provider regs) {
        if (bytes == null) return;
        ItemStack newStack = ItemStackSerializer.fromBytes(bytes, regs);
        ItemStack old = sp.getItemBySlot(vanillaSlot);
        sp.setItemSlot(vanillaSlot, newStack);
        if (!old.isEmpty() && vanillaSlot.getType() != Type.HUMANOID_ARMOR) {
            // Offhand: give the previous item back so we never silently delete.
            if (!sp.getInventory().add(old)) {
                sp.drop(old, /*dropAround=*/false);
            }
        } else if (!old.isEmpty()) {
            // Armor slot: return the replaced armor to the player's main inventory.
            if (!sp.getInventory().add(old)) {
                sp.drop(old, false);
            }
        }
    }

    private static String sanitizeName(String raw, String fallback) {
        if (raw == null) return fallback;
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) return fallback;
        if (trimmed.length() > Loadout.MAX_NAME_LENGTH) {
            return trimmed.substring(0, Loadout.MAX_NAME_LENGTH);
        }
        return trimmed;
    }
}
