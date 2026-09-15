package io.github.salasgthub.anyfind.network;

import io.github.salasgthub.anyfind.command.AnyfindCommands;
import io.github.salasgthub.anyfind.scan.ContainerScanner;
import io.github.salasgthub.anyfind.scan.ScanResult;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class AnyfindNetworking {

    private AnyfindNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RequestScanPayload.TYPE, RequestScanPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ScanResultsPayload.TYPE, ScanResultsPayload.CODEC);

        // Receivers run on the server thread, where reading block entities is safe.
        ServerPlayNetworking.registerGlobalReceiver(RequestScanPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // The radius comes from the client config, so clamp it to a sane range here.
            int radius = Math.clamp(payload.radius(), 1, AnyfindCommands.MAX_RADIUS);
            ScanResult result = ContainerScanner.scan(player.level(), player.blockPosition(), radius,
                    payload.excludeStructures());
            ServerPlayNetworking.send(player, ScanResultsPayload.from(result, radius));
        });
    }
}
