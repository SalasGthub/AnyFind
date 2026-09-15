package io.github.salasgthub.anyfind.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Locale;

/**
 * A named box the player declared as their chest area.
 */
public record Zone(String name, BoundingBox area) {

    /** Longest allowed side, to keep scans bounded. */
    public static final int MAX_SIDE = 256;

    public static final Codec<Zone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Zone::name),
            BoundingBox.CODEC.fieldOf("area").forGetter(Zone::area)
    ).apply(instance, Zone::new));

    public static Zone ofCorners(String name, BlockPos first, BlockPos second) {
        return new Zone(name, BoundingBox.fromCorners(first, second));
    }

    public static Zone ofRadius(String name, BlockPos center, int radius) {
        return ofCorners(name,
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius));
    }

    public String key() {
        return name.toLowerCase(Locale.ROOT);
    }

    public boolean contains(BlockPos pos) {
        return area.isInside(pos);
    }

    public long volume() {
        return (long) area.getXSpan() * area.getYSpan() * area.getZSpan();
    }

    public boolean isTooBig() {
        return area.getXSpan() > MAX_SIDE || area.getYSpan() > MAX_SIDE || area.getZSpan() > MAX_SIDE;
    }

    public String describeArea() {
        return "(" + area.minX() + " " + area.minY() + " " + area.minZ() + ") → ("
                + area.maxX() + " " + area.maxY() + " " + area.maxZ() + ")";
    }
}
