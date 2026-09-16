package io.github.salasgthub.anyfind.client.config;

import io.github.salasgthub.anyfind.client.SearchKeyHandler;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Settings screen, reachable from Mod Menu.
 */
public class AnyfindConfigScreen extends Screen {

    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int SPACING = 4;

    private final Screen parent;
    private final AnyfindConfig config = AnyfindConfig.get();

    public AnyfindConfigScreen(Screen parent) {
        super(Component.translatable("screen.anyfind.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (width - WIDGET_WIDTH) / 2;
        int top = Math.max(32, height / 2 - 9 * (WIDGET_HEIGHT + SPACING) / 2);

        addRenderableWidget(CycleButton.builder((Integer radius) -> Component.literal(radius + " "
                        + Component.translatable("screen.anyfind.config.blocks").getString()), config.scanRadius)
                .withValues(AnyfindConfig.RADIUS_VALUES)
                .withTooltip(radius -> Tooltip.create(Component.translatable("screen.anyfind.config.radius.tooltip")))
                .create(left, top, WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.radius"),
                        (button, radius) -> config.scanRadius = radius));

        addRenderableWidget(CycleButton.builder(KeyModifier::label, config.modifier)
                .withValues(KeyModifier.values())
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.modifier.tooltip",
                        KeyMappingHelper.getBoundKeyOf(SearchKeyHandler.OPEN_SEARCH).getDisplayName())))
                .create(left, rowTop(top, 1), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.modifier"),
                        (button, value) -> config.modifier = value));

        addRenderableWidget(CycleButton.onOffBuilder(config.openFromContainers)
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.containers.tooltip")))
                .create(left, rowTop(top, 2), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.containers"),
                        (button, value) -> config.openFromContainers = value));

        addRenderableWidget(CycleButton.onOffBuilder(config.excludeStructures)
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.structures.tooltip")))
                .create(left, rowTop(top, 3), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.structures"),
                        (button, value) -> config.excludeStructures = value));

        addRenderableWidget(CycleButton.onOffBuilder(config.showBox)
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.box.tooltip")))
                .create(left, rowTop(top, 4), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.box"),
                        (button, value) -> config.showBox = value));

        addRenderableWidget(CycleButton.onOffBuilder(config.showPath)
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.path.tooltip")))
                .create(left, rowTop(top, 5), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.path"),
                        (button, value) -> config.showPath = value));

        addRenderableWidget(CycleButton.onOffBuilder(config.showMarker)
                .withTooltip(value -> Tooltip.create(Component.translatable("screen.anyfind.config.marker.tooltip")))
                .create(left, rowTop(top, 6), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.marker"),
                        (button, value) -> config.showMarker = value));

        addRenderableWidget(CycleButton.builder((Integer seconds) -> Component.literal(seconds + " s"),
                        config.highlightSeconds)
                .withValues(AnyfindConfig.HIGHLIGHT_SECONDS_VALUES)
                .withTooltip(seconds -> Tooltip.create(Component.translatable("screen.anyfind.config.duration.tooltip")))
                .create(left, rowTop(top, 7), WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("screen.anyfind.config.duration"),
                        (button, seconds) -> config.highlightSeconds = seconds));

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.anyfind.config.rebind",
                                KeyMappingHelper.getBoundKeyOf(SearchKeyHandler.OPEN_SEARCH).getDisplayName()),
                        button -> {
                            config.save();
                            minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
                        })
                .tooltip(Tooltip.create(Component.translatable("screen.anyfind.config.rebind.tooltip")))
                .bounds(left, rowTop(top, 8), WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, height - 32, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private static int rowTop(int top, int row) {
        return top + row * (WIDGET_HEIGHT + SPACING);
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
