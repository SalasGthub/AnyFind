package io.github.salasgthub.anyfind.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.salasgthub.anyfind.client.SearchSelection;
import io.github.salasgthub.anyfind.client.config.AnyfindConfig;
import io.github.salasgthub.anyfind.network.RequestScanPayload;
import io.github.salasgthub.anyfind.network.ScanResultsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search box plus a grid with every item found in the nearby containers.
 */
public class ItemSearchScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 8;
    private static final int SEARCH_HEIGHT = 18;
    private static final int RESCAN_WIDTH = 60;
    private static final int SPACING = 4;
    private static final int STATUS_HEIGHT = 14;

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int PANEL_BORDER_COLOR = 0xFF555555;
    private static final int SLOT_COLOR = 0x60000000;
    private static final int SLOT_HOVER_COLOR = 0x80FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int MUTED_TEXT_COLOR = 0xFFA0A0A0;

    private enum State { LOADING, READY, UNSUPPORTED }

    private State state = State.LOADING;
    private ScanResultsPayload results;
    private final List<ResultSlot> allSlots = new ArrayList<>();
    private List<ResultSlot> visibleSlots = List.of();

    private EditBox searchBox;
    private Button rescanButton;
    private int columns;
    private int rows;
    private int gridLeft;
    private int gridTop;
    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int scrollRow;

    private record ResultSlot(ScanResultsPayload.Entry entry, ItemStack stack, String searchText) {
    }

    public ItemSearchScreen() {
        super(Component.translatable("screen.anyfind.search.title"));
    }

    @Override
    protected void init() {
        columns = Math.clamp((width - 2 * PADDING - 40) / SLOT_SIZE, 9, 14);
        rows = Math.clamp((height - SEARCH_HEIGHT - STATUS_HEIGHT - 4 * PADDING - 40) / SLOT_SIZE, 3, 8);

        int panelWidth = columns * SLOT_SIZE + 2 * PADDING;
        int panelHeight = PADDING + SEARCH_HEIGHT + PADDING + rows * SLOT_SIZE + PADDING + STATUS_HEIGHT;
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        panelRight = panelLeft + panelWidth;
        panelBottom = panelTop + panelHeight;
        gridLeft = panelLeft + PADDING;
        gridTop = panelTop + PADDING + SEARCH_HEIGHT + PADDING;

        String previousQuery = searchBox != null ? searchBox.getValue() : "";
        int searchWidth = columns * SLOT_SIZE - RESCAN_WIDTH - SPACING;
        searchBox = new EditBox(font, gridLeft, panelTop + PADDING, searchWidth, SEARCH_HEIGHT,
                Component.translatable("screen.anyfind.search.hint"));
        searchBox.setHint(Component.translatable("screen.anyfind.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(64);
        searchBox.setValue(previousQuery);
        searchBox.setResponder(query -> applyFilter());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        rescanButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.anyfind.search.rescan"), button -> requestScan())
                .tooltip(Tooltip.create(Component.translatable("screen.anyfind.search.rescan.tooltip")))
                .bounds(gridLeft + searchWidth + SPACING, panelTop + PADDING, RESCAN_WIDTH, SEARCH_HEIGHT)
                .build());
        rescanButton.active = state != State.LOADING;

        // init() also runs on window resize; only ask the server once.
        if (results == null && state == State.LOADING) {
            requestScan();
        }
        applyFilter();
    }

    private void requestScan() {
        if (!ClientPlayNetworking.canSend(RequestScanPayload.TYPE)) {
            state = State.UNSUPPORTED;
            updateRescanButton();
            return;
        }
        state = State.LOADING;
        updateRescanButton();
        AnyfindConfig config = AnyfindConfig.get();
        ClientPlayNetworking.send(new RequestScanPayload(config.scanRadius, config.excludeStructures,
                config.includeOtherContainers, config.includeNestedContainers));
    }

    private void updateRescanButton() {
        if (rescanButton != null) {
            rescanButton.active = state != State.LOADING;
        }
    }

    public void setResults(ScanResultsPayload payload) {
        results = payload;
        state = State.READY;
        updateRescanButton();
        allSlots.clear();
        for (ScanResultsPayload.Entry entry : payload.entries()) {
            ItemStack stack = new ItemStack(entry.item());
            String searchText = (stack.getHoverName().getString() + " "
                    + BuiltInRegistries.ITEM.getKey(entry.item())).toLowerCase(Locale.ROOT);
            allSlots.add(new ResultSlot(entry, stack, searchText));
        }
        applyFilter();
    }

    private void applyFilter() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        visibleSlots = query.isEmpty()
                ? allSlots
                : allSlots.stream().filter(slot -> slot.searchText().contains(query)).toList();
        scrollRow = 0;
    }

    private int maxScrollRow() {
        int totalRows = (visibleSlots.size() + columns - 1) / columns;
        return Math.max(0, totalRows - rows);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_COLOR);
        graphics.outline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, PANEL_BORDER_COLOR);
        graphics.centeredText(font, title, width / 2, panelTop - font.lineHeight - 4, TEXT_COLOR);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        ResultSlot hovered = null;
        int firstIndex = scrollRow * columns;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = gridLeft + column * SLOT_SIZE;
                int y = gridTop + row * SLOT_SIZE;
                int index = firstIndex + row * columns + column;
                boolean isHovered = isInside(mouseX, mouseY, x, y);
                graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1,
                        isHovered && index < visibleSlots.size() ? SLOT_HOVER_COLOR : SLOT_COLOR);
                if (index < visibleSlots.size()) {
                    ResultSlot slot = visibleSlots.get(index);
                    graphics.item(slot.stack(), x + 1, y + 1);
                    graphics.itemDecorations(font, slot.stack(), x + 1, y + 1, formatCount(slot.entry().total()));
                    if (isHovered) {
                        hovered = slot;
                    }
                }
            }
        }

        graphics.text(font, statusText(), gridLeft, panelBottom - STATUS_HEIGHT, MUTED_TEXT_COLOR);

        if (maxScrollRow() > 0) {
            int trackHeight = rows * SLOT_SIZE;
            int thumbHeight = Math.max(8, trackHeight * rows / (maxScrollRow() + rows));
            int thumbTop = gridTop + (trackHeight - thumbHeight) * scrollRow / maxScrollRow();
            graphics.fill(panelRight - 5, thumbTop, panelRight - 3, thumbTop + thumbHeight, MUTED_TEXT_COLOR);
        }

        if (hovered != null) {
            graphics.setComponentTooltipForNextFrame(font, tooltipFor(hovered), mouseX, mouseY);
        }
    }

    private Component statusText() {
        return switch (state) {
            case LOADING -> Component.translatable("screen.anyfind.search.loading");
            case UNSUPPORTED -> Component.translatable("screen.anyfind.search.unsupported");
            case READY -> {
                if (results.entries().isEmpty()) {
                    yield results.zoneName().isEmpty()
                            ? Component.translatable("screen.anyfind.search.empty", results.radius())
                            : Component.translatable("screen.anyfind.search.empty_zone", results.zoneName());
                }
                if (visibleSlots.isEmpty()) {
                    yield Component.translatable("screen.anyfind.search.no_matches");
                }
                MutableComponent summary = results.zoneName().isEmpty()
                        ? Component.translatable("screen.anyfind.search.summary",
                                visibleSlots.size(), results.containerCount(), results.radius())
                        : Component.translatable("screen.anyfind.search.summary_zone",
                                visibleSlots.size(), results.containerCount(), results.zoneName());
                int ignored = results.skippedStructureContainers() + results.skippedLootContainers();
                yield ignored == 0
                        ? summary
                        : summary.append(Component.translatable("screen.anyfind.search.ignored", ignored));
            }
        };
    }

    private List<Component> tooltipFor(ResultSlot slot) {
        return List.of(
                slot.stack().getHoverName(),
                Component.translatable("screen.anyfind.search.tooltip.total", slot.entry().total())
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("screen.anyfind.search.tooltip.containers", slot.entry().locations().size())
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("screen.anyfind.search.tooltip.click").withStyle(ChatFormatting.YELLOW));
    }

    private static String formatCount(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        if (count < 10_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return (count / 1000) + "k";
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    private ResultSlot slotAt(double mouseX, double mouseY) {
        if (mouseX < gridLeft || mouseY < gridTop) {
            return null;
        }
        int column = (int) ((mouseX - gridLeft) / SLOT_SIZE);
        int row = (int) ((mouseY - gridTop) / SLOT_SIZE);
        if (column >= columns || row >= rows) {
            return null;
        }
        int index = (scrollRow + row) * columns + column;
        return index < visibleSlots.size() ? visibleSlots.get(index) : null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            ResultSlot slot = slotAt(event.x(), event.y());
            if (slot != null) {
                select(slot);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && maxScrollRow() > 0) {
            scrollRow = Math.clamp(scrollRow - (long) Math.signum(scrollY), 0, maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) && !visibleSlots.isEmpty()) {
            select(visibleSlots.getFirst());
            return true;
        }
        return super.keyPressed(event);
    }

    private void select(ResultSlot slot) {
        onClose();
        SearchSelection.select(slot.entry(), slot.stack());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
