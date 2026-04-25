package com.targetedentropy.lockers.common.compat;

import com.targetedentropy.lockers.common.model.SlotId;
import java.util.Map;
import java.util.Set;

/**
 * Zero-capability bridge used when neither Curios nor Accessories is loaded
 * (or when the user forces {@code preferred_accessory_impl = NONE}). Armor
 * and offhand slots are still saved by the Locker — accessories are simply
 * omitted.
 */
public final class NoopAccessoryBridge<P, S> implements AccessoryBridge<P, S> {

    public static final String ID = "noop";

    @Override public String id() { return ID; }

    @Override public boolean isAvailable() { return true; }

    @Override public Map<SlotId, byte[]> capture(P player) { return Map.of(); }

    @Override public void apply(P player, Map<SlotId, byte[]> stacks) { /* intentionally empty */ }

    @Override public Set<SlotId> knownSlots(P player) { return Set.of(); }
}
