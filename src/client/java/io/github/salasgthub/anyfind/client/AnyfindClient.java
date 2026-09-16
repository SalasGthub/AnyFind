package io.github.salasgthub.anyfind.client;

import io.github.salasgthub.anyfind.client.highlight.OpenedContainerWatcher;
import io.github.salasgthub.anyfind.client.highlight.SelectionHighlighter;
import io.github.salasgthub.anyfind.client.screen.ItemSearchScreen;
import io.github.salasgthub.anyfind.network.ScanResultsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AnyfindClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SearchKeyHandler.register();
        SelectionHighlighter.register();
        OpenedContainerWatcher.register();

        // Don't keep highlighting containers from a world we already left.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SearchSelection.clear());

        ClientPlayNetworking.registerGlobalReceiver(ScanResultsPayload.TYPE, (payload, context) -> {
            if (context.client().gui.screen() instanceof ItemSearchScreen searchScreen) {
                searchScreen.setResults(payload);
            }
        });
    }
}
