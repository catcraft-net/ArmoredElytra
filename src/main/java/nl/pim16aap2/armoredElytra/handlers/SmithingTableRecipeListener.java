package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.ArmoredElytra;
import nl.pim16aap2.armoredElytra.nbtEditor.DurabilityManager;
import nl.pim16aap2.armoredElytra.nbtEditor.NBTEditor;
import nl.pim16aap2.armoredElytra.util.ArmorTier;
import nl.pim16aap2.armoredElytra.util.ConfigLoader;
import nl.pim16aap2.armoredElytra.util.Util;
import nl.pim16aap2.armoredElytra.util.itemInput.ElytraInput;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

import static nl.pim16aap2.armoredElytra.util.SmithingTableUtil.SMITHING_TABLE_RESULT_SLOT;

/**
 * Class for handling smithing table events using recipes.
 * <p>
 * This class should only be used on servers running Minecraft 1.21.1 or newer.
 */
class SmithingTableRecipeListener extends AbstractSmithingTableListener implements Listener
{
    /**
     * The recipe choice for the template item for upgrading to netherite.
     * <p>
     * This is {@code null} on versions without a template slot.
     */
    private static final @Nullable RecipeChoice NETHERITE_UPGRADE_TEMPLATE_CHOICE =
        new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

    /**
     * Placeholder result. Our event handler will handle the actual result, including:
     * <ul>
     *     <li>Permissions</li>
     *     <li>Durability</li>
     *     <li>Enchantments</li>
     *     <li>Whatever else we might add in the future.</li>
     * </ul>
     */
    private static final ItemStack RECIPE_RESULT_PLACEHOLDER = createRecipeResultPlaceholder();

    /**
     * The recipe choice for the elytra.
     */
    private static final RecipeChoice RECIPE_CHOICE_ELYTRA = new RecipeChoice.MaterialChoice(Material.ELYTRA);

