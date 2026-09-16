package io.github.salasgthub.anyfind.scan;

/**
 * What counts as a container for a scan. Comes from the client config.
 *
 * @param includeOtherContainers also read hoppers, droppers, dispensers, furnaces and the like, on top of
 *                               chests, barrels and shulker boxes
 * @param includeNestedContainers also read what is inside shulker boxes stored in a container
 */
public record ScanOptions(boolean includeOtherContainers, boolean includeNestedContainers) {

    public static final ScanOptions DEFAULT = new ScanOptions(false, true);
}
