package io.github.salasgthub.anyfind.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.salasgthub.anyfind.client.config.AnyfindConfigScreen;

/**
 * Adds the settings button to AnyFind's entry in Mod Menu. Only loaded when Mod Menu is installed.
 */
public class AnyfindModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AnyfindConfigScreen::new;
    }
}
