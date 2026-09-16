package io.github.salasgthub.anyfind.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Key that has to be held together with the search key. Without one, the search key alone opens the screen,
 * which is fine when it is bound to a key vanilla does not already use.
 */
public enum KeyModifier {

    NONE("none"),
    CTRL("ctrl", InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL),
    ALT("alt", InputConstants.KEY_LALT, InputConstants.KEY_RALT),
    SHIFT("shift", InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT);

    private final String id;
    private final int[] keys;

    KeyModifier(String id, int... keys) {
        this.id = id;
        this.keys = keys;
    }

    public Component label() {
        return Component.translatable("screen.anyfind.config.modifier." + id);
    }

    /** In the world there is no key event, so the keyboard state is read directly. */
    public boolean isHeld() {
        for (int key : keys) {
            if (InputConstants.isKeyDown(key)) {
                return true;
            }
        }
        return keys.length == 0;
    }

    /** Inside a screen the event already carries the modifiers, and handles Cmd on macOS. */
    public boolean isHeld(KeyEvent event) {
        return switch (this) {
            case NONE -> true;
            case CTRL -> event.hasControlDown();
            case ALT -> event.hasAltDown();
            case SHIFT -> event.hasShiftDown();
        };
    }
}
