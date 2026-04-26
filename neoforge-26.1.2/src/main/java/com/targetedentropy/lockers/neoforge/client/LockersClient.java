package com.targetedentropy.lockers.neoforge.client;

import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.registry.LockersMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = LockersMod.MOD_ID, value = Dist.CLIENT)
public final class LockersClient {

    private LockersClient() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LockersMenus.LOCKER.get(), LockerScreen::new);
    }
}
