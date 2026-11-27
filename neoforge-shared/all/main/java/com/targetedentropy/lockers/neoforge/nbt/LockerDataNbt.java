package com.targetedentropy.lockers.neoforge.nbt;

import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import net.minecraft.nbt.CompoundTag;

/**
 * {@link LockerData} ↔ {@link CompoundTag}: the one encoding used both for
 * block-entity persistence and for carrying data on the dropped Locker item.
 */
public final class LockerDataNbt {

    private LockerDataNbt() {}

    public static CompoundTag encode(LockerData data) {
        return DataTagBridge.toCompoundTag(LockerDataCodec.encode(data));
    }

    public static LockerData decode(CompoundTag tag) {
        return LockerDataCodec.decode(DataTagBridge.fromCompoundTag(tag));
    }
}
