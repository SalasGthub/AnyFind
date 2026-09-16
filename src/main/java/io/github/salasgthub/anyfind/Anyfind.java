package io.github.salasgthub.anyfind;

import io.github.salasgthub.anyfind.command.AnyfindCommands;
import io.github.salasgthub.anyfind.network.AnyfindNetworking;
import net.fabricmc.api.ModInitializer;
import io.github.salasgthub.anyfind.zone.ZoneSuggester;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class Anyfind implements ModInitializer {

    public static final String MOD_ID = "anyfind";

    @Override
    public void onInitialize() {
        AnyfindNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AnyfindCommands.register(dispatcher));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ZoneSuggester.forget(handler.getPlayer()));
    }
}
