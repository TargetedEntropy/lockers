package com.targetedentropy.lockers.neoforge.client;

import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import com.targetedentropy.lockers.neoforge.network.DeleteLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.LoadLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.RenameLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SaveLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Six-row loadout grid. Each row:
 * <pre>
 *   N: [editable name]  [Save] [Load] [X]
 * </pre>
 * Per-slot state from {@link SyncLockerPacket}; server is authoritative.
 * <ul>
 *   <li>The name {@link EditBox} is editable when the slot is populated and
 *       disabled when empty (shows an "Empty" placeholder).</li>
 *   <li>Hitting Enter or losing focus while the value differs from the synced
 *       name fires a {@link RenameLoadoutPacket}.</li>
 *   <li>Save is always active; Load and Delete only when the slot has a loadout.</li>
 * </ul>
 */
public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    @SuppressWarnings("unused")
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(LockersMod.MOD_ID, "textures/gui/locker.png");

    private static final int GUI_W = 256;
    private static final int GUI_H = 184;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 4;
    private static final int ROW_Y0 = 24;

    private Optional<LockerData> data = Optional.empty();
    private final List<SlotWidgets> rows = new ArrayList<>();

    public LockerScreen(LockerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int i = 0; i < LockerData.SLOT_COUNT; i++) {
            rows.add(buildRow(i));
        }
        refreshFromData();
    }

    public void onSync(SyncLockerPacket pkt) {
        if (!pkt.pos().equals(menu.getPos())) return;
        this.data = Optional.of(pkt.data());
        refreshFromData();
    }

    private SlotWidgets buildRow(int slotIndex) {
        int y = topPos + ROW_Y0 + slotIndex * (ROW_H + ROW_GAP);
        int labelX = leftPos + 8;
        int nameX  = leftPos + 26;
        int nameW  = 110;
        int saveX  = leftPos + GUI_W - 132;
        int loadX  = leftPos + GUI_W - 76;
        int delX   = leftPos + GUI_W - 22;

        EditBox name = new EditBox(font, nameX, y + 1, nameW, 18, Component.translatable("gui.lockers.name_field"));
        name.setMaxLength(Loadout.MAX_NAME_LENGTH);

        Button save = Button.builder(
                Component.translatable("gui.lockers.save"),
                btn -> sendSave(slotIndex))
                .bounds(saveX, y, 50, 20)
                .build();

        Button load = Button.builder(
                Component.translatable("gui.lockers.load"),
                btn -> sendLoad(slotIndex))
                .bounds(loadX, y, 50, 20)
                .build();

        Button delete = Button.builder(
                Component.literal("X"),
                btn -> sendDelete(slotIndex))
                .bounds(delX, y, 18, 20)
                .build();

        SlotWidgets row = new SlotWidgets(slotIndex, labelX, y, name, save, load, delete);
        // Commit rename when focus leaves the EditBox.
        name.setResponder(s -> { /* tracked locally; rename fired on Enter or focus-out */ });
        addRenderableWidget(name);
        addRenderableWidget(save);
        addRenderableWidget(load);
        addRenderableWidget(delete);
        return row;
    }

    /**
     * Push the latest server-side {@link LockerData} into all per-slot widgets.
     * Skips the EditBox if the user is currently focused on it AND has typed a
     * different value — we don't want to clobber an in-progress edit.
     */
    private void refreshFromData() {
        for (SlotWidgets row : rows) {
            Optional<Loadout> lo = data.flatMap(d -> d.slot(row.slotIndex));
            boolean populated = lo.isPresent();
            row.load.active = populated;
            row.delete.active = populated;

            String authoritative = lo.map(Loadout::name).orElse("");
            if (!row.name.isFocused()) {
                row.name.setValue(authoritative);
            }
            row.name.setEditable(populated);
            row.lastSyncedName = authoritative;
        }
    }

    private void sendSave(int slot) {
        // Use the EditBox value if user typed a name; else server picks "Loadout N".
        String typed = rows.get(slot).name.getValue().strip();
        String name = typed.isEmpty() ? ("Loadout " + (slot + 1)) : typed;
        PacketDistributor.sendToServer(new SaveLoadoutPacket(menu.getPos(), slot, name));
    }

    private void sendLoad(int slot) {
        PacketDistributor.sendToServer(new LoadLoadoutPacket(menu.getPos(), slot));
    }

    private void sendDelete(int slot) {
        PacketDistributor.sendToServer(new DeleteLoadoutPacket(menu.getPos(), slot));
    }

    private void maybeSendRename(SlotWidgets row) {
        // Only renamable when the slot is populated server-side.
        boolean populated = data.flatMap(d -> d.slot(row.slotIndex)).isPresent();
        if (!populated) return;
        String value = row.name.getValue().strip();
        if (value.isEmpty() || value.equals(row.lastSyncedName)) return;
        PacketDistributor.sendToServer(new RenameLoadoutPacket(menu.getPos(), row.slotIndex, value));
        row.lastSyncedName = value;  // optimistic; server will re-confirm via SyncLocker
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter on a focused EditBox commits the rename, then defocuses.
        if (keyCode == 257 /* GLFW_KEY_ENTER */ || keyCode == 335 /* numpad enter */) {
            for (SlotWidgets row : rows) {
                if (row.name.isFocused()) {
                    maybeSendRename(row);
                    row.name.setFocused(false);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If clicking outside a focused EditBox, commit any pending rename.
        for (SlotWidgets row : rows) {
            if (row.name.isFocused() && !row.name.isMouseOver(mouseX, mouseY)) {
                maybeSendRename(row);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2A2A2A);
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF808080);
        gfx.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF101010);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderRowLabels(gfx);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderRowLabels(GuiGraphics gfx) {
        for (SlotWidgets row : rows) {
            gfx.drawString(font, (row.slotIndex + 1) + ":", row.labelX, row.y + 6, 0xFFFFFFFF, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, this.title, 8, 8, 0xFFFFFFFF, false);
    }

    private static final class SlotWidgets {
        final int slotIndex;
        final int labelX;
        final int y;
        final EditBox name;
        final Button save;
        final Button load;
        final Button delete;
        String lastSyncedName = "";

        SlotWidgets(int slotIndex, int labelX, int y,
                    EditBox name, Button save, Button load, Button delete) {
            this.slotIndex = slotIndex;
            this.labelX = labelX;
            this.y = y;
            this.name = name;
            this.save = save;
            this.load = load;
            this.delete = delete;
        }
    }
}
