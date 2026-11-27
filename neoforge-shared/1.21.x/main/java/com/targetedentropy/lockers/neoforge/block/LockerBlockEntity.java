package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.neoforge.McCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** MC 1.21.x persistence for the Locker block entity: {@code CompoundTag}-based save/load. */
public class LockerBlockEntity extends LockerBlockEntityBase {

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        encodedData().ifPresent(encoded -> tag.put(TAG_ROOT, encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        McCompat.compound(tag, TAG_ROOT).ifPresent(this::readEncodedData);
    }
}
