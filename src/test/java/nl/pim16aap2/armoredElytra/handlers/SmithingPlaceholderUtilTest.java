package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.nbtEditor.NBTEditor;
import nl.pim16aap2.armoredElytra.util.ArmorTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

class SmithingPlaceholderUtilTest
{
    private static final NamespacedKey PLACEHOLDER_KEY =
        new NamespacedKey("armoredelytra", "st_placeholder");

    @Test
    void acceptsOnlyExactPlainLegacyCandidate()
    {
        final ItemStack item = Mockito.mock(ItemStack.class);
        final ItemMeta meta = Mockito.mock(ItemMeta.class);
        final PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);
        final NBTEditor nbtEditor = Mockito.mock(NBTEditor.class);

        Mockito.when(item.getType()).thenReturn(Material.ELYTRA);
        Mockito.when(item.getAmount()).thenReturn(1);
        Mockito.when(item.hasItemMeta()).thenReturn(true);
        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(PLACEHOLDER_KEY, PersistentDataType.BYTE)).thenReturn(true);
        Mockito.when(pdc.get(PLACEHOLDER_KEY, PersistentDataType.BYTE)).thenReturn((byte) 1);
        Mockito.when(pdc.getKeys()).thenReturn(Set.of(PLACEHOLDER_KEY));
        Mockito.when(nbtEditor.getArmorTierFromElytra(item)).thenReturn(ArmorTier.NONE);

        Assertions.assertTrue(SmithingPlaceholderUtil.isLegacyPlainRepairCandidate(item, nbtEditor));
    }

    @Test
    void rejectsPartiallyArmoredOrUnexpectedPluginData()
    {
        final ItemStack item = Mockito.mock(ItemStack.class);
        final ItemMeta meta = Mockito.mock(ItemMeta.class);
        final PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);
        final NBTEditor nbtEditor = Mockito.mock(NBTEditor.class);
        final NamespacedKey durabilityKey =
            new NamespacedKey("armoredelytra", "armored_elytra_durability");

        Mockito.when(item.getType()).thenReturn(Material.ELYTRA);
        Mockito.when(item.getAmount()).thenReturn(1);
        Mockito.when(item.hasItemMeta()).thenReturn(true);
        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(PLACEHOLDER_KEY, PersistentDataType.BYTE)).thenReturn(true);
        Mockito.when(pdc.get(PLACEHOLDER_KEY, PersistentDataType.BYTE)).thenReturn((byte) 1);
        Mockito.when(pdc.getKeys()).thenReturn(Set.of(PLACEHOLDER_KEY, durabilityKey));
        Mockito.when(nbtEditor.getArmorTierFromElytra(item)).thenReturn(ArmorTier.NONE);

        Assertions.assertFalse(SmithingPlaceholderUtil.isLegacyPlainRepairCandidate(item, nbtEditor));

        Mockito.when(pdc.getKeys()).thenReturn(Set.of(PLACEHOLDER_KEY));
        Mockito.when(nbtEditor.getArmorTierFromElytra(item)).thenReturn(ArmorTier.NETHERITE);
        Assertions.assertFalse(SmithingPlaceholderUtil.isLegacyPlainRepairCandidate(item, nbtEditor));
    }
}
