package nl.pim16aap2.armoredElytra.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired before an armored elytra loses durability from a hit, so other plugins (e.g. mob arenas that disable
 * durability) can cancel or change it like a vanilla item damage event.
 * <p>
 * It shares {@link PlayerItemDamageEvent}'s handler list, so every listener of that event receives it.
 */
public class ArmoredElytraDamageEvent extends PlayerItemDamageEvent
{
    public ArmoredElytraDamageEvent(Player player, ItemStack what, int damage)
    {
        super(player, what, damage, damage);
    }
}
