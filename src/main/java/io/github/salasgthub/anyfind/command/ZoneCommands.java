package io.github.salasgthub.anyfind.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.salasgthub.anyfind.zone.Zone;
import io.github.salasgthub.anyfind.zone.ZoneStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * {@code /anyfind zone ...}: define, list and delete the chest areas of a dimension.
 */
public final class ZoneCommands {

    private static final int DEFAULT_ZONE_RADIUS = 24;

    private static final SuggestionProvider<CommandSourceStack> ZONE_NAMES = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    ZoneStorage.of(context.getSource().getLevel()).all().stream().map(Zone::name).toList(),
                    builder);

    private ZoneCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("zone")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> create(context, zoneAroundSource(context, DEFAULT_ZONE_RADIUS)))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, Zone.MAX_SIDE / 2))
                                        .executes(context -> create(context, zoneAroundSource(context,
                                                IntegerArgumentType.getInteger(context, "radius")))))
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .executes(context -> create(context, Zone.ofCorners(
                                                        StringArgumentType.getString(context, "name"),
                                                        BlockPosArgument.getBlockPos(context, "from"),
                                                        BlockPosArgument.getBlockPos(context, "to"))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(ZONE_NAMES)
                                .executes(ZoneCommands::remove)))
                .then(Commands.literal("list").executes(ZoneCommands::list))
                .then(Commands.literal("here").executes(ZoneCommands::here));
    }

    private static Zone zoneAroundSource(CommandContext<CommandSourceStack> context, int radius) {
        return Zone.ofRadius(StringArgumentType.getString(context, "name"),
                BlockPos.containing(context.getSource().getPosition()), radius);
    }

    private static int create(CommandContext<CommandSourceStack> context, Zone zone) {
        CommandSourceStack source = context.getSource();
        if (zone.isTooBig()) {
            source.sendFailure(Component.literal("La zona es demasiado grande: cada lado puede tener hasta "
                    + Zone.MAX_SIDE + " bloques"));
            return 0;
        }

        Zone replaced = ZoneStorage.of(source.getLevel()).put(zone);
        source.sendSuccess(() -> prefix()
                .append(Component.literal((replaced == null ? "Zona creada: " : "Zona reemplazada: ") + zone.name()
                        + " " + zone.describeArea()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        Zone removed = ZoneStorage.of(source.getLevel()).remove(name);
        if (removed == null) {
            source.sendFailure(Component.literal("No existe la zona " + name));
            return 0;
        }
        source.sendSuccess(() -> prefix()
                .append(Component.literal("Zona borrada: " + removed.name()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<Zone> zones = ZoneStorage.of(source.getLevel()).all();
        if (zones.isEmpty()) {
            source.sendSuccess(() -> prefix().append(Component.literal(
                    "No hay zonas en esta dimensión. Creá una con /anyfind zone create <nombre>")
                    .withStyle(ChatFormatting.GRAY)), false);
            return 0;
        }
        source.sendSuccess(() -> prefix()
                .append(Component.literal(zones.size() + " zona(s):").withStyle(ChatFormatting.WHITE)), false);
        for (Zone zone : zones) {
            source.sendSuccess(() -> Component.literal("  " + zone.name() + " ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(zone.describeArea()).withStyle(ChatFormatting.GRAY)), false);
        }
        return zones.size();
    }

    private static int here(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Zone zone = ZoneStorage.of(source.getLevel()).zoneAt(pos);
        if (zone == null) {
            source.sendSuccess(() -> prefix().append(Component.literal("No estás dentro de ninguna zona")
                    .withStyle(ChatFormatting.GRAY)), false);
            return 0;
        }
        source.sendSuccess(() -> prefix().append(Component.literal("Estás en la zona " + zone.name() + " "
                + zone.describeArea()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static net.minecraft.network.chat.MutableComponent prefix() {
        return Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD);
    }
}
