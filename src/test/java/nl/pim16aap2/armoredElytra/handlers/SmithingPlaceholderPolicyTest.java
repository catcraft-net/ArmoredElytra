package nl.pim16aap2.armoredElytra.handlers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SmithingPlaceholderPolicyTest
{
    @Test
    void onlyAutoRepairsMarkerInheritedFromKnownArmoredInput()
    {
        Assertions.assertTrue(SmithingPlaceholderPolicy.canAutoRepair(true, true, true));

        Assertions.assertFalse(SmithingPlaceholderPolicy.canAutoRepair(true, false, true));
        Assertions.assertFalse(SmithingPlaceholderPolicy.canAutoRepair(false, true, true));
        Assertions.assertFalse(SmithingPlaceholderPolicy.canAutoRepair(true, true, false));
    }
}
