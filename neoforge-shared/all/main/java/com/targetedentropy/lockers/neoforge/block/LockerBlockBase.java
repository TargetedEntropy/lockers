package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.neoforge.McCompat;
import com.targetedentropy.lockers.neoforge.menu.LockerMenu;
import com.targetedentropy.lockers.neoforge.nbt.LockerDataNbt;
import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Locker is a 3-block-tall multi-block structure (BOTTOM / MIDDLE / TOP).
 * <p>
 * The {@link LockerBlockEntity} lives only on BOTTOM. MIDDLE and TOP are real
 * solid cells that render their slice of the model, block placement in the
 * upper cells and collide with entities. Right-click on any part forwards to
 * BOTTOM.
 * <p>
 * Breaking any part atomically removes the other two; the BOTTOM's saved
 * loadouts are stashed onto the dropped item via
 * {@link DataComponents#CUSTOM_DATA} (key {@link #STORAGE_KEY}).
 * <p>
 * Each MC version's {@code LockerBlock} extends this with the members whose
 * signatures differ per version: {@code codec()} and the pick-block override
 * (which delegates to {@link #cloneWithData}).
 */
public abstract class LockerBlockBase extends BaseEntityBlock {

    public static final String STORAGE_KEY = "lockers:saved_locker_data";

    /** Typed as {@link Property} because the concrete property class changed in 1.21.4. */
    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<LockerPart> PART = EnumProperty.create("part", LockerPart.class);

    /** A single 1×1×1 cube; each multi-block cell uses this for collision and outline. */
    private static final VoxelShape CELL_SHAPE = Shapes.block();

    protected LockerBlockBase(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, LockerPart.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PART);
    }

    // --- placement -------------------------------------------------------------

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        // Need 2 cells of headroom above the clicked position for MIDDLE + TOP.
        if (pos.getY() >= McCompat.maxY(level) - 1) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(ctx)) return null;
        if (!level.getBlockState(pos.above(2)).canBeReplaced(ctx)) return null;

        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(PART, LockerPart.BOTTOM);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (state.getValue(PART) != LockerPart.BOTTOM) return;

        // Drop in MIDDLE and TOP siblings (BlockFlags = NOTIFY_NEIGHBOURS | UPDATE_CLIENTS)
        level.setBlock(pos.above(),  state.setValue(PART, LockerPart.MIDDLE), 3);
        level.setBlock(pos.above(2), state.setValue(PART, LockerPart.TOP), 3);

        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity be)) return;

        LockerData restored = readLockerData(stack);
        if (restored != null) {
            be.replaceData(placer instanceof ServerPlayer sp
                    ? restored.withOwner(sp.getUUID(), McCompat.profileName(sp))
                    : restored);
            return;
        }
        if (placer instanceof ServerPlayer sp) {
            be.initOnPlace(sp);
        }
    }

    // --- destruction -----------------------------------------------------------

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos bottomPos = bottomOf(pos, state);

            // Capture drop with NBT BEFORE we remove the BOTTOM block.
            ItemStack drop = new ItemStack(LockersItems.LOCKER_ITEM.get());
            lockerAt(level, bottomPos).flatMap(LockerBlockEntity::data)
                    .ifPresent(d -> writeLockerData(drop, d));

            // Remove the OTHER parts silently (no drops, no BE saved). Self
            // is removed by vanilla's standard playerWillDestroy → setBlockAndUpdate.
            removeSiblingPart(level, bottomPos,          pos);
            removeSiblingPart(level, bottomPos.above(),  pos);
            removeSiblingPart(level, bottomPos.above(2), pos);

            if (!player.isCreative()) {
                popResource(level, pos, drop);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Drops are handled manually in {@link #playerWillDestroy} — no extra loot table drop. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    /**
     * Pick-block stack carrying the locker's data. Picking on any part walks
     * to the BOTTOM to source the block entity.
     */
    protected ItemStack cloneWithData(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack base = new ItemStack(LockersItems.LOCKER_ITEM.get());
        if (level.getBlockEntity(bottomOf(pos, state)) instanceof LockerBlockEntity be) {
            be.data().ifPresent(d -> writeLockerData(base, d));
        }
        return base;
    }

    // --- rendering & shapes ----------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Each part has its own slice of the locker visual; all render normally.
        // (The earlier single-block-tall-model approach failed because vanilla
        // rejects model elements outside [-16, 32] on any axis. See
        // assets/lockers/models/block/locker_{bottom,middle,top}.json.)
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CELL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CELL_SHAPE;
    }

    // --- block entity ----------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == LockerPart.BOTTOM ? new LockerBlockEntity(pos, state) : null;
    }

    // --- interaction -----------------------------------------------------------

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        BlockPos bottomPos = bottomOf(pos, state);
        LockerBlockEntity be = lockerAt(level, bottomPos).orElse(null);
        if (be == null) return InteractionResult.PASS;  // structure broken
        if (!be.canAccess(sp)) {
            McCompat.overlay(sp, Component.translatable("message.lockers.not_owner"));
            return InteractionResult.CONSUME;
        }
        sp.openMenu(new LockerMenuProvider(be), bottomPos);
        be.syncTo(sp);
        return InteractionResult.CONSUME;
    }

    // --- helpers ---------------------------------------------------------------

    /** From any part of a Locker, return the world position of its BOTTOM cell. */
    private static BlockPos bottomOf(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case BOTTOM -> pos;
            case MIDDLE -> pos.below();
            case TOP -> pos.below(2);
        };
    }

    /** The block entity at {@code bottomPos}, if that cell really is a Locker's BOTTOM. */
    private Optional<LockerBlockEntity> lockerAt(Level level, BlockPos bottomPos) {
        BlockState bottomState = level.getBlockState(bottomPos);
        if (!bottomState.is(this) || bottomState.getValue(PART) != LockerPart.BOTTOM) {
            return Optional.empty();
        }
        return level.getBlockEntity(bottomPos) instanceof LockerBlockEntity be
                ? Optional.of(be)
                : Optional.empty();
    }

    /**
     * Remove a sibling cell during multi-block destruction. Skips the cell
     * being broken by the player (vanilla handles that), only acts if the
     * target really is one of our parts (defensive against /setblock or
     * world-corruption mid-flight).
     */
    private void removeSiblingPart(Level level, BlockPos targetPos, BlockPos selfPos) {
        if (targetPos.equals(selfPos)) return;
        if (!level.getBlockState(targetPos).is(this)) return;
        // BlockFlags = NOTIFY_NEIGHBOURS | UPDATE_CLIENTS, no drops.
        level.removeBlock(targetPos, false);
    }

    // --- NBT carrier helpers ---------------------------------------------------

    public static void writeLockerData(ItemStack stack, LockerData data) {
        CompoundTag merged = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        merged.put(STORAGE_KEY, LockerDataNbt.encode(data));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
    }

    @Nullable
    public static LockerData readLockerData(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        try {
            return McCompat.compound(cd.copyTag(), STORAGE_KEY).map(LockerDataNbt::decode).orElse(null);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private record LockerMenuProvider(LockerBlockEntity be) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("container.lockers.locker");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
            return new LockerMenu(containerId, inv, be.getBlockPos());
        }
    }
}
