package com.targetedentropy.lockers.common.config;

import com.targetedentropy.lockers.common.model.AccessControl;
import java.util.Objects;

/**
 * Server-side config surface exposed to version-specific configuration systems.
 * Kept as a plain record so it is trivially constructable in tests.
 */
public record CommonConfig(
        AccessControl defaultAccess,
        boolean opsBypassOwnership,
        AccessoryImplPreference preferredAccessoryImpl
) {

    public CommonConfig {
        Objects.requireNonNull(defaultAccess, "defaultAccess");
        Objects.requireNonNull(preferredAccessoryImpl, "preferredAccessoryImpl");
    }

    /** Default config: owner-only access, ops bypass ownership, auto-detect accessory API. */
    public static CommonConfig defaults() {
        return new CommonConfig(AccessControl.OWNER_ONLY, true, AccessoryImplPreference.AUTO);
    }

    /** Which accessory API to use when one or both are installed. */
    public enum AccessoryImplPreference {
        /** Prefer Curios if loaded, else Accessories, else no accessories. */
        AUTO,
        /** Force Curios; warn and fall back if not installed. */
        CURIOS,
        /** Force Accessories; warn and fall back if not installed. */
        ACCESSORIES,
        /** Disable accessory support entirely (armor + offhand only). */
        NONE
    }
}
