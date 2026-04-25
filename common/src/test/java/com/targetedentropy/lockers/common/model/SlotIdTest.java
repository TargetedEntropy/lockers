package com.targetedentropy.lockers.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlotIdTest {

    @Test
    void parseAndFormatRoundtripCurios() {
        SlotId id = SlotId.parse("curios:ring/0");
        assertThat(id.namespace()).isEqualTo("curios");
        assertThat(id.path()).isEqualTo("ring/0");
        assertThat(id.asString()).isEqualTo("curios:ring/0");
    }

    @Test
    void parseAndFormatRoundtripAccessories() {
        SlotId id = SlotId.parse("accessories:finger/1");
        assertThat(id.asString()).isEqualTo("accessories:finger/1");
    }

    @Test
    void parseAndFormatRoundtripVanillaArmor() {
        SlotId id = SlotId.parse("vanilla:armor/chest");
        assertThat(id.namespace()).isEqualTo("vanilla");
        assertThat(id.path()).isEqualTo("armor/chest");
    }

    @Test
    void parseFailsOnMissingColon() {
        assertThatThrownBy(() -> SlotId.parse("no-colon-here"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing ':'");
    }

    @Test
    void parseFailsOnNull() {
        assertThatThrownBy(() -> SlotId.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsBlankNamespace() {
        assertThatThrownBy(() -> new SlotId("", "path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("namespace");
    }

    @Test
    void constructorRejectsBlankPath() {
        assertThatThrownBy(() -> new SlotId("curios", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path");
    }

    @Test
    void constructorRejectsNamespaceWithColon() {
        assertThatThrownBy(() -> new SlotId("foo:bar", "path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("':'");
    }

    @Test
    void constructorRejectsNullNamespace() {
        assertThatThrownBy(() -> new SlotId(null, "path"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullPath() {
        assertThatThrownBy(() -> new SlotId("curios", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsAndHashCodeWorkAsValue() {
        SlotId a = SlotId.parse("curios:ring/0");
        SlotId b = new SlotId("curios", "ring/0");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(SlotId.parse("curios:ring/1"));
        assertThat(a).isNotEqualTo("not a slot id");
    }

    @Test
    void pathMayContainMultipleSlashesAndColonsAreOnlySplitOnce() {
        // Only the FIRST colon is the delimiter. Path can contain anything after.
        SlotId id = SlotId.parse("mod:deep/path/with/slashes");
        assertThat(id.namespace()).isEqualTo("mod");
        assertThat(id.path()).isEqualTo("deep/path/with/slashes");
    }
}
