package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.nbtEditor.AutoPersistentDataContainer;
import nl.pim16aap2.armoredElytra.nbtEditor.NBTEditor;
import nl.pim16aap2.armoredElytra.util.ArmorTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

/**
 * Utilities for the internal smithing recipe placeholder marker.
 */
final class SmithingPlaceholderUtil
{
    private static final NamespacedKey RECIPE_PLACEHOLDER_KEY =
        new NamespacedKey("armoredelytra", "st_placeholder");
    private static final NamespacedKey RECIPE_PLACEHOLDER_V2_KEY =
        new NamespacedKey("armoredelytra", "st_placeholder_v2");

    private SmithingPlaceholderUtil()
    {
    }

    /**
     * Marks a newly-created internal recipe placeholder. The second marker distinguishes current protected placeholders
     * from legacy player-owned elytras that accidentally retained only the old marker.
     */
    static void addMarker(ItemStack item)
    {
        try (var pdc = new AutoPersistentDataContainer(item))
        {
            pdc.set(RECIPE_PLACEHOLDER_KEY, PersistentDataType.BYTE, (byte) 1);
            pdc.set(RECIPE_PLACEHOLDER_V2_KEY, PersistentDataType.BYTE, (byte) 1);
        }
    }

    static boolean hasMarker(@Nullable ItemStack item)
    {
        return NBTEditor.hasPdcWithKey(item, RECIPE_PLACEHOLDER_KEY, PersistentDataType.BYTE);
    }

    static void removeMarker(ItemStack item)
    {
        try (var pdc = new AutoPersistentDataContainer(item))
        {
            pdc.remove(RECIPE_PLACEHOLDER_KEY);
            pdc.remove(RECIPE_PLACEHOLDER_V2_KEY);
        }
    }

    /**
     * Checks whether an item is an exact legacy plain-elytra recovery candidate.
     * <p>
     * Legacy affected items contain the original {@code st_placeholder=1} marker but no current V2 marker, no armor
     * tier, and no other ArmoredElytra persistent data. Current internal placeholders always receive both markers and
     * therefore cannot qualify for this recovery path.
     */
    static boolean isLegacyPlainRepairCandidate(@Nullable ItemStack item, NBTEditor nbtEditor)
    {
        if (item == null || item.getType() != Material.ELYTRA || item.getAmount() != 1 || !item.hasItemMeta())
            return false;

        final @Nullable ItemMeta meta = item.getItemMeta();
        if (meta == null || nbtEditor.getArmorTierFromElytra(item) != ArmorTier.NONE)
            return false;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final @Nullable Byte marker = pdc.get(RECIPE_PLACEHOLDER_KEY, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1)
            return false;
        if (pdc.has(RECIPE_PLACEHOLDER_V2_KEY, PersistentDataType.BYTE))
            return false;

        final String namespace = RECIPE_PLACEHOLDER_KEY.getNamespace();
        return pdc.getKeys().stream()
                  .filter(key -> namespace.equals(key.getNamespace()))
                  .allMatch(RECIPE_PLACEHOLDER_KEY::equals);
    }
}
