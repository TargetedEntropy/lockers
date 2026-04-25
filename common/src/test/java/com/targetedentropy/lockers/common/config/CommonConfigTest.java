package com.targetedentropy.lockers.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.config.CommonConfig.AccessoryImplPreference;
import com.targetedentropy.lockers.common.model.AccessControl;
import org.junit.jupiter.api.Test;

class CommonConfigTest {

    @Test
    void defaultsAreOwnerOnlyOpBypassAutoAccessories() {
        CommonConfig c = CommonConfig.defaults();
        assertThat(c.defaultAccess()).isEqualTo(AccessControl.OWNER_ONLY);
        assertThat(c.opsBypassOwnership()).isTrue();
        assertThat(c.preferredAccessoryImpl()).isEqualTo(AccessoryImplPreference.AUTO);
    }

    @Test
    void constructorRejectsNullDefaultAccess() {
        assertThatThrownBy(() -> new CommonConfig(null, true, AccessoryImplPreference.AUTO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullPreferredAccessoryImpl() {
        assertThatThrownBy(() -> new CommonConfig(AccessControl.PUBLIC, true, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allPreferenceValuesPresent() {
        assertThat(AccessoryImplPreference.values())
                .containsExactly(
                        AccessoryImplPreference.AUTO,
                        AccessoryImplPreference.CURIOS,
                        AccessoryImplPreference.ACCESSORIES,
                        AccessoryImplPreference.NONE);
    }

    @Test
    void preferenceValueOfRoundtrip() {
        for (AccessoryImplPreference p : AccessoryImplPreference.values()) {
            assertThat(AccessoryImplPreference.valueOf(p.name())).isEqualTo(p);
        }
    }
}
