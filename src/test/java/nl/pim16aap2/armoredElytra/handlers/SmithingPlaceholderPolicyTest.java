package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.util.itemInput.InputAction;
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

    @Test
    void sanitizesOnlyLegacyPlainElytraCreateInputs()
    {
        Assertions.assertTrue(SmithingPlaceholderPolicy.canSanitizeLegacyInput(InputAction.CREATE, true));

        Assertions.assertFalse(SmithingPlaceholderPolicy.canSanitizeLegacyInput(InputAction.CREATE, false));
        Assertions.assertFalse(SmithingPlaceholderPolicy.canSanitizeLegacyInput(InputAction.UPGRADE, true));
        Assertions.assertFalse(SmithingPlaceholderPolicy.canSanitizeLegacyInput(InputAction.BLOCK, true));
    }
}
