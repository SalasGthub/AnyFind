package io.github.salasgthub.anyfind.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.salasgthub.anyfind.scan.ContainerScanner;
import io.github.salasgthub.anyfind.scan.ScanRequest;
import io.github.salasgthub.anyfind.scan.ScanResult;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AnyfindCommands {

    public static final int DEFAULT_RADIUS = 32;
    public static final int MAX_RADIUS = 128;
    private static final int SUMMARY_LINES = 10;

    private AnyfindCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("anyfind")
                .then(Commands.literal("scan")
                        .executes(context -> scan(context.getSource(), DEFAULT_RADIUS, true))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                .executes(context -> scan(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius"), true))
                                .then(Commands.argument("includeStructures", BoolArgumentType.bool())
                                        .executes(context -> scan(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius"),
                                                !BoolArgumentType.getBool(context, "includeStructures"))))))
                .then(ZoneCommands.build()));
    }

    private static int scan(CommandSourceStack source, int radius, boolean excludeStructures) {
        BlockPos center = BlockPos.containing(source.getPosition());
        ScanRequest request = ScanRequest.resolve(source.getLevel(), center, radius, excludeStructures);
        long start = System.nanoTime();
        ScanResult result = request.run(source.getLevel());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        String area = request.zoneName().isEmpty() ? "radio " + radius : "zona " + request.zoneName();
        source.sendSuccess(() -> Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(result.containerCount() + " contenedores, "
                        + result.distinctItemCount() + " items distintos (" + area + ", "
                        + elapsedMs + " ms)").withStyle(ChatFormatting.WHITE)), false);

        if (result.skippedLootContainers() > 0) {
            source.sendSuccess(() -> Component.literal("  " + result.skippedLootContainers()
                    + " contenedores con loot sin generar fueron ignorados").withStyle(ChatFormatting.GRAY), false);
        }
        if (result.truncated()) {
            source.sendSuccess(() -> Component.literal("  El escaneo se cortó en "
                    + ContainerScanner.MAX_CONTAINERS + " contenedores").withStyle(ChatFormatting.GRAY), false);
        }
        if (result.skippedStructureContainers() > 0) {
            source.sendSuccess(() -> Component.literal("  " + result.skippedStructureContainers()
                    + " contenedores dentro de estructuras fueron ignorados").withStyle(ChatFormatting.GRAY), false);
        }

        List<ScanResult.ItemEntry> entries = result.sortedByTotal();
        for (ScanResult.ItemEntry entry : entries.subList(0, Math.min(SUMMARY_LINES, entries.size()))) {
            source.sendSuccess(() -> summaryLine(entry, center), false);
        }
        if (entries.size() > SUMMARY_LINES) {
            source.sendSuccess(() -> Component.literal("  ... y " + (entries.size() - SUMMARY_LINES) + " más")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return result.containerCount();
    }

    private static MutableComponent summaryLine(ScanResult.ItemEntry entry, BlockPos center) {
        BlockPos closest = entry.closestTo(center);
        return Component.literal("  " + entry.total() + "x ").withStyle(ChatFormatting.YELLOW)
                .append(new ItemStack(entry.item()).getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" en " + entry.locations().size() + " contenedor(es), más cercano: "
                        + closest.toShortString()).withStyle(ChatFormatting.GRAY));
    }
}
