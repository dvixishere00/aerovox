package com.aerovox.glue;

import com.aerovox.AerovoxConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Right-click a corner, right-click the opposite corner, then Shift + right-click to glue.
 * Right-click air to cancel. The server keeps the authoritative selection; the client mirrors it for the outline.
 */
public class SuperglueItem extends Item {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    public SuperglueItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) {
            GlueSelectionView.click(context.getClickedPos(), player.isShiftKeyDown());
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        BlockPos clicked = context.getClickedPos();
        Selection sel = SELECTIONS.computeIfAbsent(player.getUUID(), id -> new Selection());
        ResourceKey<Level> dim = level.dimension();
        if (sel.dimension != null && !sel.dimension.equals(dim)) sel.reset();
        sel.dimension = dim;

        if (player.isShiftKeyDown() && sel.first != null && sel.second != null) {
            confirm(serverLevel, player, sel);
            return InteractionResult.SUCCESS;
        }

        if (sel.first == null || sel.second != null) {
            sel.first = clicked.immutable();
            sel.second = null;
            send(player, "Superglue: first corner set. Right-click the opposite corner.");
        } else {
            sel.second = clicked.immutable();
            send(player, "Superglue: region of " + volume(sel.first, sel.second)
                    + " positions. Shift + right-click to glue, right-click air to cancel.");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            GlueSelectionView.clear();
        } else if (SELECTIONS.remove(player.getUUID()) != null) {
            send(player, "Superglue selection cleared.");
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void confirm(ServerLevel level, Player player, Selection sel) {
        long volume = volume(sel.first, sel.second);
        if (volume > AerovoxConfig.MAX_GLUE_BLOCKS.get()) {
            send(player, "Superglue: selection is " + volume + " positions; limit is "
                    + AerovoxConfig.MAX_GLUE_BLOCKS.get() + ".");
            return;
        }

        GlueData glue = GlueData.get(level);
        List<BlockPos> solid = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(sel.first, sel.second)) {
            BlockState state = level.getBlockState(p);
            if (state.isAir()) continue;
            if (glue.groupAt(p).isPresent()) {
                send(player, "Superglue: selection overlaps an existing glued structure.");
                return;
            }
            solid.add(p.immutable());
        }

        if (solid.isEmpty()) {
            send(player, "Superglue: no solid blocks in that region.");
            return;
        }
        glue.createGroup(solid);
        SELECTIONS.remove(player.getUUID());
        send(player, "Superglue: glued " + solid.size() + " blocks.");
    }

    private static long volume(BlockPos a, BlockPos b) {
        long dx = Math.abs(a.getX() - b.getX()) + 1L;
        long dy = Math.abs(a.getY() - b.getY()) + 1L;
        long dz = Math.abs(a.getZ() - b.getZ()) + 1L;
        return dx * dy * dz;
    }

    private static void send(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    private static final class Selection {
        ResourceKey<Level> dimension;
        BlockPos first;
        BlockPos second;

        void reset() {
            first = null;
            second = null;
        }
    }
}
