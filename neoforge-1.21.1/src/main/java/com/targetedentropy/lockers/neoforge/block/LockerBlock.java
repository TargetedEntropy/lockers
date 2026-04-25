package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.common.model.LockerData;
import com.targetedentropy.lockers.common.serialize.LockerDataCodec;
import com.targetedentropy.lockers.neoforge.nbt.DataTagBridge;
import com.targetedentropy.lockers.neoforge.registry.LockersBlocks;
import com.targetedentropy.lockers.neoforge.registry.LockersItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class LockerBlock extends BaseEntityBlock {

    /** Sub-key inside the dropped item's {@code minecraft:custom_data} compound. */
    public static final String STORAGE_KEY = "lockers:saved_locker_data";

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * 1×3×1 collision/outline matching the visual model height. The visual
     * model in {@code common-resources/assets/lockers/models/block/locker.json}
     * extends from y=0 to y=48 (3 vanilla blocks tall). This shape blocks the
     * player from walking through the upper "phantom" cells and gives them a
     * sensible right-click hit target. (NOTE: other blocks can still be placed
     * in the upper 2 cells, since they are world-empty — turning the locker
     * into a true multi-block structure is a follow-up.)
     */
    private static final VoxelShape LOCKER_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 3.0, 1.0);

    public LockerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LOCKER_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LOCKER_SHAPE;
    }

    @Override
    protected com.mojang.serialization.MapCodec<LockerBlock> codec() {
        return simpleCodec(LockerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity be)) return;

        // If the placed item carries saved Locker data (block was previously
        // broken with loadouts inside), restore it. The new placer becomes the
        // owner — saved loadouts survive the move, ownership does not.
        LockerData restored = readLockerData(stack);
        if (restored != null) {
            if (placer instanceof ServerPlayer sp) {
                be.replaceData(restored.withOwner(sp.getUUID(), sp.getGameProfile().getName()));
            } else {
                be.replaceData(restored);
            }
            return;
        }

        // No carried data — fresh placement.
        if (placer instanceof ServerPlayer sp) {
            be.initOnPlace(sp);
        }
    }

    /**
     * Drop a Locker item that carries the saved {@link LockerData} via the
     * vanilla {@link DataComponents#CUSTOM_DATA} component. Replaces the
     * static loot table — there is no scenario where we want a loadout-less
     * drop from a Locker that had data.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = new ItemStack(LockersItems.LOCKER_ITEM.get());
        BlockEntity raw = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (raw instanceof LockerBlockEntity be) {
            be.data().ifPresent(d -> writeLockerData(drop, d));
        }
        return List.of(drop);
    }

    /**
     * Middle-click pick-block returns a stack carrying the data so creative
     * users can copy a populated Locker.
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack base = new ItemStack(LockersItems.LOCKER_ITEM.get());
        if (level.getBlockEntity(pos) instanceof LockerBlockEntity be) {
            be.data().ifPresent(d -> writeLockerData(base, d));
        }
        return base;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity be)) return InteractionResult.PASS;
        if (!be.canAccess(sp)) {
            sp.displayClientMessage(Component.translatable("message.lockers.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        sp.openMenu(new LockerMenuProvider(be), pos);
        be.syncTo(sp);  // prime the client-side LockerScreen with current state
        return InteractionResult.CONSUME;
    }

    // --- NBT carrier helpers ---------------------------------------------------

    /** Encode {@code data} into the stack's {@link CustomData} under {@link #STORAGE_KEY}. */
    public static void writeLockerData(ItemStack stack, LockerData data) {
        CompoundTag dataNbt = DataTagBridge.toCompoundTag(LockerDataCodec.encode(data));
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag merged = existing.copyTag();
        merged.put(STORAGE_KEY, dataNbt);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
    }

    /** Read {@link LockerData} from the stack's {@link CustomData}; null if absent or malformed. */
    @Nullable
    public static LockerData readLockerData(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag tag = cd.copyTag();
        if (!tag.contains(STORAGE_KEY)) return null;
        try {
            return LockerDataCodec.decode(DataTagBridge.fromCompoundTag(tag.getCompound(STORAGE_KEY)));
        } catch (IllegalStateException e) {
            // Corrupt or version-incompatible NBT — drop the saved data rather than crashing.
            return null;
        }
    }

    // Keeps the static reference to LockersBlocks live in case future refactors
    // need the block instance from this file (e.g. for blockstate-aware drops).
    @SuppressWarnings("unused")
    private static void touchBlocksReference() {
        Object ignore = LockersBlocks.LOCKER;
    }

    private record LockerMenuProvider(LockerBlockEntity be) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("container.lockers.locker");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
            return new com.targetedentropy.lockers.neoforge.menu.LockerMenu(
                    containerId, inv, be.getBlockPos());
        }
    }
}
