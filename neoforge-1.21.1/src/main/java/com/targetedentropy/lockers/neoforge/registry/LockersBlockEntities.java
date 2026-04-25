package com.targetedentropy.lockers.neoforge.registry;

import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.block.LockerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LockersBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LockersMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockerBlockEntity>> LOCKER =
            REGISTRY.register(
                    "locker",
                    () -> BlockEntityType.Builder
                            .of(LockerBlockEntity::new, LockersBlocks.LOCKER.get())
                            .build(null));

    private LockersBlockEntities() {}
}
