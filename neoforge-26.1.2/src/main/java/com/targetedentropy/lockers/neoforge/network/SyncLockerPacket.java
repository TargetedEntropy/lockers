package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.nbt.DataTagBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: full {@link LockerData} snapshot for the Locker at {@code pos}.
 * Sent when the menu opens and after every server-side mutation
 * (save / load / rename / delete).
 */
public record SyncLockerPacket(BlockPos pos, LockerData data) implements CustomPacketPayload {

    public static final Type<SyncLockerPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LockersMod.MOD_ID, "sync_locker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLockerPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncLockerPacket::pos,
                    ByteBufCodecs.COMPOUND_TAG, p -> DataTagBridge.toCompoundTag(LockerDataCodec.encode(p.data())),
                    (pos, tag) -> new SyncLockerPacket(pos, decode(tag)));

    private static LockerData decode(CompoundTag tag) {
        return LockerDataCodec.decode(DataTagBridge.fromCompoundTag(tag));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
