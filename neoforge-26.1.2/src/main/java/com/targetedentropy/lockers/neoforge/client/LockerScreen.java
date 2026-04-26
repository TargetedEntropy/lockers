package com.targetedentropy.lockers.neoforge.client;

import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import com.targetedentropy.lockers.neoforge.network.ChangeAccessPacket;
import com.targetedentropy.lockers.neoforge.network.DeleteLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.LoadLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.RenameLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SaveLoadoutPacket;
import com.targetedentropy.lockers.neoforge.network.SyncLockerPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Six-row loadout grid + owner-only access toggle.
 * <ul>
 *   <li>Each row shows: <code>N: [editable name] [Save] [Load] [X]</code>.</li>
 *   <li>{@code [Save]} on a populated slot is destructive — the button label
 *       flips to <code>Confirm?</code> for {@value #CONFIRM_WINDOW_MS} ms;
 *       a second click within that window commits.</li>
 *   <li>{@code [X]} uses the same confirm pattern.</li>
 *   <li>Top-right shows {@code [Public]}/{@code [Private]} toggle, visible only
 *       to the locker owner. Click sends {@link ChangeAccessPacket}.</li>
 *   <li>Server is authoritative; sync packets clobber stale local state, but
 *       in-progress name edits are preserved.</li>
 * </ul>
 */
public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    @SuppressWarnings("unused")
    private static final Identifier BG =
            Identifier.fromNamespaceAndPath(LockersMod.MOD_ID, "textures/gui/locker.png");

    private static final int GUI_W = 256;
    private static final int GUI_H = 184;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 4;
    private static final int ROW_Y0 = 24;

    /** How long a confirm prompt remains armed after the first click. */
    static final long CONFIRM_WINDOW_MS = 2000L;

    private Optional<LockerData> data = Optional.empty();
    private final List<SlotWidgets> rows = new ArrayList<>();
    private Button accessToggle;

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
        accessToggle = Button.builder(
                Component.translatable("gui.lockers.access.private"),
                btn -> sendChangeAccess())
                .bounds(leftPos + GUI_W - 76, topPos + 4, 70, 16)
                .build();
        addRenderableWidget(accessToggle);
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

        EditBox name = new EditBox(font, nameX, y + 1, nameW, 18,
                Component.translatable("gui.lockers.name_field"));
        name.setMaxLength(Loadout.MAX_NAME_LENGTH);

        Button save = Button.builder(
                Component.translatable("gui.lockers.save"),
                btn -> onSaveClicked(slotIndex))
                .bounds(saveX, y, 50, 20)
                .build();

        Button load = Button.builder(
                Component.translatable("gui.lockers.load"),
                btn -> onLoadClicked(slotIndex))
                .bounds(loadX, y, 50, 20)
                .build();

        Button delete = Button.builder(
                Component.literal("X"),
                btn -> onDeleteClicked(slotIndex))
                .bounds(delX, y, 18, 20)
                .build();

        SlotWidgets row = new SlotWidgets(slotIndex, labelX, y, name, save, load, delete);
        addRenderableWidget(name);
        addRenderableWidget(save);
        addRenderableWidget(load);
        addRenderableWidget(delete);
        return row;
    }

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
            row.lastSyncedName = authoritative;
        }
        updateAccessToggle();
    }

    private void updateAccessToggle() {
        if (accessToggle == null) return;
        boolean isOwner = isClientPlayerOwner();
        accessToggle.visible = isOwner;
        accessToggle.active = isOwner;
        if (data.isPresent()) {
            accessToggle.setMessage(data.get().access() == AccessControl.PUBLIC
                    ? Component.translatable("gui.lockers.access.public")
                    : Component.translatable("gui.lockers.access.private"));
        }
    }

    private boolean isClientPlayerOwner() {
        if (data.isEmpty() || minecraft == null || minecraft.player == null) return false;
        return minecraft.player.getUUID().equals(data.get().owner());
    }

    // --- click handlers --------------------------------------------------------

    private void onSaveClicked(int slot) {
        boolean populated = data.flatMap(d -> d.slot(slot)).isPresent();
        SlotWidgets row = rows.get(slot);
        if (populated && !row.saveConfirmArmed()) {
            row.armSaveConfirm();
            return;
        }
        row.disarmSaveConfirm();
        String typed = row.name.getValue().strip();
        String name = typed.isEmpty() ? ("Loadout " + (slot + 1)) : typed;
        PacketDistributor.sendToServer(new SaveLoadoutPacket(menu.getPos(), slot, name));
    }

    private void onLoadClicked(int slot) {
        // Loading is non-destructive (returns gear to inventory) — no confirm.
        rows.forEach(SlotWidgets::disarmConfirms);
        PacketDistributor.sendToServer(new LoadLoadoutPacket(menu.getPos(), slot));
    }

    private void onDeleteClicked(int slot) {
        SlotWidgets row = rows.get(slot);
        if (!row.deleteConfirmArmed()) {
            row.armDeleteConfirm();
            return;
        }
        row.disarmDeleteConfirm();
        PacketDistributor.sendToServer(new DeleteLoadoutPacket(menu.getPos(), slot));
    }

    private void sendChangeAccess() {
        if (data.isEmpty()) return;
        AccessControl current = data.get().access();
        AccessControl next = current == AccessControl.PUBLIC
                ? AccessControl.OWNER_ONLY
                : AccessControl.PUBLIC;
        PacketDistributor.sendToServer(new ChangeAccessPacket(menu.getPos(), next));
    }

    private void maybeSendRename(SlotWidgets row) {
        boolean populated = data.flatMap(d -> d.slot(row.slotIndex)).isPresent();
        if (!populated) return;
        String value = row.name.getValue().strip();
        if (value.isEmpty() || value.equals(row.lastSyncedName)) return;
        PacketDistributor.sendToServer(new RenameLoadoutPacket(menu.getPos(), row.slotIndex, value));
        row.lastSyncedName = value;
    }

    // --- keyboard / mouse ------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
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
        for (SlotWidgets row : rows) {
            if (row.name.isFocused() && !row.name.isMouseOver(mouseX, mouseY)) {
                maybeSendRename(row);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // --- tick / render ---------------------------------------------------------

    @Override
    public void containerTick() {
        super.containerTick();
        long now = System.currentTimeMillis();
        for (SlotWidgets row : rows) {
            // Update Save button label based on confirm state and slot population.
            boolean populated = data.flatMap(d -> d.slot(row.slotIndex)).isPresent();
            if (row.pendingSaveExpiresAt > 0 && now >= row.pendingSaveExpiresAt) {
                row.disarmSaveConfirm();
            }
            row.save.setMessage(row.saveConfirmArmed()
                    ? Component.translatable("gui.lockers.confirm")
                    : Component.translatable("gui.lockers.save"));

            // Update Delete button label.
            if (row.pendingDeleteExpiresAt > 0 && now >= row.pendingDeleteExpiresAt) {
                row.disarmDeleteConfirm();
            }
            row.delete.setMessage(row.deleteConfirmArmed()
                    ? Component.literal("?")
                    : Component.literal("X"));

            // Stale "armed" state cleared when the slot becomes empty.
            if (!populated) {
                row.disarmConfirms();
            }
        }
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

    /** Force-fire pending sync handler — called by {@link Minecraft} via the screen. */
    @SuppressWarnings("unused")
    private static void forceSyncDispatch() {}

    private static final class SlotWidgets {
        final int slotIndex;
        final int labelX;
        final int y;
        final EditBox name;
        final Button save;
        final Button load;
        final Button delete;
        String lastSyncedName = "";
        long pendingSaveExpiresAt = 0L;
        long pendingDeleteExpiresAt = 0L;

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

        boolean saveConfirmArmed() {
            return pendingSaveExpiresAt > System.currentTimeMillis();
        }

        boolean deleteConfirmArmed() {
            return pendingDeleteExpiresAt > System.currentTimeMillis();
        }

        void armSaveConfirm() {
            pendingSaveExpiresAt = System.currentTimeMillis() + CONFIRM_WINDOW_MS;
        }

        void armDeleteConfirm() {
            pendingDeleteExpiresAt = System.currentTimeMillis() + CONFIRM_WINDOW_MS;
        }

        void disarmSaveConfirm() { pendingSaveExpiresAt = 0L; }
        void disarmDeleteConfirm() { pendingDeleteExpiresAt = 0L; }

        void disarmConfirms() {
            disarmSaveConfirm();
            disarmDeleteConfirm();
        }
    }
}
