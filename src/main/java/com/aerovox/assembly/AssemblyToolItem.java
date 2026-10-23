package com.aerovox.assembly;

import com.aerovox.glue.GlueData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Right-click a superglued structure to assemble it into an aircraft. Shift + right-click an aircraft to disassemble. */
public class AssemblyToolItem extends Item {
    public AssemblyToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null || !(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        GlueData glue = GlueData.get(serverLevel);
        Optional<UUID> group = glue.groupAt(context.getClickedPos());
        if (group.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Aim at a superglued structure to assemble it."), false);
            return InteractionResult.FAIL;
        }
        return AircraftAssembler.assemble(serverLevel, player, glue, group.get());
    }
}
