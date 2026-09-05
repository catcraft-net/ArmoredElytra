package nl.pim16aap2.armoredElytra.handlers;

/**
 * Safety policy for automatically removing a stale smithing placeholder marker.
 * <p>
 * A plain elytra carrying only the placeholder marker is indistinguishable from a leaked recipe placeholder, so it
 * must never be auto-repaired. Automatic repair is only safe when the marker is known to have been inherited from an
 * already-armored input and the generated result is also a genuine armored elytra.
 */
final class SmithingPlaceholderPolicy
{
    private SmithingPlaceholderPolicy()
    {
    }

    static boolean canAutoRepair(
        boolean inputHasPlaceholderMarker,
        boolean inputIsArmored,
        boolean resultIsArmored)
    {
        return inputHasPlaceholderMarker && inputIsArmored && resultIsArmored;
    }
}
