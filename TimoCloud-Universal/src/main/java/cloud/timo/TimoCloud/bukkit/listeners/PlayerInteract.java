package cloud.timo.TimoCloud.bukkit.listeners;

import cloud.timo.TimoCloud.bukkit.TimoCloudBukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;

public class PlayerInteract implements Listener {

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (! event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block block = event.getClickedBlock();

        if (TimoCloudBukkit.getInstance().isVersion113OrAbove()){
            if (! Arrays.asList(Material.LEGACY_WALL_SIGN, Material.LEGACY_SIGN_POST).contains(block.getType())) return;
            TimoCloudBukkit.getInstance().getSignManager().onSignClick(event.getPlayer(), block.getLocation());
        } else if (!TimoCloudBukkit.getInstance().isVersion113OrAbove()){
            if (! Arrays.asList(Material.LEGACY_WALL_SIGN, Material.valueOf("SIGN_POST")).contains(block.getType())) return;
            TimoCloudBukkit.getInstance().getSignManager().onSignClick(event.getPlayer(), block.getLocation());
        }
    }
}
