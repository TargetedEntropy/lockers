package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.neoforge.LockersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S: owner toggling the Locker's access control between OWNER_ONLY and PUBLIC.
 * Server validates the request via {@code AccessPolicy.canModifyAccess} which
 * is owner-only (or ops-bypass).
 */
public record ChangeAccessPacket(BlockPos pos, AccessControl access) implements CustomPacketPayload {

    public static final Type<ChangeAccessPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, "change_access"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeAccessPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChangeAccessPacket::pos,
                    ByteBufCodecs.STRING_UTF8, p -> p.access().name(),
                    (pos, name) -> new ChangeAccessPacket(pos, AccessControl.valueOf(name)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
