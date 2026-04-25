package com.targetedentropy.lockers.neoforge.network;

import com.targetedentropy.lockers.neoforge.LockersMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all Lockers payload types on both sides. C2S packets run on the
 * main server thread (via the default handler thread) so the handlers can
 * freely mutate block-entity state.
 */
@EventBusSubscriber(modid = LockersMod.MOD_ID)
public final class LockersNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private LockersNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(LockersMod.MOD_ID).versioned(PROTOCOL_VERSION);

        r.playToServer(SaveLoadoutPacket.TYPE,   SaveLoadoutPacket.STREAM_CODEC,   LockerPayloadHandler::handleSave);
        r.playToServer(LoadLoadoutPacket.TYPE,   LoadLoadoutPacket.STREAM_CODEC,   LockerPayloadHandler::handleLoad);
        r.playToServer(RenameLoadoutPacket.TYPE, RenameLoadoutPacket.STREAM_CODEC, LockerPayloadHandler::handleRename);
        r.playToServer(DeleteLoadoutPacket.TYPE, DeleteLoadoutPacket.STREAM_CODEC, LockerPayloadHandler::handleDelete);
        r.playToServer(ChangeAccessPacket.TYPE,  ChangeAccessPacket.STREAM_CODEC,  LockerPayloadHandler::handleChangeAccess);

        r.playToClient(SyncLockerPacket.TYPE,    SyncLockerPacket.STREAM_CODEC,    LockerPayloadHandler::handleSync);
    }
}
