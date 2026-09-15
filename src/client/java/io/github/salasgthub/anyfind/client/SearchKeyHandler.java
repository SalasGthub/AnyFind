package io.github.salasgthub.anyfind.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.salasgthub.anyfind.Anyfind;
import io.github.salasgthub.anyfind.client.config.AnyfindConfig;
import io.github.salasgthub.anyfind.client.screen.ItemSearchScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

/**
 * Opens the search screen with Ctrl + the "open search" key (F by default), both in the world and from
 * inventory/container screens.
 */
public final class SearchKeyHandler {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Anyfind.MOD_ID, "main"));

    public static final KeyMapping OPEN_SEARCH = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.anyfind.open_search", InputConstants.KEY_F, CATEGORY));

    private SearchKeyHandler() {
    }

    public static void register() {
        // START runs before vanilla handles key presses in the same tick, so the swap-offhand click that
        // shares the F key can still be cancelled.
        ClientTickEvents.START_CLIENT_TICK.register(SearchKeyHandler::onClientTick);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return;
            }
            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, event) -> {
                AnyfindConfig config = AnyfindConfig.get();
                if (!config.openFromContainers || !OPEN_SEARCH.matches(event)
                        || (config.requireCtrl && !event.hasControlDown())) {
                    return true;
                }
                if (client.player != null) {
                    client.player.closeContainer();
                }
                open(client);
                return false;
            });
        });
    }

    private static void onClientTick(Minecraft client) {
        boolean pressed = false;
        while (OPEN_SEARCH.consumeClick()) {
            pressed = true;
        }
        if (!pressed || client.player == null || client.gui.screen() != null) {
            return;
        }
        if (AnyfindConfig.get().requireCtrl && !isControlDown()) {
            return;
        }
        KeyMapping swapOffhand = client.options.keySwapOffhand;
        if (KeyMappingHelper.getBoundKeyOf(swapOffhand).equals(KeyMappingHelper.getBoundKeyOf(OPEN_SEARCH))) {
            while (swapOffhand.consumeClick()) {
                // Discard: Ctrl + F should only open the search, not swap hands.
            }
        }
        open(client);
    }

    private static boolean isControlDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
    }

    private static void open(Minecraft client) {
        client.gui.setScreen(new ItemSearchScreen());
    }
}
