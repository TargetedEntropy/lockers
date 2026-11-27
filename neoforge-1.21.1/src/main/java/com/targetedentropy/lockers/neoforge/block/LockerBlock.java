package com.targetedentropy.lockers.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** MC 1.21.1 Locker block: the version-specific overrides on top of {@link LockerBlockBase}. */
public class LockerBlock extends LockerBlockBase {

    public LockerBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<LockerBlock> codec() {
        return simpleCodec(LockerBlock::new);
    }

    /** Pick-block always carries the locker's data on 1.21.1. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return cloneWithData(level, pos, state);
    }
}
