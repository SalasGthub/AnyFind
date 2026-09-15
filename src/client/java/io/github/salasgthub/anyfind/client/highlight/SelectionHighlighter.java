package io.github.salasgthub.anyfind.client.highlight;

import io.github.salasgthub.anyfind.client.SearchSelection;
import io.github.salasgthub.anyfind.client.config.AnyfindConfig;
import io.github.salasgthub.anyfind.network.ScanResultsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Draws the selected item's containers in the world: a box around each one, a trail of particles running
 * from the player to the closest one, and a column of particles over it.
 */
public final class SelectionHighlighter {

    /** Containers drawn at once; the rest would just be noise. */
    private static final int MAX_BOXES = 12;
    private static final double MAX_DISTANCE = 160.0;
    /** How close the player has to get for the highlight to turn itself off. */
    private static final double ARRIVAL_DISTANCE = 2.5;
    private static final int MARKER_INTERVAL_TICKS = 4;
    /** Distance between dots on the trail, in blocks. */
    private static final double TRAIL_SPACING = 1.25;
    /** Ticks it takes a dot to travel one gap, i.e. how fast the trail flows. */
    private static final int FLOW_PERIOD_TICKS = 10;
    private static final int MAX_TRAIL_PARTICLES = 80;

    private static final int BOX_STROKE_COLOR = 0xFFFFC74A;
    private static final int BOX_FILL_COLOR = 0x33FFC74A;
    private static final int CLOSEST_STROKE_COLOR = 0xFF6BE675;
    private static final int CLOSEST_FILL_COLOR = 0x3D6BE675;

    private static int tickCounter;

    private SelectionHighlighter() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SelectionHighlighter::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        tickCounter++;
        if (client.level == null || client.player == null || !SearchSelection.isActive()) {
            return;
        }

        BlockPos playerBlock = client.player.blockPosition();
        List<ScanResultsPayload.Location> targets = SearchSelection.locations().stream()
                .filter(location -> location.pos().distSqr(playerBlock) < MAX_DISTANCE * MAX_DISTANCE)
                .sorted(Comparator.comparingDouble(location -> location.pos().distSqr(playerBlock)))
                .limit(MAX_BOXES)
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        BlockPos closest = targets.getFirst().pos();
        if (closest.distSqr(playerBlock) <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
            announceArrival(client);
            SearchSelection.clear();
            return;
        }

        AnyfindConfig config = AnyfindConfig.get();
        Vec3 from = client.player.getEyePosition().add(0.0, -0.5, 0.0);
        Vec3 to = Vec3.atCenterOf(closest);

        if (config.showBox) {
            // Gizmos are collected per tick and drawn by the level renderer.
            try (Gizmos.TemporaryCollection ignored = client.collectPerTickGizmos()) {
                for (ScanResultsPayload.Location location : targets) {
                    boolean isClosest = location.pos().equals(closest);
                    Gizmos.cuboid(location.pos(), GizmoStyle.strokeAndFill(
                                    isClosest ? CLOSEST_STROKE_COLOR : BOX_STROKE_COLOR, 2.5f,
                                    isClosest ? CLOSEST_FILL_COLOR : BOX_FILL_COLOR))
                            .setAlwaysOnTop();
                }
            }
        }

        if (config.showPath) {
            spawnTrail(client, from, to);
        }
        if (config.showMarker && tickCounter % MARKER_INTERVAL_TICKS == 0) {
            spawnMarker(client, closest);
        }
    }

    /** A column of particles over the chest, visible without looking for the outline. */
    private static void spawnMarker(Minecraft client, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (int step = 0; step < 5; step++) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    center.x(), center.y() + 0.8 + step * 0.3, center.z(),
                    0.0, 0.006, 0.0);
        }
        client.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                center.x(), center.y() + 1.2, center.z(), 0.0, 0.0, 0.0);
    }

    /**
     * Dots running along the straight line to the chest. The starting offset advances every tick, so the
     * dots read as flowing towards the container instead of sitting still.
     */
    private static void spawnTrail(Minecraft client, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 0.5) {
            return;
        }
        Vec3 direction = delta.scale(1.0 / distance);
        double phase = (tickCounter % FLOW_PERIOD_TICKS) / (double) FLOW_PERIOD_TICKS * TRAIL_SPACING;
        int spawned = 0;
        for (double travelled = phase; travelled < distance && spawned < MAX_TRAIL_PARTICLES;
                travelled += TRAIL_SPACING, spawned++) {
            Vec3 point = from.add(direction.scale(travelled));
            client.level.addParticle(ParticleTypes.END_ROD, point.x(), point.y(), point.z(), 0.0, 0.0, 0.0);
        }
    }

    private static void announceArrival(Minecraft client) {
        client.player.sendSystemMessage(Component.literal("[AnyFind] ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("message.anyfind.arrived", SearchSelection.stack().getHoverName())
                        .withStyle(ChatFormatting.WHITE)));
    }
}
