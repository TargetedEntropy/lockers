package com.targetedentropy.lockers.common.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Stable identifier for an accessory slot across Curios / Accessories / vanilla.
 * <p>
 * Encoded as {@code namespace:path} where {@code path} may contain slashes
 * (e.g. {@code curios:ring/0}, {@code accessories:finger/1}, {@code vanilla:armor/chest}).
 * The namespace identifies which accessory API owns the slot; {@code vanilla}
 * is reserved for the 4 armor slots + offhand.
 */
public record SlotId(String namespace, String path) {

    public SlotId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (namespace.contains(":")) {
            throw new IllegalArgumentException("namespace must not contain ':'");
        }
    }

    /**
     * Parse a {@code namespace:path} string. Throws {@link IllegalArgumentException}
     * if the input does not contain exactly one ':'.
     */
    public static SlotId parse(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        int colon = encoded.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("missing ':' in slot id: " + encoded);
        }
        String ns = encoded.substring(0, colon);
        String p = encoded.substring(colon + 1);
        return new SlotId(ns, p);
    }

    /**
     * An accessory slot addressed as {@code <namespace>:<group>/<index>}, the
     * shape both the Curios and Accessories bridges use.
     */
    public static SlotId indexed(String namespace, String group, int index) {
        return new SlotId(namespace, group + "/" + index);
    }

    /** {@code <group>/<index>} split into its parts. */
    public record Indexed(String group, int index) {}

    /**
     * Split this id's path at its last {@code '/'} into group and index, or
     * empty if the path is not of that form. The index is not range-checked.
     */
    public Optional<Indexed> parseIndexed() {
        int slash = path.lastIndexOf('/');
        if (slash <= 0 || slash == path.length() - 1) return Optional.empty();
        try {
            return Optional.of(new Indexed(path.substring(0, slash),
                    Integer.parseInt(path.substring(slash + 1))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String asString() {
        return namespace + ":" + path;
    }
}
