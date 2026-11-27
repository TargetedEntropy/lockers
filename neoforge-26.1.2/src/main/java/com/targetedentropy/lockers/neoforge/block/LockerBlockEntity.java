package com.targetedentropy.lockers.neoforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * MC 26.1.2 persistence for the Locker block entity: 26.1 replaced the
 * {@code CompoundTag}/{@code HolderLookup.Provider} pair with the
 * format-agnostic {@link ValueOutput}/{@link ValueInput} API.
 */
public class LockerBlockEntity extends LockerBlockEntityBase {

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        encodedData().ifPresent(encoded -> output.store(TAG_ROOT, CompoundTag.CODEC, encoded));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read(TAG_ROOT, CompoundTag.CODEC).ifPresent(this::readEncodedData);
    }
}
