package com.targetedentropy.lockers.neoforge.block;

import com.mojang.serialization.MapCodec;
import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** MC 1.21.4 Locker block: the version-specific overrides on top of {@link LockerBlockBase}. */
public class LockerBlock extends LockerBlockBase {

    public LockerBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<LockerBlock> codec() {
        return simpleCodec(LockerBlock::new);
    }

    /** Since 1.21.4 the caller says whether to include data (false for survival pick-block). */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return includeData ? cloneWithData(level, pos, state) : new ItemStack(LockersItems.LOCKER_ITEM.get());
    }
}
