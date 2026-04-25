package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: apply the loadout in {@code slot} to the player's armor+offhand. */
public record LoadLoadoutPacket(BlockPos pos, int slot) implements CustomPacketPayload {

    public static final Type<LoadLoadoutPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, "load_loadout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoadLoadoutPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LoadLoadoutPacket::pos,
                    ByteBufCodecs.VAR_INT,   LoadLoadoutPacket::slot,
                    LoadLoadoutPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
