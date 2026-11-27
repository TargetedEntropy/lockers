package com.targetedentropy.lockers.neoforge.block;

import net.minecraft.util.StringRepresentable;

/**
 * Which slice of the 3-tall Locker structure a given block-position represents.
 * <ul>
 *   <li>{@code BOTTOM} — has the {@link LockerBlockEntity} and renders the
 *       full 3-block-tall visual model.</li>
 *   <li>{@code MIDDLE}, {@code TOP} — invisible solid placeholders that
 *       prevent blocks from being placed in the upper visual cells. They
 *       carry no BE; right-click forwards to {@code BOTTOM}.</li>
 * </ul>
 */
public enum LockerPart implements StringRepresentable {
    BOTTOM("bottom"),
    MIDDLE("middle"),
    TOP("top");

    private final String name;
    LockerPart(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }

    /** Vertical offset (in blocks) from this part to the BOTTOM. */
    public int offsetToBottom() {
        return switch (this) {
            case BOTTOM -> 0;
            case MIDDLE -> -1;
            case TOP -> -2;
        };
    }
}
