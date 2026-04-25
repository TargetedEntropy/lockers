package com.targetedentropy.lockers.neoforge.gametest;

import com.targetedentropy.lockers.common.model.EquipmentSlot;
import com.targetedentropy.lockers.common.model.Loadout;
import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import com.targetedentropy.lockers.neoforge.LockersMod;
import com.targetedentropy.lockers.neoforge.block.LockerBlockEntity;
import com.targetedentropy.lockers.neoforge.nbt.DataTagBridge;
import com.targetedentropy.lockers.neoforge.registry.LockersBlocks;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * In-world smoke test: place a Locker, seed its {@link LockerBlockEntity}
 * with a populated {@link LockerData}, then prove the codec + NBT bridge
 * roundtrip without data loss against a live Minecraft registry.
 * <p>
 * Full player-facing save/load-to-inventory flow is manually verified via
 * {@code ./gradlew :neoforge-1.21.1:runClient} — documented in CLAUDE.md.
 * That path requires a fully-constructed {@code ServerPlayer} which the
 * GameTest helpers don't expose cleanly; leaving it as follow-up rather
 * than a brittle test.
 * <p>
 * Requires a GameTest structure {@code lockers:empty_3x3} (a 3×3×3 empty
 * air box) to be authored under {@code data/lockers/gametest/structures/}
 * before this test can run.
 */
@GameTestHolder(LockersMod.MOD_ID)
public final class LockerPlacementGameTest {

    private LockerPlacementGameTest() {}

    @GameTest(template = "lockers:empty_3x3", batch = "placement")
    public static void lockerPersistsData(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, LockersBlocks.LOCKER.get().defaultBlockState());
        helper.assertBlockPresent(LockersBlocks.LOCKER.get(), pos);

        LockerBlockEntity be = helper.getBlockEntity(pos);
        if (be == null) {
            helper.fail("no block entity at " + pos);
            return;
        }

        UUID fakeOwner = new UUID(0xABCDEFL, 0x123456L);
        LockerData seeded = LockerData.fresh(fakeOwner, "gametest", Instant.now());

        Map<EquipmentSlot, byte[]> eq = new EnumMap<>(EquipmentSlot.class);
        eq.put(EquipmentSlot.CHEST, new byte[] {1, 2, 3, 4});
        Loadout loadout = new Loadout("test-pvp", Instant.now(), eq, Map.of());
        be.replaceData(seeded.withSlot(0, loadout));

        // Codec + DataTagBridge roundtrip, exercised against live registries.
        var encodedData = LockerDataCodec.encode(be.getData());
        var nbt = DataTagBridge.toCompoundTag(encodedData);
        var back = LockerDataCodec.decode(DataTagBridge.fromCompoundTag(nbt));
        if (!back.equals(be.getData())) {
            helper.fail("LockerData roundtrip mismatch");
            return;
        }

        helper.succeed();
    }
}
