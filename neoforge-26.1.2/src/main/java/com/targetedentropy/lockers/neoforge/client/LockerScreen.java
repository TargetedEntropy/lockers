package com.targetedentropy.lockers.neoforge.client;

import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 26.1.2 STUB.
 * <p>
 * The 26.1.2 client rendering pipeline replaced {@code GuiGraphics} with
 * {@code GuiGraphicsExtractor} (extract-then-render paradigm) and made
 * {@code imageWidth}/{@code imageHeight} final fields set via a 5-arg
 * super-constructor. The pre-26.1.2 {@code LockerScreen} (used in the
 * 1.21.1 / 1.21.4 modules) imperatively drew widgets and called
 * {@code GuiGraphics.fill / drawString}; that whole approach no longer
 * works on this MC line.
 * <p>
 * This stub opens as a blank container screen and consumes
 * {@link SyncLockerPacket} on the wire without crashing, so the
 * server-side block + BE + networking can be exercised end-to-end.
 * The full GUI port is tracked as a follow-up.
 */
public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    public LockerScreen(LockerMenu menu, Inventory playerInv, Component title) {
        // 26.1.2: imageWidth / imageHeight are final and must be passed via the
        // 5-arg constructor. 256x184 matches the pre-26.1.2 layout.
        super(menu, playerInv, title, 256, 184);
        this.inventoryLabelY = -10000;
    }

    /** Called by the network handler — placeholder so packets don't crash. */
    public void onSync(SyncLockerPacket pkt) {
        // TODO: render the loadout grid once the GuiGraphicsExtractor port lands.
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return super.mouseClicked(event, consumed);
    }
}
