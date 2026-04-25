package com.targetedentropy.lockers.common.model;

/**
 * The five non-accessory slots a Locker captures. Accessory slots (rings,
 * charms, etc.) use {@link SlotId} instead because their set is provided
 * dynamically by Curios or Accessories.
 */
public enum EquipmentSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    OFFHAND;
}
