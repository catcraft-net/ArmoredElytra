package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.util.itemInput.InputAction;

/**
 * Safety policy for recovering stale smithing placeholder markers.
 */
final class SmithingPlaceholderPolicy
{
    private SmithingPlaceholderPolicy()
    {
    }

    /**
     * A marker on an already-armored input can be removed from an armored result because the result is provably a real
     * ArmoredElytra item and not the plain recipe placeholder.
     */
    static boolean canAutoRepair(
        boolean inputHasPlaceholderMarker,
        boolean inputIsArmored,
        boolean resultIsArmored)
    {
        return inputHasPlaceholderMarker && inputIsArmored && resultIsArmored;
    }

    /**
     * A legacy marker-only plain elytra may be sanitized only as the source of a real CREATE transaction. The caller
     * must additionally prove that the item is an exact legacy candidate and not a current protected placeholder.
     */
    static boolean canSanitizeLegacyInput(InputAction inputAction, boolean exactLegacyCandidate)
    {
        return inputAction == InputAction.CREATE && exactLegacyCandidate;
    }
}
