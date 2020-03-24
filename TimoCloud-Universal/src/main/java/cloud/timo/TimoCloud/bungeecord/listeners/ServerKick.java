package cloud.timo.TimoCloud.bungeecord.listeners;

import cloud.timo.TimoCloud.bungeecord.TimoCloudBungee;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;

public class ServerKick implements Listener {
    @EventHandler (priority = 65)
    public void onServerKickEvent(ServerKickEvent event) {
        if (! TimoCloudBungee.getInstance().getFileManager().getConfig().getBoolean("useFallback")) return;
        ServerInfo server = TimoCloudBungee.getInstance().getLobbyManager().getFreeLobby(event.getPlayer().getUniqueId(), true);
        if (server == null) {
            TimoCloudBungee.getInstance().info("No fallback server found");
            return;
        }
        TimoCloudBungee.getInstance().info("Connecting to fallback server: " + server.getName() + " (" + event.getPlayer().getName() + ") - Kicked from " + event.getKickedFrom().getName() + ": " + TextComponent.toLegacyText(event.getKickReasonComponent()));
        event.setCancelled(true);
        if (server.equals(event.getKickedFrom())) {
            event.getPlayer().disconnect(event.getKickReasonComponent());
        } else {
            event.setCancelServer(server);
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        }
    }
}
