package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C2S: clear the loadout in {@code slot}. */
public record DeleteLoadoutPacket(BlockPos pos, int slot) implements CustomPacketPayload {

    public static final Type<DeleteLoadoutPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LockersMod.MOD_ID, "delete_loadout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteLoadoutPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DeleteLoadoutPacket::pos,
                    ByteBufCodecs.VAR_INT,   DeleteLoadoutPacket::slot,
                    DeleteLoadoutPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
