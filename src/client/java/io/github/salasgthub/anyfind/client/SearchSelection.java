package io.github.salasgthub.anyfind.client;

import io.github.salasgthub.anyfind.client.config.AnyfindConfig;
import io.github.salasgthub.anyfind.network.ScanResultsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * The item the player picked in the search screen, plus the containers holding it.
 * {@link io.github.salasgthub.anyfind.client.highlight.SelectionHighlighter} draws it in the world.
 */
public final class SearchSelection {

    private static ScanResultsPayload.Entry entry;
    private static ItemStack stack = ItemStack.EMPTY;
    private static long expiresAtMillis;

    private SearchSelection() {
    }

    public static boolean isActive() {
        return entry != null && System.currentTimeMillis() < expiresAtMillis;
    }

    public static List<ScanResultsPayload.Location> locations() {
        return entry == null ? List.of() : entry.locations();
    }

    public static ItemStack stack() {
        return stack;
    }

    public static void clear() {
        entry = null;
        stack = ItemStack.EMPTY;
        expiresAtMillis = 0;
    }

    public static void select(ScanResultsPayload.Entry selected, ItemStack selectedStack) {
        entry = selected;
        stack = selectedStack;
        expiresAtMillis = System.currentTimeMillis() + AnyfindConfig.get().highlightSeconds * 1000L;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        BlockPos playerPos = minecraft.player.blockPosition();
        ScanResultsPayload.Location closest = closestTo(playerPos);
        if (closest == null) {
            return;
        }
        int distance = (int) Math.round(Math.sqrt(closest.pos().distSqr(playerPos)));
        minecraft.player.sendSystemMessage(Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("message.anyfind.selected",
                        selectedStack.getHoverName(), selected.total(), selected.locations().size(),
                        closest.pos().toShortString(), distance).withStyle(ChatFormatting.WHITE)));
    }

    public static ScanResultsPayload.Location closestTo(BlockPos origin) {
        return locations().stream()
                .min(Comparator.comparingDouble(location -> location.pos().distSqr(origin)))
                .orElse(null);
    }
}
