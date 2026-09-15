package io.github.salasgthub.anyfind;

import io.github.salasgthub.anyfind.command.AnyfindCommands;
import io.github.salasgthub.anyfind.network.AnyfindNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Anyfind implements ModInitializer {

    public static final String MOD_ID = "anyfind";

    @Override
    public void onInitialize() {
        AnyfindNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AnyfindCommands.register(dispatcher));
    }
}
