package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.block.LockerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersBlocks {

    public static final DeferredRegister.Blocks REGISTRY =
            DeferredRegister.createBlocks(LockersMod.MOD_ID);

    // registerBlock (not register) so NeoForge stamps the registry id onto the
    // Properties first: MC 1.21.2+ throws "Block id not set" without it.
    public static final DeferredBlock<LockerBlock> LOCKER = REGISTRY.registerBlock(
            "locker",
            props -> new LockerBlock(props
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private LockersBlocks() {}
}
