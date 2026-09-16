package io.github.salasgthub.anyfind.client.highlight;

import io.github.salasgthub.anyfind.client.SearchSelection;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Turns the highlight off once the player actually opens one of the highlighted containers.
 */
public final class OpenedContainerWatcher {

    /** Block the player last right-clicked, which is what opens a container screen. */
    private static BlockPos lastUsedBlock;

    private OpenedContainerWatcher() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                lastUsedBlock = hitResult.getBlockPos();
            }
            return InteractionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> && SearchSelection.isActive() && opensHighlighted(client)) {
                announceArrival(client);
                SearchSelection.clear();
            }
        });
    }

    private static void announceArrival(Minecraft client) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("message.anyfind.arrived", SearchSelection.stack().getHoverName())
                            .withStyle(ChatFormatting.WHITE)));
        }
    }

    private static boolean opensHighlighted(Minecraft client) {
        if (lastUsedBlock == null || client.level == null) {
            return false;
        }
        BlockPos other = otherHalfOfChest(client, lastUsedBlock);
        return SearchSelection.locations().stream()
                .anyMatch(location -> location.pos().equals(lastUsedBlock) || location.pos().equals(other));
    }

    /**
     * Scans report a double chest under one of its halves, so opening the other half has to count too.
     */
    private static BlockPos otherHalfOfChest(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return ChestBlock.getConnectedBlockPos(pos, state);
    }
}
