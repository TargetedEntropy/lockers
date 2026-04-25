package com.targetedentropy.lockers.common.compat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.compat.BridgeSelector.SelectedImpl;
import com.targetedentropy.lockers.common.compat.BridgeSelector.SelectedImpl.Kind;
import com.targetedentropy.lockers.common.config.CommonConfig;
import com.targetedentropy.lockers.common.config.CommonConfig.AccessoryImplPreference;
import com.targetedentropy.lockers.common.model.AccessControl;
import org.junit.jupiter.api.Test;

class BridgeSelectorTest {

    private static CommonConfig cfg(AccessoryImplPreference pref) {
        return new CommonConfig(AccessControl.OWNER_ONLY, true, pref);
    }

    @Test
    void autoPrefersCuriosWhenBothInstalled() {
        SelectedImpl s = BridgeSelector.choose(true, true, cfg(AccessoryImplPreference.AUTO));
        assertThat(s.kind()).isEqualTo(Kind.CURIOS);
        assertThat(s.degraded()).isFalse();
    }

    @Test
    void autoPicksCuriosWhenOnlyCurios() {
        SelectedImpl s = BridgeSelector.choose(true, false, cfg(AccessoryImplPreference.AUTO));
        assertThat(s.kind()).isEqualTo(Kind.CURIOS);
    }

    @Test
    void autoPicksAccessoriesWhenOnlyAccessories() {
        SelectedImpl s = BridgeSelector.choose(false, true, cfg(AccessoryImplPreference.AUTO));
        assertThat(s.kind()).isEqualTo(Kind.ACCESSORIES);
    }

    @Test
    void autoPicksNoopWhenNeitherInstalled() {
        SelectedImpl s = BridgeSelector.choose(false, false, cfg(AccessoryImplPreference.AUTO));
        assertThat(s.kind()).isEqualTo(Kind.NOOP);
    }

    @Test
    void nonePreferenceAlwaysYieldsNoop() {
        assertThat(BridgeSelector.choose(true, true, cfg(AccessoryImplPreference.NONE)).kind())
                .isEqualTo(Kind.NOOP);
        assertThat(BridgeSelector.choose(false, false, cfg(AccessoryImplPreference.NONE)).kind())
                .isEqualTo(Kind.NOOP);
    }

    @Test
    void curiosPreferenceUsesCuriosWhenAvailable() {
        SelectedImpl s = BridgeSelector.choose(true, true, cfg(AccessoryImplPreference.CURIOS));
        assertThat(s.kind()).isEqualTo(Kind.CURIOS);
        assertThat(s.degraded()).isFalse();
        assertThat(s.requested()).isNull();
    }

    @Test
    void curiosPreferenceFallsBackToAccessoriesWhenCuriosMissing() {
        SelectedImpl s = BridgeSelector.choose(false, true, cfg(AccessoryImplPreference.CURIOS));
        assertThat(s.kind()).isEqualTo(Kind.ACCESSORIES);
        assertThat(s.degraded()).isTrue();
        assertThat(s.requested()).isEqualTo(Kind.CURIOS);
    }

    @Test
    void curiosPreferenceFallsBackToNoopWhenBothMissing() {
        SelectedImpl s = BridgeSelector.choose(false, false, cfg(AccessoryImplPreference.CURIOS));
        assertThat(s.kind()).isEqualTo(Kind.NOOP);
        assertThat(s.degraded()).isTrue();
        assertThat(s.requested()).isEqualTo(Kind.CURIOS);
    }

    @Test
    void accessoriesPreferenceUsesAccessoriesWhenAvailable() {
        SelectedImpl s = BridgeSelector.choose(true, true, cfg(AccessoryImplPreference.ACCESSORIES));
        assertThat(s.kind()).isEqualTo(Kind.ACCESSORIES);
        assertThat(s.degraded()).isFalse();
    }

    @Test
    void accessoriesPreferenceFallsBackToCuriosWhenAccessoriesMissing() {
        SelectedImpl s = BridgeSelector.choose(true, false, cfg(AccessoryImplPreference.ACCESSORIES));
        assertThat(s.kind()).isEqualTo(Kind.CURIOS);
        assertThat(s.degraded()).isTrue();
        assertThat(s.requested()).isEqualTo(Kind.ACCESSORIES);
    }

    @Test
    void accessoriesPreferenceFallsBackToNoopWhenNothingInstalled() {
        SelectedImpl s = BridgeSelector.choose(false, false, cfg(AccessoryImplPreference.ACCESSORIES));
        assertThat(s.kind()).isEqualTo(Kind.NOOP);
        assertThat(s.degraded()).isTrue();
        assertThat(s.requested()).isEqualTo(Kind.ACCESSORIES);
    }

    @Test
    void rejectsNullConfig() {
        assertThatThrownBy(() -> BridgeSelector.choose(true, true, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void selectedImplRejectsNullKind() {
        assertThatThrownBy(() -> new SelectedImpl(null, false, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constantsExposeExpectedKinds() {
        assertThat(SelectedImpl.CURIOS.kind()).isEqualTo(Kind.CURIOS);
        assertThat(SelectedImpl.ACCESSORIES.kind()).isEqualTo(Kind.ACCESSORIES);
        assertThat(SelectedImpl.NOOP.kind()).isEqualTo(Kind.NOOP);
    }
}
