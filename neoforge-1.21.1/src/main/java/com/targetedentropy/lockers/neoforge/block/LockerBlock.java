package com.targetedentropy.lockers.neoforge.block;

import com.targetedentropy.lockers.neoforge.registry.LockersBlockEntities;
import com.targetedentropy.lockers.neoforge.registry.LockersMenus;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class LockerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public LockerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
        if (level.getBlockEntity(pos) instanceof LockerBlockEntity be
                && placer instanceof ServerPlayer sp) {
            be.initOnPlace(sp);
        }
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
        return InteractionResult.CONSUME;
    }

    /** {@link MenuProvider} backed by a specific block entity (captures pos via the BE). */
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
