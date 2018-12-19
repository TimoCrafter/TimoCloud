package cloud.timo.TimoCloud.bungeecord;


import cloud.timo.TimoCloud.api.TimoCloudAPI;
import cloud.timo.TimoCloud.api.implementations.EventManager;
import cloud.timo.TimoCloud.api.implementations.TimoCloudUniversalAPIBasicImplementation;
import cloud.timo.TimoCloud.api.utils.APIInstanceUtil;
import cloud.timo.TimoCloud.bungeecord.api.TimoCloudBungeeAPIImplementation;
import cloud.timo.TimoCloud.bungeecord.api.TimoCloudInternalMessageAPIBungeeImplementation;
import cloud.timo.TimoCloud.bungeecord.api.TimoCloudMessageAPIBungeeImplementation;
import cloud.timo.TimoCloud.bungeecord.api.TimoCloudUniversalAPIBungeeImplementation;
import cloud.timo.TimoCloud.bungeecord.commands.FindCommand;
import cloud.timo.TimoCloud.bungeecord.commands.GlistCommand;
import cloud.timo.TimoCloud.bungeecord.commands.LobbyCommand;
import cloud.timo.TimoCloud.bungeecord.commands.TimoCloudCommand;
import cloud.timo.TimoCloud.bungeecord.listeners.*;
import cloud.timo.TimoCloud.bungeecord.managers.BungeeEventManager;
import cloud.timo.TimoCloud.bungeecord.managers.BungeeFileManager;
import cloud.timo.TimoCloud.bungeecord.managers.IpManager;
import cloud.timo.TimoCloud.bungeecord.managers.LobbyManager;
import cloud.timo.TimoCloud.bungeecord.sockets.BungeeSocketClient;
import cloud.timo.TimoCloud.bungeecord.sockets.BungeeSocketClientHandler;
import cloud.timo.TimoCloud.bungeecord.sockets.BungeeSocketMessageManager;
import cloud.timo.TimoCloud.bungeecord.sockets.BungeeStringHandler;
import cloud.timo.TimoCloud.lib.logging.LoggingOutputStream;
import cloud.timo.TimoCloud.lib.messages.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimoCloudBungee extends Plugin {

    private static TimoCloudBungee instance;
    private BungeeFileManager fileManager;
    private LobbyManager lobbyManager;
    private BungeeEventManager eventManager;
    private IpManager ipManager;
    private BungeeSocketClient socketClient;
    private BungeeSocketClientHandler socketClientHandler;
    private BungeeSocketMessageManager socketMessageManager;
    private BungeeStringHandler bungeeStringHandler;
    private TimoCloudCommand timoCloudCommand;
    private String prefix;
    private boolean shuttingDown = false;

    public void info(String message) {
        getLogger().info(ChatColor.translateAlternateColorCodes('&',  message));
    }

    public void warning(String message) {
        getLogger().warning(ChatColor.translateAlternateColorCodes('&', " " + message));
    }

    public void severe(String message) {
        getLogger().severe(ChatColor.translateAlternateColorCodes('&', " &c" + message));
    }

    public void severe(Throwable throwable) {
        throwable.printStackTrace(new PrintStream(new LoggingOutputStream(this::severe)));
    }

    @Override
    public void onEnable() {
        try {
            instance = this;
            info("&eEnabling &bTimoCloudBungee &eversion &7[&6" + getDescription().getVersion() + "&7]&e...");
            makeInstances();
            registerCommands();
            registerListeners();
            registerTasks();
            Executors.newSingleThreadExecutor().submit(this::connectToCore);
            while (!((TimoCloudUniversalAPIBasicImplementation) TimoCloudAPI.getUniversalAPI()).gotAnyData()) {
                try {
                    Thread.sleep(50); // Wait until we get the API data
                } catch (Exception e) {}
            }
            info("&aSuccessfully started TimoCloudBungee!");
        } catch (Exception e) {
            severe("Error while enabling TimoCloudBungee: ");
            TimoCloudBungee.getInstance().severe(e);
        }
    }

    @Override
    public void onDisable() {
        setShuttingDown(true);
        info("&cSuccessfully stopped TimoCloudBungee!");
    }

    private void makeInstances() throws Exception {
        fileManager = new BungeeFileManager();
        lobbyManager = new LobbyManager();
        eventManager = new BungeeEventManager();
        ipManager = new IpManager();
        socketClient = new BungeeSocketClient();
        socketClientHandler = new BungeeSocketClientHandler();
        socketMessageManager = new BungeeSocketMessageManager();
        bungeeStringHandler = new BungeeStringHandler();
        timoCloudCommand = new TimoCloudCommand();

        APIInstanceUtil.setInternalMessageInstance(new TimoCloudInternalMessageAPIBungeeImplementation());
        APIInstanceUtil.setUniversalInstance(new TimoCloudUniversalAPIBungeeImplementation());
        APIInstanceUtil.setBungeeInstance(new TimoCloudBungeeAPIImplementation());
        APIInstanceUtil.setEventInstance(new EventManager());
        APIInstanceUtil.setMessageInstance(new TimoCloudMessageAPIBungeeImplementation());
    }

    private void registerCommands() {
        getProxy().getPluginManager().registerCommand(this, getTimoCloudCommand());
        getProxy().getPluginManager().registerCommand(this, new GlistCommand());
        getProxy().getPluginManager().registerCommand(this, new FindCommand());
        List<String> lobbyCommands = getFileManager().getConfig().getStringList("lobbyCommands");
        if (lobbyCommands.size() > 0) {
            String[] aliases = lobbyCommands.subList(1, lobbyCommands.size()).toArray(new String[0]);
            getProxy().getPluginManager().registerCommand(this, new LobbyCommand(lobbyCommands.get(0), aliases));
        }
    }

    private void connectToCore() {
        info("&6Connecting to TimoCloudCore...");
        try {
            socketClient.init(getTimoCloudCoreIP(), getTimoCloudCoreSocketPort());
        } catch (Exception e) {
            severe("Error while connecting to Core:");
            TimoCloudBungee.getInstance().severe(e);
            onSocketDisconnect();
        }
    }

    private void registerTasks() {
        getProxy().getScheduler().schedule(this, this::everySecond, 1L, 1L, TimeUnit.SECONDS);
    }

    public String getTimoCloudCoreIP() {
        return System.getProperty("timocloud-corehost").split(":")[0];
    }

    public int getTimoCloudCoreSocketPort() {
        return Integer.parseInt(System.getProperty("timocloud-corehost").split(":")[1]);
    }

    public void onSocketConnect() {
        getSocketMessageManager().sendMessage(Message.create().setType("PROXY_HANDSHAKE").setTarget(getProxyId()));
    }

    public void onSocketDisconnect() {
        info("Disconnected from TimoCloudCore. Shutting down....");
        stop();
    }

    public void onHandshakeSuccess() {
        everySecond();
    }

    private void stop() {
        getProxy().stop();
    }

    private void everySecond() {
        sendEverything();
        requestApiData();
    }

    private void requestApiData() {
        getSocketMessageManager().sendMessage(Message.create().setType("GET_API_DATA"));
    }

    private void sendEverything() {
        sendPlayerCount();
    }

    public void sendPlayerCount() {
        getSocketMessageManager().sendMessage(Message.create().setType("SET_PLAYER_COUNT").setData(getProxy().getOnlineCount()));
    }

    private void registerListeners() {
        getProxy().getPluginManager().registerListener(this, new LobbyJoin());
        getProxy().getPluginManager().registerListener(this, new ServerKick());
        getProxy().getPluginManager().registerListener(this, new ProxyPing());
        getProxy().getPluginManager().registerListener(this, new EventMonitor());
        getProxy().getPluginManager().registerListener(this, new IpInjector());
    }

    public String getProxyName() {
        return System.getProperty("timocloud-proxyname");
    }

    public String getProxyId() {
        return System.getProperty("timocloud-proxyid");
    }

    public static TimoCloudBungee getInstance() {
        return instance;
    }

    public BungeeFileManager getFileManager() {
        return fileManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public BungeeEventManager getEventManager() {
        return eventManager;
    }

    public IpManager getIpManager() {
        return ipManager;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public BungeeSocketClient getSocketClient() {
        return socketClient;
    }

    public BungeeSocketClientHandler getSocketClientHandler() {
        return socketClientHandler;
    }

    public BungeeSocketMessageManager getSocketMessageManager() {
        return socketMessageManager;
    }

    public BungeeStringHandler getBungeeStringHandler() {
        return bungeeStringHandler;
    }

    public TimoCloudCommand getTimoCloudCommand() {
        return timoCloudCommand;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public void setShuttingDown(boolean shuttingDown) {
        this.shuttingDown = shuttingDown;
    }


}
