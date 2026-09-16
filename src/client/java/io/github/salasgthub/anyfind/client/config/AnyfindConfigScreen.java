package io.github.salasgthub.anyfind.client.config;

import io.github.salasgthub.anyfind.client.SearchKeyHandler;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Settings screen, reachable from Mod Menu. Laid out in two columns so every option fits without scrolling.
 */
public class AnyfindConfigScreen extends Screen {

    private static final int COLUMN_WIDTH = 158;
    private static final int WIDGET_HEIGHT = 20;
    private static final int SPACING = 4;
    private static final int ROWS_PER_COLUMN = 5;

    private final Screen parent;
    private final AnyfindConfig config = AnyfindConfig.get();

    public AnyfindConfigScreen(Screen parent) {
        super(Component.translatable("screen.anyfind.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<AbstractWidget> options = new ArrayList<>();

        options.add(CycleButton.builder((Integer radius) -> Component.literal(radius + " "
                        + Component.translatable("screen.anyfind.config.blocks").getString()), config.scanRadius)
                .withValues(AnyfindConfig.RADIUS_VALUES)
                .withTooltip(radius -> Tooltip.create(Component.translatable("screen.anyfind.config.radius.tooltip")))
                .create(0, 0, COLUMN_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.radius"),
                        (button, radius) -> config.scanRadius = radius));

        options.add(CycleButton.builder(KeyModifier::label, config.modifier)
                .withValues(KeyModifier.values())
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.modifier.tooltip",
                        KeyMappingHelper.getBoundKeyOf(SearchKeyHandler.OPEN_SEARCH).getDisplayName())))
                .create(0, 0, COLUMN_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.modifier"),
                        (button, value) -> config.modifier = value));

        options.add(toggle("containers", () -> config.openFromContainers, value -> config.openFromContainers = value));
        options.add(toggle("structures", () -> config.excludeStructures, value -> config.excludeStructures = value));
        options.add(toggle("others", () -> config.includeOtherContainers,
                value -> config.includeOtherContainers = value));
        options.add(toggle("nested", () -> config.includeNestedContainers,
                value -> config.includeNestedContainers = value));
        options.add(toggle("box", () -> config.showBox, value -> config.showBox = value));
        options.add(toggle("path", () -> config.showPath, value -> config.showPath = value));
        options.add(toggle("marker", () -> config.showMarker, value -> config.showMarker = value));

        options.add(CycleButton.builder((Integer seconds) -> Component.literal(seconds + " s"),
                        config.highlightSeconds)
                .withValues(AnyfindConfig.HIGHLIGHT_SECONDS_VALUES)
                .withTooltip(seconds -> Tooltip.create(Component.translatable("screen.anyfind.config.duration.tooltip")))
                .create(0, 0, COLUMN_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.duration"),
                        (button, seconds) -> config.highlightSeconds = seconds));

        int rowHeight = WIDGET_HEIGHT + SPACING;
        int gridWidth = 2 * COLUMN_WIDTH + SPACING;
        int left = (width - gridWidth) / 2;
        int top = Math.max(32, (height - ROWS_PER_COLUMN * rowHeight) / 2 - rowHeight);

        for (int index = 0; index < options.size(); index++) {
            AbstractWidget widget = options.get(index);
            widget.setX(left + (index / ROWS_PER_COLUMN) * (COLUMN_WIDTH + SPACING));
            widget.setY(top + (index % ROWS_PER_COLUMN) * rowHeight);
            addRenderableWidget(widget);
        }

        int footerTop = top + ROWS_PER_COLUMN * rowHeight + SPACING;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.anyfind.config.rebind",
                                KeyMappingHelper.getBoundKeyOf(SearchKeyHandler.OPEN_SEARCH).getDisplayName()),
                        button -> {
                            config.save();
                            minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
                        })
                .tooltip(Tooltip.create(Component.translatable("screen.anyfind.config.rebind.tooltip")))
                .bounds(left, footerTop, gridWidth, WIDGET_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + (gridWidth - COLUMN_WIDTH) / 2, footerTop + WIDGET_HEIGHT + SPACING,
                        COLUMN_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private CycleButton<Boolean> toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return CycleButton.onOffBuilder(getter.getAsBoolean())
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config." + key + ".tooltip")))
                .create(0, 0, COLUMN_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config." + key),
                        (button, value) -> setter.accept(value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.gui.setScreen(parent);
    }
}