    SmithingTableRecipeListener(
        ArmoredElytra plugin,
        NBTEditor nbtEditor,
        DurabilityManager durabilityManager,
        ConfigLoader config)
    {
        super(plugin, nbtEditor, durabilityManager, config);

        registerRecipes();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmithingTableUsage(final PrepareSmithingEvent event)
    {
        final SmithingInventory inventory = event.getInventory();
        final ElytraInput input = ElytraInput.fromInventory(config, inventory);
        if (input.isIgnored())
            return;

        // Affected legacy plain elytras may contain the old st_placeholder marker. The builder clones the input item, so
        // building from it directly would copy that stale marker to the genuine armored result and make us reject it as
        // an internal placeholder. Sanitize only a temporary clone used for this valid CREATE transaction; the actual
        // input item stays untouched and is still consumed exactly once by the normal result-click transaction.
        final ElytraInput buildInput = sanitizeLegacyPlainInput(input);
        event.setResult(armoredElytraBuilder.handleInput(event.getView().getPlayer(), buildInput));
        verifyRecipeResultPlaceholder(inventory, input);
    }

    /**
     * Returns a build-only copy of the input with a stale legacy marker removed when it is safe to do so.
     */
    private ElytraInput sanitizeLegacyPlainInput(ElytraInput input)
    {
        final boolean exactLegacyCandidate =
            SmithingPlaceholderUtil.isLegacyPlainRepairCandidate(input.elytra(), nbtEditor);
        if (!SmithingPlaceholderPolicy.canSanitizeLegacyInput(input.inputAction(), exactLegacyCandidate))
            return input;

        final ItemStack cleanElytra = new ItemStack(input.elytra());
        SmithingPlaceholderUtil.removeMarker(cleanElytra);

        return new ElytraInput(
            cleanElytra,
            input.combinedWith(),
            input.template(),
            input.name(),
            input.inputAction(),
            input.oldArmorTier(),
            input.newArmorTier()
        );
    }

    /**
     * Processes the general {@link InventoryClickEvent} for this plugin.
     * <p>
     * This method will check if the event is fired while a smithing table is open, and if so, will call the appropriate
     * methods to further process the event.
     * <p>
     * See {@link #onSmithingInventoryClick(InventoryClickEvent, Player, SmithingInventory)}.
     *
     * @param event
     *     The {@link InventoryClickEvent} to process.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final Player player = Util.humanEntityToPlayer(event.getWhoClicked());

        if (!(player.getOpenInventory().getTopInventory() instanceof SmithingInventory))
            return;

        if (event.getClickedInventory() instanceof SmithingInventory clickedSmithingInventory)
            onSmithingInventoryClick(event, player, clickedSmithingInventory);
    }

    /**
     * Processes the {@link InventoryClickEvent} when the player clicks on a slot in a smithing table.
     *
     * @param event
     *     The {@link InventoryClickEvent} to process.
     * @param player
     *     The {@link Player} who clicked on a slot in the smithing table.
     * @param smithingInventory
     *     The {@link SmithingInventory} which was clicked.
     */
    protected void onSmithingInventoryClick(
        InventoryClickEvent event,
        Player player,
        SmithingInventory smithingInventory)
    {
        if (event.getSlot() == SMITHING_TABLE_RESULT_SLOT)
            onSmithingInventoryResultClick(event, player, smithingInventory);
    }

    /**
     * Creates a placeholder result for the recipe.
     * <p>
     * It is a regular elytra with protected custom marker data so we can identify it.
     *
     * @return The placeholder result.
     */
    private static ItemStack createRecipeResultPlaceholder()
    {
        final ItemStack result = new ItemStack(Material.ELYTRA);
        SmithingPlaceholderUtil.addMarker(result);
        return result;
    }

    /**
     * Checks if the given item is a placeholder result for a recipe.
     *
     * @param item
     *     The item to check.
     *
     * @return {@code true} if the item is a placeholder result, {@code false} otherwise.
     */
    @Override
    protected boolean isRecipeResultPlaceholder(ItemStack item)
    {
        if (item == null || item.getType() != Material.ELYTRA)
            return false;
        return SmithingPlaceholderUtil.hasMarker(item);
    }

    /**
     * Checks if the recipe result still contains a placeholder marker.
     * <p>
     * Legacy plain inputs are sanitized before building, so they should never reach this method with a marked result.
     * An armored input that somehow retained the old marker can still be repaired here because the result is provably a
     * genuine armored elytra. Any other marked result remains blocked as an internal placeholder.
     *
     * @param inventory
     *     The inventory to check the result in.
     * @param input
     *     The original input for the recipe.
     */
    private void verifyRecipeResultPlaceholder(final SmithingInventory inventory, ElytraInput input)
    {
        final @Nullable ItemStack result = inventory.getItem(SMITHING_TABLE_RESULT_SLOT);
        if (!isRecipeResultPlaceholder(result))
            return;

        final boolean inputHasPlaceholderMarker = SmithingPlaceholderUtil.hasMarker(input.elytra());
        final boolean inputIsArmored = input.oldArmorTier() != ArmorTier.NONE;
        final boolean resultIsArmored = nbtEditor.getArmorTierFromElytra(result) != ArmorTier.NONE;

        if (SmithingPlaceholderPolicy.canAutoRepair(
            inputHasPlaceholderMarker,
            inputIsArmored,
            resultIsArmored))
        {
            SmithingPlaceholderUtil.removeMarker(result);
            inventory.setItem(SMITHING_TABLE_RESULT_SLOT, result);
            return;
        }

        plugin.myLogger(
            Level.SEVERE,
            "Smithing Table: Attempted to retrieve a placeholder result! Result: " + result +
                ", input: " + input
        );
        inventory.setItem(SMITHING_TABLE_RESULT_SLOT, null);
    }

    /**
     * Registers the recipes for the armored elytra.
     * <p>
     * The exact recipes depend on the configuration.
     */
    private void registerRecipes()
    {
        if (config.allowCraftingInSmithingTable())
            ArmorTier.ARMOR_TIERS.forEach(this::registerCraftingRecipe);

        if (config.allowUpgradeToNetherite())
            registerUpgradeToNetheriteRecipe();
    }

    /**
     * Registers the recipe for upgrading diamond elytras to netherite elytras.
     */
    private void registerUpgradeToNetheriteRecipe()
    {
        final NamespacedKey key = new NamespacedKey(plugin, "st_upgrade_to_netherite");

        final RecipeChoice netheriteIngot = new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT);

        registerCraftingRecipe(key, NETHERITE_UPGRADE_TEMPLATE_CHOICE, netheriteIngot);
    }

    /**
     * Registers a crafting recipe for the given tier.
     * <p>
     * This method will handle the differences between versions with and without a template slot.
     *
     * @param tier
     *     The tier to register the recipe for.
     */
    private void registerCraftingRecipe(ArmorTier tier)
    {
        final NamespacedKey key = new NamespacedKey(plugin, "st_recipe_" + tier.name().toLowerCase(Locale.ROOT));

        final RecipeChoice chestPlate = new RecipeChoice.MaterialChoice(
            Objects.requireNonNull(Util.tierToChestPlate(tier)));

        registerCraftingRecipe(key, null, chestPlate);
    }

    /**
     * Registers a smithing recipe with a template item.
     * <p>
     * This method cannot be used on versions without a template slot.
     *
     * @param key
     *     The key for the recipe.
     * @param template
     *     The recipe choice for the template item. May be null to not require a template item.
     * @param chestPlate
     *     The recipe choice for the chest plate.
     */
    private void registerCraftingRecipe(
        NamespacedKey key,
        @Nullable RecipeChoice template,
        RecipeChoice chestPlate)
    {
        Bukkit.addRecipe(new SmithingTransformRecipe(
            key,
            SmithingTableRecipeListener.RECIPE_RESULT_PLACEHOLDER,
            template,
            SmithingTableRecipeListener.RECIPE_CHOICE_ELYTRA,
            chestPlate
        ));
    }
}
