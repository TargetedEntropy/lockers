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
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 26.1.2 port of {@link LockerBlockEntity}.
 * <p>
 * Notable per-version differences from the 1.21.1 / 1.21.4 siblings:
 * <ul>
 *   <li>{@code saveAdditional(CompoundTag, HolderLookup.Provider)} →
 *       {@code saveAdditional(ValueOutput)} (data-format-agnostic IO API).
 *       Same for {@code loadAdditional} → {@link ValueInput}.</li>
 *   <li>{@code GameProfile.getName()} → {@code GameProfile.name()} (authlib 7.x
 *       made the class a record).</li>
 *   <li>{@code player.hasPermissions(2)} → permission-set lookup against
 *       {@link Permissions#COMMANDS_GAMEMASTER} — vanilla level 2 ≈ ops with
 *       gamemaster command access.</li>
 *   <li>{@code displayClientMessage(component, true)} (overlay) →
 *       {@code sendOverlayMessage(component)}.</li>
 * </ul>
 */
public class LockerBlockEntity extends BlockEntity {

    private static final String TAG_ROOT = "lockerData";

    private LockerData data;

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(LockersBlockEntities.LOCKER.get(), pos, state);
    }

    public void initOnPlace(ServerPlayer placer) {
        this.data = LockerData.fresh(
                placer.getUUID(),
                placer.getGameProfile().name(),
                Instant.now());
        setChanged();
    }

    public Optional<LockerData> data() {
        return Optional.ofNullable(data);
    }

    public void replaceData(LockerData newData) {
        this.data = newData;
        setChanged();
    }

    public boolean canAccess(ServerPlayer player) {
        if (data == null) return false;
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        return AccessPolicy.canAccess(player.getUUID(), isOp, data, CommonConfig.defaults())
                .allowed();
    }

    // --- loadout operations ----------------------------------------------------

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

        if (equipment.isEmpty() && accessories.isEmpty()) {
            sp.sendOverlayMessage(Component.translatable("message.lockers.nothing_to_save"));
            return;
        }

        data.slot(slot).ifPresent(old -> returnLoadoutToInventory(sp, old, regs));

        Loadout lo = new Loadout(name, Instant.now(), equipment, accessories);
        this.data = data.withSlot(slot, lo);

        for (EquipmentSlot eqSlot : equipment.keySet()) {
            sp.setItemSlot(toMcSlot(eqSlot), ItemStack.EMPTY);
        }
        BridgeRegistry.get().clear(sp, accessories.keySet());

        setChanged();
        syncTo(sp);
        sp.sendOverlayMessage(Component.translatable("message.lockers.saved", name));
    }

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

        this.data = data.clearSlot(slot);
        setChanged();
        syncTo(sp);
        sp.sendOverlayMessage(Component.translatable("message.lockers.loaded", lo.name()));
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

    public void changeAccess(com.targetedentropy.lockers.common.model.AccessControl newAccess,
                             ServerPlayer sp) {
        if (data == null) return;
        boolean isOp = sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (!AccessPolicy.canModifyAccess(sp.getUUID(), isOp, data, CommonConfig.defaults())
                .allowed()) {
            return;
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

    // --- NBT persistence (26.1.2 ValueOutput / ValueInput) ---------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (data != null) {
            DataTag.Compound encoded = LockerDataCodec.encode(data);
            CompoundTag tag = DataTagBridge.toCompoundTag(encoded);
            output.store(TAG_ROOT, CompoundTag.CODEC, tag);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read(TAG_ROOT, CompoundTag.CODEC).ifPresent(tag -> {
            DataTag.Compound encoded = DataTagBridge.fromCompoundTag(tag);
            this.data = LockerDataCodec.decode(encoded);
        });
    }

    // --- helpers ---------------------------------------------------------------

    private static void putIfNotEmpty(Map<EquipmentSlot, byte[]> out, EquipmentSlot key,
                                      ItemStack stack, HolderLookup.Provider regs) {
        if (stack.isEmpty()) return;
        out.put(key, ItemStackSerializer.toBytes(stack, regs));
    }

    private static void applySlot(ServerPlayer sp,
                                  net.minecraft.world.entity.EquipmentSlot vanillaSlot,
                                  byte[] bytes,
                                  HolderLookup.Provider regs) {
        if (bytes == null) return;
        ItemStack newStack = ItemStackSerializer.fromBytes(bytes, regs);
        ItemStack old = sp.getItemBySlot(vanillaSlot);
        sp.setItemSlot(vanillaSlot, newStack);
        if (!old.isEmpty() && vanillaSlot.getType() != Type.HUMANOID_ARMOR) {
            if (!sp.getInventory().add(old)) {
                sp.drop(old, false);
            }
        } else if (!old.isEmpty()) {
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

    private static net.minecraft.world.entity.EquipmentSlot toMcSlot(EquipmentSlot s) {
        return switch (s) {
            case HEAD    -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case CHEST   -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGS    -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case FEET    -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        };
    }

    private static void returnLoadoutToInventory(ServerPlayer sp, Loadout lo, HolderLookup.Provider regs) {
        for (byte[] bytes : lo.equipment().values()) {
            addOrDrop(sp, ItemStackSerializer.fromBytes(bytes, regs));
        }
        for (byte[] bytes : lo.accessories().values()) {
            addOrDrop(sp, ItemStackSerializer.fromBytes(bytes, regs));
        }
    }

    private static void addOrDrop(ServerPlayer sp, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!sp.getInventory().add(stack)) sp.drop(stack, false);
    }
}
