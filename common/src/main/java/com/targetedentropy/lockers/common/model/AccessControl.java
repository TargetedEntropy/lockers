package com.targetedentropy.lockers.common.model;

/**
 * How a Locker decides who may open, save, load, rename, and delete loadouts.
 */
public enum AccessControl {
    /** Only the player who placed the Locker (the owner). Default. */
    OWNER_ONLY,
    /** Anyone who can reach the block may use it. */
    PUBLIC,
    /** Reserved for future team/claim-mod integration. Behaves as OWNER_ONLY today. */
    TEAM;
}
