package com.targetedentropy.lockers.common.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.access.AccessPolicy.Decision;
import com.targetedentropy.lockers.common.access.AccessPolicy.ReasonCode;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.config.CommonConfig.AccessoryImplPreference;
import com.targetedentropy.lockers.common.model.AccessControl;
import com.targetedentropy.lockers.common.model.LockerData;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-04-25T00:00:00Z");
    private static final CommonConfig BYPASS_ON = new CommonConfig(
            AccessControl.OWNER_ONLY, true, AccessoryImplPreference.AUTO);
    private static final CommonConfig BYPASS_OFF = new CommonConfig(
            AccessControl.OWNER_ONLY, false, AccessoryImplPreference.AUTO);

    private static LockerData dataWithAccess(AccessControl access) {
        return LockerData.fresh(OWNER, "alice", T0).withAccess(access);
    }

    @Test
    void ownerCanAlwaysAccessOwnerOnly() {
        Decision d = AccessPolicy.canAccess(OWNER, false, dataWithAccess(AccessControl.OWNER_ONLY), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.OWNER);
    }

    @Test
    void nonOwnerDeniedForOwnerOnlyWhenNotOp() {
        Decision d = AccessPolicy.canAccess(OTHER, false, dataWithAccess(AccessControl.OWNER_ONLY), BYPASS_ON);
        assertThat(d.allowed()).isFalse();
        assertThat(d.reason()).isEqualTo(ReasonCode.NOT_OWNER);
    }

    @Test
    void opBypassesOwnerOnly() {
        Decision d = AccessPolicy.canAccess(OTHER, true, dataWithAccess(AccessControl.OWNER_ONLY), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.OP_BYPASS);
    }

    @Test
    void opDoesNotBypassWhenBypassDisabled() {
        Decision d = AccessPolicy.canAccess(OTHER, true, dataWithAccess(AccessControl.OWNER_ONLY), BYPASS_OFF);
        assertThat(d.allowed()).isFalse();
        assertThat(d.reason()).isEqualTo(ReasonCode.NOT_OWNER);
    }

    @Test
    void publicAllowsNonOwner() {
        Decision d = AccessPolicy.canAccess(OTHER, false, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.PUBLIC);
    }

    @Test
    void publicAllowsOwner() {
        // Ops-bypass is checked first, but we already know bypass-on + op always allows.
        // For the OP_BYPASS vs PUBLIC ordering specifically, with op=true we get OP_BYPASS.
        Decision d = AccessPolicy.canAccess(OWNER, false, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.PUBLIC);
    }

    @Test
    void opBypassCheckPrecedesPublic() {
        Decision d = AccessPolicy.canAccess(OTHER, true, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.reason()).isEqualTo(ReasonCode.OP_BYPASS);
    }

    @Test
    void teamBehavesLikeOwnerOnlyForNow() {
        Decision owner = AccessPolicy.canAccess(OWNER, false, dataWithAccess(AccessControl.TEAM), BYPASS_ON);
        Decision other = AccessPolicy.canAccess(OTHER, false, dataWithAccess(AccessControl.TEAM), BYPASS_ON);
        assertThat(owner.allowed()).isTrue();
        assertThat(other.allowed()).isFalse();
    }

    @Test
    void canModifyAccessDisallowsNonOwner() {
        Decision d = AccessPolicy.canModifyAccess(OTHER, false, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.allowed()).isFalse();
    }

    @Test
    void canModifyAccessAllowsOwner() {
        Decision d = AccessPolicy.canModifyAccess(OWNER, false, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.OWNER);
    }

    @Test
    void canModifyAccessHonorsOpBypass() {
        Decision d = AccessPolicy.canModifyAccess(OTHER, true, dataWithAccess(AccessControl.PUBLIC), BYPASS_ON);
        assertThat(d.allowed()).isTrue();
        assertThat(d.reason()).isEqualTo(ReasonCode.OP_BYPASS);
    }

    @Test
    void canModifyAccessOpWithoutBypassIsDenied() {
        Decision d = AccessPolicy.canModifyAccess(OTHER, true, dataWithAccess(AccessControl.PUBLIC), BYPASS_OFF);
        assertThat(d.allowed()).isFalse();
    }

    @Test
    void canAccessRejectsNullArguments() {
        LockerData data = dataWithAccess(AccessControl.OWNER_ONLY);
        assertThatThrownBy(() -> AccessPolicy.canAccess(null, false, data, BYPASS_ON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessPolicy.canAccess(OWNER, false, null, BYPASS_ON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessPolicy.canAccess(OWNER, false, data, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canModifyAccessRejectsNullArguments() {
        LockerData data = dataWithAccess(AccessControl.OWNER_ONLY);
        assertThatThrownBy(() -> AccessPolicy.canModifyAccess(null, false, data, BYPASS_ON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessPolicy.canModifyAccess(OWNER, false, null, BYPASS_ON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccessPolicy.canModifyAccess(OWNER, false, data, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void decisionFactoryMethodsProduceCorrectValues() {
        assertThat(Decision.allow(ReasonCode.OWNER).allowed()).isTrue();
        assertThat(Decision.deny(ReasonCode.NOT_OWNER).allowed()).isFalse();
    }
}
