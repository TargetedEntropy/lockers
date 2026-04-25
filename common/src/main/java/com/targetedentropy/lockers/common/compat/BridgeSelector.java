package com.targetedentropy.lockers.common.compat;

import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.config.CommonConfig.AccessoryImplPreference;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Pure selection logic that picks one accessory API (or the no-op bridge)
 * given the installed mods and user config. Kept {@code static} and
 * dependency-free so the decision matrix is trivially unit-testable.
 * <p>
 * Each per-version NeoForge module calls {@link #choose} at mod-construction
 * time, passing {@code ModList.isLoaded(...)} booleans for Curios and
 * Accessories, along with the resolved {@link CommonConfig}.
 */
public final class BridgeSelector {

    private BridgeSelector() {}

    /**
     * Decide which accessory implementation to use.
     *
     * @param curiosAvailable     whether the Curios mod is loaded
     * @param accessoriesAvailable whether the Accessories mod is loaded
     * @param cfg                  the resolved server-side config
     * @return the selected {@link SelectedImpl}
     */
    public static SelectedImpl choose(
            boolean curiosAvailable,
            boolean accessoriesAvailable,
            CommonConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
        AccessoryImplPreference pref = cfg.preferredAccessoryImpl();

        return switch (pref) {
            case NONE -> SelectedImpl.NOOP;
            case CURIOS -> curiosAvailable
                    ? SelectedImpl.CURIOS
                    : SelectedImpl.forceFallback(SelectedImpl.CURIOS,
                            autoFallback(curiosAvailable, accessoriesAvailable));
            case ACCESSORIES -> accessoriesAvailable
                    ? SelectedImpl.ACCESSORIES
                    : SelectedImpl.forceFallback(SelectedImpl.ACCESSORIES,
                            autoFallback(curiosAvailable, accessoriesAvailable));
            case AUTO -> autoFallback(curiosAvailable, accessoriesAvailable);
        };
    }

    /** Curios wins on AUTO when both are installed (larger ecosystem share). */
    private static SelectedImpl autoFallback(boolean curios, boolean accessories) {
        if (curios) return SelectedImpl.CURIOS;
        if (accessories) return SelectedImpl.ACCESSORIES;
        return SelectedImpl.NOOP;
    }

    /**
     * The outcome of a selection, including whether a user-requested impl
     * could not be honored (so the caller can log a warning).
     */
    public record SelectedImpl(Kind kind, boolean degraded, Kind requested) {

        public static final SelectedImpl CURIOS = new SelectedImpl(Kind.CURIOS, false, null);
        public static final SelectedImpl ACCESSORIES = new SelectedImpl(Kind.ACCESSORIES, false, null);
        public static final SelectedImpl NOOP = new SelectedImpl(Kind.NOOP, false, null);

        public SelectedImpl {
            Objects.requireNonNull(kind, "kind");
        }

        static SelectedImpl forceFallback(SelectedImpl requested, SelectedImpl actual) {
            return new SelectedImpl(actual.kind, true, requested.kind);
        }

        public enum Kind { CURIOS, ACCESSORIES, NOOP }
    }
}
