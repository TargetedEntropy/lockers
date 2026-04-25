package com.targetedentropy.lockers.common.access;

import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.LockerData;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure-function access control for Lockers. Every branch is testable without
 * touching Minecraft.
 */
public final class AccessPolicy {

    private AccessPolicy() {}

    /**
     * Decide whether {@code playerId} may open / save / load / rename / delete
     * on the given Locker.
     */
    public static Decision canAccess(UUID playerId, boolean isOp, LockerData data, CommonConfig cfg) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(cfg, "cfg");

        if (isOp && cfg.opsBypassOwnership()) {
            return Decision.allow(ReasonCode.OP_BYPASS);
        }

        AccessControl access = data.access();
        return switch (access) {
            case PUBLIC -> Decision.allow(ReasonCode.PUBLIC);
            case OWNER_ONLY, TEAM -> playerId.equals(data.owner())
                    ? Decision.allow(ReasonCode.OWNER)
                    : Decision.deny(ReasonCode.NOT_OWNER);
        };
    }

    /** Whether a player can *change* access control settings. Only the owner (or op-bypass). */
    public static Decision canModifyAccess(UUID playerId, boolean isOp, LockerData data, CommonConfig cfg) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(cfg, "cfg");

        if (isOp && cfg.opsBypassOwnership()) {
            return Decision.allow(ReasonCode.OP_BYPASS);
        }
        return playerId.equals(data.owner())
                ? Decision.allow(ReasonCode.OWNER)
                : Decision.deny(ReasonCode.NOT_OWNER);
    }

    public record Decision(boolean allowed, ReasonCode reason) {
        public static Decision allow(ReasonCode r) { return new Decision(true, r); }
        public static Decision deny(ReasonCode r) { return new Decision(false, r); }
    }

    public enum ReasonCode {
        OWNER,
        OP_BYPASS,
        PUBLIC,
        NOT_OWNER
    }
}
