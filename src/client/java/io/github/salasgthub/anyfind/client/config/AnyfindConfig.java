package io.github.salasgthub.anyfind.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.salasgthub.anyfind.Anyfind;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Client settings, stored in {@code config/anyfind.json}. Editable from Mod Menu.
 */
public class AnyfindConfig {

    public static final List<Integer> RADIUS_VALUES = List.of(16, 32, 48, 64, 96, 128);
    public static final List<Integer> HIGHLIGHT_SECONDS_VALUES = List.of(15, 30, 60, 120, 300);

    private static final Logger LOGGER = LoggerFactory.getLogger(Anyfind.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(Anyfind.MOD_ID + ".json");

    private static AnyfindConfig instance;

    /** How far around the player the server scans. */
    public int scanRadius = 32;
    /** Key held together with the search key (avoids clashing with the vanilla use of that key). */
    public KeyModifier modifier = KeyModifier.CTRL;
    /** Ignore containers inside generated structures, so dungeon and village chests stay out of the results. */
    public boolean excludeStructures = true;
    /** Also read hoppers, droppers, dispensers, furnaces and the like. */
    public boolean includeOtherContainers = false;
    /** Also read what is inside shulker boxes stored in a container. */
    public boolean includeNestedContainers = true;
    /** Whether the search key also works while an inventory or container screen is open. */
    public boolean openFromContainers = true;
    /** Draw a box around each container holding the selected item. */
    public boolean showBox = true;
    /** Trail of particles from the player to the closest container. */
    public boolean showPath = true;
    /** Column of particles over the closest container. */
    public boolean showMarker = true;
    /** How long the highlight stays on screen. */
    public int highlightSeconds = 60;

    public static AnyfindConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static AnyfindConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                AnyfindConfig loaded = GSON.fromJson(reader, AnyfindConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Could not read {}, using defaults", PATH, exception);
            }
        }
        return new AnyfindConfig();
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not write {}", PATH, exception);
        }
    }

    private void sanitize() {
        if (modifier == null) {
            modifier = KeyModifier.CTRL;
        }
        if (!RADIUS_VALUES.contains(scanRadius)) {
            scanRadius = Math.clamp(scanRadius, RADIUS_VALUES.getFirst(), RADIUS_VALUES.getLast());
        }
        if (!HIGHLIGHT_SECONDS_VALUES.contains(highlightSeconds)) {
            highlightSeconds = Math.clamp(highlightSeconds,
                    HIGHLIGHT_SECONDS_VALUES.getFirst(), HIGHLIGHT_SECONDS_VALUES.getLast());
        }
    }
}
