package com.targetedentropy.lockers.neoforge.client;

import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import com.targetedentropy.lockers.neoforge.network.DeleteLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.LoadLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SaveLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Six-row loadout grid. Each row:
 * <pre>
 *   Slot N: &lt;name or "Empty"&gt;  [Save] [Load] [X]
 * </pre>
 * State comes from {@link SyncLockerPacket} pushed by the server whenever the
 * Locker mutates. No local state is trusted — the server is authoritative.
 * <p>
 * Rename UX is intentionally deferred to v0.2 (requires a text-field and
 * edit-mode flow); the server honors {@code RenameLoadoutPacket} already.
 */
public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, "textures/gui/locker.png");

    private static final int GUI_W = 248;
    private static final int GUI_H = 184;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 4;
    private static final int ROW_Y0 = 24;

    /** Most recent server-authoritative view of the locker. Empty until first sync. */
    private Optional<LockerData> data = Optional.empty();

    private final List<SlotWidgets> rows = new ArrayList<>();

    public LockerScreen(LockerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = -10000;  // hide the default "Inventory" label for now
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int i = 0; i < LockerData.SLOT_COUNT; i++) {
            rows.add(buildRow(i));
        }
        refreshButtonStates();
    }

    /** Called by {@link com.targetedentropy.lockers.neoforge.network.LockerPayloadHandler} on sync. */
    public void onSync(SyncLockerPacket pkt) {
        if (!pkt.pos().equals(menu.getPos())) return;
        this.data = Optional.of(pkt.data());
        refreshButtonStates();
    }

    private SlotWidgets buildRow(int slotIndex) {
        int y = topPos + ROW_Y0 + slotIndex * (ROW_H + ROW_GAP);
        int leftX = leftPos + 8;
        int saveX = leftPos + GUI_W - 170;
        int loadX = leftPos + GUI_W - 110;
        int delX  = leftPos + GUI_W - 30;

        Button save = Button.builder(
                Component.translatable("gui.lockers.save"),
                btn -> sendSave(slotIndex))
                .bounds(saveX, y, 55, 20)
                .build();

        Button load = Button.builder(
                Component.translatable("gui.lockers.load"),
                btn -> sendLoad(slotIndex))
                .bounds(loadX, y, 55, 20)
                .build();

        Button delete = Button.builder(
                Component.literal("X"),
                btn -> sendDelete(slotIndex))
                .bounds(delX, y, 20, 20)
                .build();

        addRenderableWidget(save);
        addRenderableWidget(load);
        addRenderableWidget(delete);

        return new SlotWidgets(slotIndex, leftX, y, save, load, delete);
    }

    private void refreshButtonStates() {
        for (SlotWidgets row : rows) {
            Optional<Loadout> lo = data.flatMap(d -> d.slot(row.slotIndex));
            boolean populated = lo.isPresent();
            row.load.active = populated;
            row.delete.active = populated;
            // Save is always active — it overwrites.
        }
    }

    private void sendSave(int slot) {
        String name = "Loadout " + (slot + 1);  // auto-named; rename UX deferred
        PacketDistributor.sendToServer(new SaveLoadoutPacket(menu.getPos(), slot, name));
    }

    private void sendLoad(int slot) {
        PacketDistributor.sendToServer(new LoadLoadoutPacket(menu.getPos(), slot));
    }

    private void sendDelete(int slot) {
        PacketDistributor.sendToServer(new DeleteLoadoutPacket(menu.getPos(), slot));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        // Plain dark panel until a real background texture is authored.
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2A2A2A);
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF808080);
        gfx.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF101010);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderSlotLabels(gfx);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderSlotLabels(GuiGraphics gfx) {
        for (SlotWidgets row : rows) {
            Optional<Loadout> lo = data.flatMap(d -> d.slot(row.slotIndex));
            String label = (row.slotIndex + 1) + ": "
                    + lo.map(Loadout::name).orElseGet(() ->
                            I18n("gui.lockers.empty_slot", "Empty"));
            gfx.drawString(font, label, row.labelX, row.y + 6, 0xFFFFFFFF, false);
        }
    }

    private static String I18n(String key, String fallback) {
        var translated = Component.translatable(key);
        String s = translated.getString();
        return s.equals(key) ? fallback : s;
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, this.title, 8, 8, 0xFFFFFFFF, false);
    }

    private record SlotWidgets(int slotIndex, int labelX, int y,
                               Button save, Button load, Button delete) {}
}
