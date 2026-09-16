package io.github.salasgthub.anyfind.zone;

import io.github.salasgthub.anyfind.scan.ScanRequest;
import io.github.salasgthub.anyfind.scan.ScanResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When a scan without a zone finds a cluster of containers, offers to turn it into a zone. It only suggests:
 * creating the zone is always the player's call.
 */
public final class ZoneSuggester {

    /** Containers that have to show up before it is worth suggesting anything. */
    private static final int CLUSTER_SIZE = 8;
    private static final long COOLDOWN_MILLIS = 10 * 60 * 1000L;

    private static final Map<UUID, Long> lastSuggestion = new HashMap<>();

    private ZoneSuggester() {
    }

    public static void maybeSuggest(ServerPlayer player, ScanRequest request, ScanResult result) {
        if (!request.zoneName().isEmpty() || result.containerCount() < CLUSTER_SIZE) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastSuggestion.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_MILLIS) {
            return;
        }
        lastSuggestion.put(player.getUUID(), now);

        Component command = Component.literal("/anyfind zone create ").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand("/anyfind zone create ")));
        player.sendSystemMessage(Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(result.containerCount()
                                + " contenedores por acá. Podés guardarlos como zona: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(command));
    }

    /** Forgets the cooldown of a player who left, so the map does not grow forever. */
    public static void forget(ServerPlayer player) {
        lastSuggestion.remove(player.getUUID());
    }
}
