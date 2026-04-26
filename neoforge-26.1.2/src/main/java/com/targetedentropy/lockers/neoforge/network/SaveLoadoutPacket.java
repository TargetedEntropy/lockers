package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C2S: player wants to save their current armor+offhand into {@code slot}. */
public record SaveLoadoutPacket(BlockPos pos, int slot, String name) implements CustomPacketPayload {

    public static final Type<SaveLoadoutPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LockersMod.MOD_ID, "save_loadout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveLoadoutPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SaveLoadoutPacket::pos,
                    ByteBufCodecs.VAR_INT,   SaveLoadoutPacket::slot,
                    ByteBufCodecs.STRING_UTF8, SaveLoadoutPacket::name,
                    SaveLoadoutPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
