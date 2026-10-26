package nl.pim16aap2.armoredElytra.handlers;

import nl.pim16aap2.armoredElytra.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DurabilitySafetyTest
{
    @Test
    void damageRunsAfterProtectionPluginsAndRejectsCancelledHits() throws Exception
    {
        final EventHandler annotation = EventHandlers.class
            .getMethod("onPlayerDamage", EntityDamageEvent.class)
            .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.MONITOR, annotation.priority());
        assertTrue(annotation.ignoreCancelled());

        final EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.isCancelled()).thenReturn(true);
        final EventHandlers handler = mock(EventHandlers.class, CALLS_REAL_METHODS);

        handler.onPlayerDamage(event);

        verify(event, never()).getEntity();
    }

    @Test
    void flightDurabilityRunsAfterArenaPluginsAndSkipsHitEvents() throws Exception
    {
        final EventHandler annotation = FlyDurabilityHandler.class
            .getMethod("onItemDamage", org.bukkit.event.player.PlayerItemDamageEvent.class)
            .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.HIGHEST, annotation.priority());
        assertTrue(annotation.ignoreCancelled());

        final ArmoredElytraDamageEvent event = mock(ArmoredElytraDamageEvent.class);
        new FlyDurabilityHandler(false, null, null).onItemDamage(event);

        verify(event, never()).getItem();
        verify(event, never()).setCancelled(true);
    }

    @Test
    void cancelledElytraDamageEventSkipsDurabilityLoss()
    {
        try (MockedStatic<Bukkit> bukkit = mockBukkit(event -> event.setCancelled(true)))
        {
            assertEquals(-1, EventHandlers.durabilityLossAfterEvent(mock(Player.class), mock(ItemStack.class), 3));
        }
    }

    @Test
    void changedElytraDamageEventSetsTheDurabilityLoss()
    {
        try (MockedStatic<Bukkit> bukkit = mockBukkit(event -> event.setDamage(7)))
        {
            assertEquals(7, EventHandlers.durabilityLossAfterEvent(mock(Player.class), mock(ItemStack.class), 3));
        }
    }

    @Test
    void noDurabilityLossFiresNoEvent()
    {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class))
        {
            assertEquals(0, EventHandlers.durabilityLossAfterEvent(mock(Player.class), mock(ItemStack.class), 0));
            bukkit.verifyNoInteractions();
        }
    }

    private static MockedStatic<Bukkit> mockBukkit(Consumer<ArmoredElytraDamageEvent> listener)
    {
        final PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation ->
        {
            listener.accept(invocation.getArgument(0));
            return null;
        }).when(pluginManager).callEvent(any(ArmoredElytraDamageEvent.class));
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        return bukkit;
    }

    @Test
    void brokenElytraMovesToAnEmptySlotWithoutMutatingTheEquippedStack()
    {
        final TransferFixture fixture = new TransferFixture(false, true);

        Util.moveChestplateToInventory(fixture.player);

        verify(fixture.inventory).setItem(7, fixture.copy);
        verify(fixture.inventory).setChestplate(null);
        verify(fixture.source, never()).setAmount(anyInt());
    }

    @Test
    void fullInventoryDropsOneOwnerOnlyCopyAndClearsTheSource()
    {
        final TransferFixture fixture = new TransferFixture(true, true);

        Util.moveChestplateToInventory(fixture.player);

        verify(fixture.dropped).setOwner(fixture.playerId);
        verify(fixture.world, times(1)).dropItem(eq(fixture.location), eq(fixture.copy), any());
        verify(fixture.inventory).setChestplate(null);
        verify(fixture.source, never()).setAmount(anyInt());
    }

    @Test
    void rejectedDropKeepsTheEquippedSource()
    {
        final TransferFixture fixture = new TransferFixture(true, false);

        Util.moveChestplateToInventory(fixture.player);

        verify(fixture.inventory, never()).setChestplate(null);
    }

    @Test
    void reentrantDropEventCannotCreateASecondCopy()
    {
        final TransferFixture fixture = new TransferFixture(true, true);
        when(fixture.world.dropItem(eq(fixture.location), eq(fixture.copy), any())).thenAnswer(invocation ->
        {
            invocation.<java.util.function.Consumer<Item>>getArgument(2).accept(fixture.dropped);
            Util.moveChestplateToInventory(fixture.player);
            return fixture.dropped;
        });

        Util.moveChestplateToInventory(fixture.player);

        verify(fixture.world, times(1)).dropItem(eq(fixture.location), eq(fixture.copy), any());
        verify(fixture.inventory).setChestplate(null);
    }

    @Test
    void changedSourceRemovesSpawnedCopyAndKeepsReplacementEquipped()
    {
        final TransferFixture fixture = new TransferFixture(true, true);
        final ItemStack replacement = mock(ItemStack.class);
        when(replacement.getAmount()).thenReturn(1);
        when(fixture.world.dropItem(eq(fixture.location), eq(fixture.copy), any())).thenAnswer(invocation ->
        {
            invocation.<java.util.function.Consumer<Item>>getArgument(2).accept(fixture.dropped);
            when(fixture.inventory.getChestplate()).thenReturn(replacement);
            return fixture.dropped;
        });

        Util.moveChestplateToInventory(fixture.player);

        verify(fixture.dropped).remove();
        verify(fixture.inventory, never()).setChestplate(null);
    }

    private static final class TransferFixture
    {
        private final Player player = mock(Player.class);
        private final PlayerInventory inventory = mock(PlayerInventory.class);
        private final ItemStack source = mock(ItemStack.class);
        private final ItemStack copy = mock(ItemStack.class);
        private final World world = mock(World.class);
        private final Item dropped = mock(Item.class);
        private final Location location = mock(Location.class);
        private final UUID playerId = UUID.randomUUID();

        private TransferFixture(boolean full, boolean dropValid)
        {
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenReturn(location);
            when(inventory.getChestplate()).thenReturn(source);
            when(inventory.firstEmpty()).thenReturn(full ? -1 : 7);
            when(source.getAmount()).thenReturn(1);
            when(source.clone()).thenReturn(copy);
            when(source.isSimilar(copy)).thenReturn(true);
            when(copy.getAmount()).thenReturn(1);
            when(dropped.isValid()).thenReturn(dropValid);
            when(world.dropItem(eq(location), eq(copy), any())).thenAnswer(invocation ->
            {
                invocation.<java.util.function.Consumer<Item>>getArgument(2).accept(dropped);
                return dropped;
            });
        }
    }
}
