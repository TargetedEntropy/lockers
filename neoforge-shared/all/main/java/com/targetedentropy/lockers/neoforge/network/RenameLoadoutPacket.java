package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.McCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: rename the loadout in {@code slot}. */
public record RenameLoadoutPacket(BlockPos pos, int slot, String name) implements CustomPacketPayload {

    public static final Type<RenameLoadoutPacket> TYPE =
            new Type<>(McCompat.id("rename_loadout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameLoadoutPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenameLoadoutPacket::pos,
                    ByteBufCodecs.VAR_INT,   RenameLoadoutPacket::slot,
                    ByteBufCodecs.STRING_UTF8, RenameLoadoutPacket::name,
                    RenameLoadoutPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
