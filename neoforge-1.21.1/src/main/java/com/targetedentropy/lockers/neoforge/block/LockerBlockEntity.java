package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.common.access.AccessPolicy;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.serialize.DataTag;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import com.targetedentropy.lockers.neoforge.nbt.DataTagBridge;
import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import java.time.Instant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent state for a single Locker block. Holds {@link LockerData} and
 * serializes it through the {@link DataTagBridge}. Kept small because unit
 * logic lives in {@code common}.
 */
public class LockerBlockEntity extends BlockEntity {

    private static final String TAG_ROOT = "lockerData";

    private LockerData data;

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(LockersBlockEntities.LOCKER.get(), pos, state);
    }

    /** Called by {@link LockerBlock#setPlacedBy} to stamp owner + creation time. */
    public void initOnPlace(ServerPlayer placer) {
        this.data = LockerData.fresh(
                placer.getUUID(),
                placer.getGameProfile().getName(),
                Instant.now());
        setChanged();
    }

    public LockerData getData() {
        if (data == null) {
            // Can happen on a chunk-load before loadAdditional populated us. Shouldn't
            // normally occur since loadAdditional precedes any access, but keep a
            // harmless sentinel so the NPE path isn't silently swallowed.
            throw new IllegalStateException("LockerBlockEntity accessed before data was populated");
        }
        return data;
    }

    public boolean hasData() {
        return data != null;
    }

    public void setData(LockerData data) {
        this.data = data;
        setChanged();
    }

    public boolean canAccess(ServerPlayer player) {
        if (data == null) return false;
        boolean isOp = player.hasPermissions(2);
        return AccessPolicy.canAccess(player.getUUID(), isOp, data, CommonConfig.defaults())
                .allowed();
    }

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
}
