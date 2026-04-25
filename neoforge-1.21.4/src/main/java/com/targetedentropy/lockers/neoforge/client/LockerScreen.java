package com.targetedentropy.lockers.neoforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Minimal initial screen for the Locker. Renders a 2×3 grid of buttons, one
 * per loadout slot. Each button shows the loadout name (or "empty"). Full
 * save/load/rename/delete actions are wired up by follow-up networking work;
 * the buttons currently only log their intent.
 */
public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, "textures/gui/locker.png");

    public LockerScreen(LockerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 200;
        this.imageHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos + 10;
        int top = topPos + 25;
        int btnW = 85;
        int btnH = 24;
        for (int i = 0; i < LockerData.SLOT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            int finalI = i;
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.lockers.slot", i + 1),
                    btn -> onSlotButton(finalI))
                    .bounds(left + col * (btnW + 6), top + row * (btnH + 4), btnW, btnH)
                    .build());
        }
    }

    private void onSlotButton(int slotIndex) {
        // TODO: send SaveLoadoutC2S / LoadLoadoutC2S packet based on modifier keys.
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("message.lockers.slot_clicked", slotIndex + 1), true);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Plain dark background until textures/gui/locker.png is authored.
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC202020);
    }
}
