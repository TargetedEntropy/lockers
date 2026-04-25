package com.targetedentropy.lockers.common.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.targetedentropy.lockers.common.serialize.DataTag.ByteArrayTag;
import com.targetedentropy.lockers.common.serialize.DataTag.Compound;
import com.targetedentropy.lockers.common.serialize.DataTag.IntTag;
import com.targetedentropy.lockers.common.serialize.DataTag.ListTag;
import com.targetedentropy.lockers.common.serialize.DataTag.LongTag;
import com.targetedentropy.lockers.common.serialize.DataTag.StringTag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataTagTest {

    @Test
    void builderProducesAccessibleCompound() {
        Compound c = Compound.builder()
                .putString("s", "hi")
                .putInt("i", 42)
                .putLong("l", 1_000_000_000L)
                .putByteArray("b", new byte[] {1, 2, 3})
                .build();

        assertThat(c.getString("s")).isEqualTo("hi");
        assertThat(c.getInt("i")).isEqualTo(42);
        assertThat(c.getLong("l")).isEqualTo(1_000_000_000L);
        assertThat(c.getByteArray("b")).containsExactly(1, 2, 3);
        assertThat(c.has("i")).isTrue();
        assertThat(c.has("missing")).isFalse();
    }

    @Test
    void emptyCompoundHasNoEntries() {
        assertThat(Compound.empty().entries()).isEmpty();
    }

    @Test
    void emptyListTagHasNoItems() {
        assertThat(ListTag.empty().items()).isEmpty();
    }

    @Test
    void listTagOfBuilderCopiesList() {
        List<DataTag> src = new java.util.ArrayList<>();
        src.add(new IntTag(1));
        ListTag lt = ListTag.of(src);
        src.add(new IntTag(2));
        assertThat(lt.items()).hasSize(1);
    }

    @Test
    void getCompoundReturnsNestedCompound() {
        Compound inner = Compound.builder().putInt("x", 7).build();
        Compound outer = Compound.builder().put("n", inner).build();
        assertThat(outer.getCompound("n").getInt("x")).isEqualTo(7);
    }

    @Test
    void getListReturnsListTag() {
        ListTag inner = new ListTag(List.of(new IntTag(1), new IntTag(2)));
        Compound c = Compound.builder().put("lst", inner).build();
        assertThat(c.getList("lst").items()).hasSize(2);
    }

    @Test
    void typedAccessorsRejectWrongTypes() {
        Compound c = Compound.builder()
                .putString("s", "not-an-int")
                .build();
        assertThatThrownBy(() -> c.getInt("s"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.getLong("s"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.getByteArray("s"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.getCompound("s"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.getList("s"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getStringRejectsWrongType() {
        Compound c = Compound.builder().putInt("i", 1).build();
        assertThatThrownBy(() -> c.getString("i"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getReturnsOptional() {
        Compound c = Compound.builder().putInt("i", 1).build();
        assertThat(c.get("i")).isPresent();
        assertThat(c.get("missing")).isEmpty();
    }

    @Test
    void toBuilderPreservesExistingEntries() {
        Compound original = Compound.builder().putInt("x", 1).build();
        Compound patched = original.toBuilder().putInt("y", 2).build();
        assertThat(patched.getInt("x")).isEqualTo(1);
        assertThat(patched.getInt("y")).isEqualTo(2);
        assertThat(original.has("y")).isFalse();
    }

    @Test
    void byteArrayTagDefensivelyCopiesAndEqualsByValue() {
        byte[] src = {1, 2, 3};
        ByteArrayTag a = new ByteArrayTag(src);
        src[0] = 99;
        assertThat(a.value()).containsExactly(1, 2, 3);

        ByteArrayTag b = new ByteArrayTag(new byte[] {1, 2, 3});
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new ByteArrayTag(new byte[] {1}));
        assertThat(a).isNotEqualTo("not a tag");
        assertThat(a.toString()).contains("length=3");
    }

    @Test
    void stringTagRejectsNull() {
        assertThatThrownBy(() -> new StringTag(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void byteArrayTagRejectsNull() {
        assertThatThrownBy(() -> new ByteArrayTag(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void compoundRejectsNullEntries() {
        assertThatThrownBy(() -> new Compound(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void listTagRejectsNullItems() {
        assertThatThrownBy(() -> new ListTag(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullKey() {
        assertThatThrownBy(() -> Compound.builder().put(null, new IntTag(0)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullValue() {
        assertThatThrownBy(() -> Compound.builder().put("k", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void intTagAndLongTagSimpleRoundtrip() {
        assertThat(new IntTag(7).value()).isEqualTo(7);
        assertThat(new LongTag(Long.MAX_VALUE).value()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void entriesMapIsImmutable() {
        Compound c = Compound.builder().putInt("x", 1).build();
        assertThatThrownBy(() -> c.entries().put("y", new IntTag(2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void builderToBuilderFromEmpty() {
        // toBuilder on an empty compound should work and produce a fresh builder.
        Compound empty = Compound.empty();
        Compound built = empty.toBuilder().putInt("x", 1).build();
        assertThat(built.getInt("x")).isEqualTo(1);
    }
}
