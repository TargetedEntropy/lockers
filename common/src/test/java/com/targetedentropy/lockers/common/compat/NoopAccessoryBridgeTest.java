package com.targetedentropy.lockers.common.compat;

import static org.assertj.core.api.Assertions.assertThat;

import com.targetedentropy.lockers.common.model.SlotId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NoopAccessoryBridgeTest {

    @Test
    void idIsNoop() {
        assertThat(new NoopAccessoryBridge<>().id()).isEqualTo("noop");
    }

    @Test
    void reportsAvailable() {
        assertThat(new NoopAccessoryBridge<>().isAvailable()).isTrue();
    }

    @Test
    void captureIsEmptyMap() {
        assertThat(new NoopAccessoryBridge<Object, Object>().capture(new Object())).isEmpty();
    }

    @Test
    void applyIsNoop() {
        NoopAccessoryBridge<Object, Object> b = new NoopAccessoryBridge<>();
        // Should not throw for any map size, including non-empty input it will ignore.
        b.apply(new Object(), Map.of());
        b.apply(new Object(), Map.of(SlotId.parse("curios:ring/0"), new byte[] {1}));
    }

    @Test
    void knownSlotsEmpty() {
        assertThat(new NoopAccessoryBridge<Object, Object>().knownSlots(new Object())).isEmpty();
    }
}
