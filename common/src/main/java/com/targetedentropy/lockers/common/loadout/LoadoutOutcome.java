package com.targetedentropy.lockers.common.loadout;

import com.targetedentropy.lockers.common.model.LockerData;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a {@link LoadoutService} operation: the new locker state to
 * persist and sync (if anything changed), and the message to show the player
 * (if any).
 */
public record LoadoutOutcome(Optional<LockerData> updated, Optional<Feedback> feedback) {

    public static final String NOTHING_TO_SAVE = "message.lockers.nothing_to_save";
    public static final String SAVED = "message.lockers.saved";
    public static final String LOADED = "message.lockers.loaded";

    /** Nothing changed and there is nothing to tell the player. */
    public static final LoadoutOutcome NONE = new LoadoutOutcome(Optional.empty(), Optional.empty());

    /** A translatable message: a translation key plus its string arguments. */
    public record Feedback(String translationKey, List<String> args) {
        public Feedback {
            Objects.requireNonNull(translationKey, "translationKey");
            args = List.copyOf(args);
        }
    }

    public LoadoutOutcome {
        Objects.requireNonNull(updated, "updated");
        Objects.requireNonNull(feedback, "feedback");
    }

    static LoadoutOutcome updated(LockerData data) {
        return new LoadoutOutcome(Optional.of(data), Optional.empty());
    }

    static LoadoutOutcome updated(LockerData data, String translationKey, String arg) {
        return new LoadoutOutcome(Optional.of(data),
                Optional.of(new Feedback(translationKey, List.of(arg))));
    }

    static LoadoutOutcome message(String translationKey) {
        return new LoadoutOutcome(Optional.empty(),
                Optional.of(new Feedback(translationKey, List.of())));
    }
}
