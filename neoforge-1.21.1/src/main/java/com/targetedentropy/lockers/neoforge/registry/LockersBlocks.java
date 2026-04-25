package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.block.LockerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersBlocks {

    public static final DeferredRegister.Blocks REGISTRY =
            DeferredRegister.createBlocks(LockersMod.MOD_ID);

    public static final DeferredBlock<LockerBlock> LOCKER = REGISTRY.register(
            "locker",
            () -> new LockerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private LockersBlocks() {}
}
