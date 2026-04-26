package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.block.LockerBlockEntity;
import com.targetedentropy.lockers.neoforge.client.LockerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side handlers for the four C2S mutation packets and the client-side
 * receiver for {@link SyncLockerPacket}. Kept as plain statics so registration
 * via method reference stays compact.
 */
public final class LockerPayloadHandler {

    private LockerPayloadHandler() {}

    public static void handleSave(SaveLoadoutPacket pkt, IPayloadContext ctx) {
        withServerBlockEntity(pkt.pos(), ctx, (be, sp) ->
                be.saveLoadoutFromPlayer(pkt.slot(), pkt.name(), sp));
    }

    public static void handleLoad(LoadLoadoutPacket pkt, IPayloadContext ctx) {
        withServerBlockEntity(pkt.pos(), ctx, (be, sp) ->
                be.loadLoadoutToPlayer(pkt.slot(), sp));
    }

    public static void handleRename(RenameLoadoutPacket pkt, IPayloadContext ctx) {
        withServerBlockEntity(pkt.pos(), ctx, (be, sp) ->
                be.renameLoadout(pkt.slot(), pkt.name(), sp));
    }

    public static void handleDelete(DeleteLoadoutPacket pkt, IPayloadContext ctx) {
        withServerBlockEntity(pkt.pos(), ctx, (be, sp) ->
                be.deleteLoadout(pkt.slot(), sp));
    }

    /**
     * Owner-only mutation — re-validated server-side via {@code AccessPolicy.canModifyAccess}
     * inside {@link LockerBlockEntity#changeAccess}. The {@link #withServerBlockEntity} guard
     * is intentionally not used here because that runs {@code canAccess} (read-only), which
     * permits PUBLIC viewers; modifying access requires the stricter check.
     */
    public static void handleChangeAccess(
            com.targetedentropy.lockers.neoforge.network.ChangeAccessPacket pkt,
            IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        BlockEntity raw = sp.level().getBlockEntity(pkt.pos());
        if (!(raw instanceof LockerBlockEntity be)) {
            ctx.disconnect(net.minecraft.network.chat.Component.literal(
                    "Locker block entity missing at " + pkt.pos()));
            return;
        }
        be.changeAccess(pkt.access(), sp);
    }

    /** Client-side: cache the latest LockerData on the open screen (if any). */
    public static void handleSync(SyncLockerPacket pkt, IPayloadContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof LockerScreen screen) {
            screen.onSync(pkt);
        }
    }

    private static void withServerBlockEntity(
            net.minecraft.core.BlockPos pos,
            IPayloadContext ctx,
            ServerOp op) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        BlockEntity raw = sp.level().getBlockEntity(pos);
        if (!(raw instanceof LockerBlockEntity be)) {
            ctx.disconnect(net.minecraft.network.chat.Component.literal(
                    "Locker block entity missing at " + pos));
            return;
        }
        if (!be.canAccess(sp)) {
            // Silently drop — do not leak information about the locker.
            return;
        }
        op.run(be, sp);
    }

    @FunctionalInterface
    private interface ServerOp {
        void run(LockerBlockEntity be, ServerPlayer sp);
    }
}
