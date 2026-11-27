package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.common.access.AccessPolicy;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.loadout.LoadoutOutcome;
import com.targetedentropy.lockers.common.loadout.LoadoutService;
import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.McCompat;
import com.targetedentropy.lockers.neoforge.compat.BridgeRegistry;
import com.targetedentropy.lockers.neoforge.nbt.LockerDataNbt;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import com.targetedentropy.lockers.neoforge.player.ServerPlayerEquipment;
import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Everything the Locker's block entity does except NBT persistence, whose
 * override signatures differ per MC version. Each version's
 * {@code LockerBlockEntity} extends this and implements
 * {@code saveAdditional}/{@code loadAdditional} via {@link #encodedData()} and
 * {@link #readEncodedData(CompoundTag)}.
 * <p>
 * Loadout rules live in {@link LoadoutService}; this class only supplies the
 * player adapters, persists the returned state, syncs it and shows feedback.
 */
public abstract class LockerBlockEntityBase extends BlockEntity {

    protected static final String TAG_ROOT = "lockerData";

    private LockerData data;

    protected LockerBlockEntityBase(BlockPos pos, BlockState state) {
        super(LockersBlockEntities.LOCKER.get(), pos, state);
    }

    public void initOnPlace(ServerPlayer placer) {
        this.data = LockerData.fresh(placer.getUUID(), McCompat.profileName(placer), Instant.now());
        setChanged();
    }

    /**
     * The persisted {@link LockerData}, if present. Empty only in the brief
     * window between block placement and {@link #initOnPlace}; callers must
     * handle that rather than {@code .get()}-ing.
     */
    public Optional<LockerData> data() {
        return Optional.ofNullable(data);
    }

    /**
     * Replace the state wholesale: used when a Locker is re-placed carrying
     * saved data, and by GameTests. Named to avoid colliding with the
     * inherited {@code BlockEntity.setData(AttachmentType, T)}.
     */
    public void replaceData(LockerData newData) {
        this.data = newData;
        setChanged();
    }

    public boolean canAccess(ServerPlayer player) {
        return data != null && AccessPolicy.canAccess(
                player.getUUID(), McCompat.isOp(player), data, CommonConfig.defaults()).allowed();
    }

    // --- loadout operations (rules in LoadoutService) ------------------------

    /** Move the player's gear into {@code slot}; see {@link LoadoutService#save}. */
    public void saveLoadoutFromPlayer(int slot, String rawName, ServerPlayer sp) {
        run(sp, (svc, d) -> svc.save(d, slot, rawName, sp));
    }

    /** Move {@code slot}'s gear onto the player; see {@link LoadoutService#load}. */
    public void loadLoadoutToPlayer(int slot, ServerPlayer sp) {
        run(sp, (svc, d) -> svc.load(d, slot, sp));
    }

    public void renameLoadout(int slot, String rawName, ServerPlayer sp) {
        run(sp, (svc, d) -> svc.rename(d, slot, rawName));
    }

    public void deleteLoadout(int slot, ServerPlayer sp) {
        run(sp, (svc, d) -> svc.delete(d, slot));
    }

    /**
     * Owner-only access toggle, validated against
     * {@link AccessPolicy#canModifyAccess}. Silently dropped otherwise so a
     * non-owner learns nothing about the locker.
     */
    public void changeAccess(AccessControl newAccess, ServerPlayer sp) {
        if (data == null) return;
        if (!AccessPolicy.canModifyAccess(sp.getUUID(), McCompat.isOp(sp), data, CommonConfig.defaults())
                .allowed()) {
            return;
        }
        commit(data.withAccess(newAccess), sp);
    }

    public void syncTo(ServerPlayer sp) {
        if (data == null) return;
        PacketDistributor.sendToPlayer(sp, new SyncLockerPacket(getBlockPos(), data));
    }

    @FunctionalInterface
    private interface Operation {
        LoadoutOutcome apply(LoadoutService<ServerPlayer> service, LockerData data);
    }

    private void run(ServerPlayer sp, Operation op) {
        if (data == null) return;
        LoadoutService<ServerPlayer> service =
                new LoadoutService<>(ServerPlayerEquipment.INSTANCE, BridgeRegistry.get(), Instant::now);
        LoadoutOutcome outcome = op.apply(service, data);
        outcome.updated().ifPresent(d -> commit(d, sp));
        outcome.feedback().ifPresent(f ->
                McCompat.overlay(sp, Component.translatable(f.translationKey(), f.args().toArray())));
    }

    private void commit(LockerData newData, ServerPlayer sp) {
        this.data = newData;
        setChanged();
        syncTo(sp);
    }

    // --- persistence hooks for the per-version subclass ----------------------

    /** The state as NBT, or empty when there is none to save. */
    protected Optional<CompoundTag> encodedData() {
        return data().map(LockerDataNbt::encode);
    }

    protected void readEncodedData(CompoundTag tag) {
        this.data = LockerDataNbt.decode(tag);
    }
}
