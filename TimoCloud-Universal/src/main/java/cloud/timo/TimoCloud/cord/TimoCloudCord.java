package cloud.timo.TimoCloud.cord;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import cloud.timo.TimoCloud.api.implementations.EventManager;
import cloud.timo.TimoCloud.cord.api.TimoCloudUniversalAPICordImplementation;
import cloud.timo.TimoCloud.cord.managers.CordFileManager;
import cloud.timo.TimoCloud.cord.managers.ProxyManager;
import cloud.timo.TimoCloud.cord.sockets.*;
import cloud.timo.TimoCloud.lib.modules.ModuleType;
import cloud.timo.TimoCloud.lib.modules.TimoCloudModule;
import cloud.timo.TimoCloud.lib.utils.options.OptionSet;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimoCloudCord implements TimoCloudModule {

    public static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static TimoCloudCord instance;
    private OptionSet options;
    private String prefix = ANSI_YELLOW + "[" +ANSI_CYAN + "Timo" + ANSI_RESET + "Cloud" + ANSI_YELLOW + "]" + ANSI_RESET;
    private CordFileManager fileManager;
    private ProxyManager proxyManager;
    private CordSocketClient socketClient;
    private CordSocketClientHandler socketClientHandler;
    private CordSocketMessageManager socketMessageManager;
    private CordSocketServer socketServer;
    private MinecraftDecoder minecraftDecoder;
    private CordStringHandler stringHandler;
    private ScheduledExecutorService scheduler;
    private Channel channel;
    private boolean connected = false;
    private EventLoopGroup workerGroup;

    public static String getTime() {
        return "[" + format.format(new Date()) + "] ";
    }

    private String formatLog(String message, String color) {
        return (getTime() + getPrefix() + color + message + ANSI_RESET);
    }

    public void info(String message) {
        System.out.println(formatLog(message, ANSI_RESET));
    }

    public void severe(String message) {
        System.err.println(formatLog(message, ANSI_RED));
    }

    @Override
    public void load(OptionSet optionSet) {
        this.options = optionSet;
        makeInstances();
        new Thread(this::initSocketServer).start();
        info(ANSI_GREEN + "TimoCloudCord has been loaded");
        scheduleConnecting();
    }

    @Override
    public void unload() {

    }

    private void makeInstances() {
        instance = this;
        fileManager = new CordFileManager();
        proxyManager = new ProxyManager();
        socketClient = new CordSocketClient();
        socketClientHandler = new CordSocketClientHandler();
        socketMessageManager = new CordSocketMessageManager();
        socketServer = new CordSocketServer();
        minecraftDecoder = new MinecraftDecoder();
        stringHandler = new CordStringHandler();
        scheduler = Executors.newScheduledThreadPool(1);
        workerGroup = new NioEventLoopGroup();

        TimoCloudAPI.setUniversalImplementation(new TimoCloudUniversalAPICordImplementation());
        TimoCloudAPI.setEventImplementation(new EventManager());
    }

    private void scheduleConnecting() {
        scheduler.scheduleAtFixedRate(this::everySecond, 0, 1, TimeUnit.SECONDS);
        alertConnecting();
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void alertConnecting() {
        info("Connecting to Core...");
    }

    private void everySecond() {
        try {
            connectToSocket();
            getSocketMessageManager().sendMessage("GET_API_DATA", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectToSocket() {
        if (isConnected()) return;
        new Thread(() -> {
            try {
                getSocketClient().init(getCoreSocketIP(), getCoreSocketPort());
            } catch (Exception e) {
            }
        }).start();
    }



    public void onSocketConnect() {
        setConnected(true);
        getSocketMessageManager().sendMessage("CORD_HANDSHAKE", null);
        info("Successfully connected to Core socket!");
    }

    public void onSocketDisconnect() {
        if (isConnected()) info("Disconnected from Core. Reconnecting...");
        setConnected(false);
    }

    public void onHandshakeSuccess() {

    }

    private void initSocketServer() {
        try {
            socketServer.init("0.0.0.0", getProxyPort());
        } catch (Exception e) {
            severe("Error while initializing socket server:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int getProxyPort() {
        Object port = getFileManager().getConfig().get("proxy-port");
        if (port != null) {
            try {
                return (Integer) port;
            } catch (Exception e) {}
        }
        info("No proxy port specified, using any free port.");
        return getFreePort();
    }

    private Integer getFreePort() {
        for (int p = 40000; p<=50000; p++) {
            if (portIsFree(p)) return p;
        }
        return null;
    }


    private boolean portIsFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFileName() {
        return new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getName();
    }

    public String getName() {
        return (String) getFileManager().getConfig().get("name");
    }

    public String getCoreSocketIP() {
        return (String) getFileManager().getConfig().get("core-ip");
    }

    public Integer getCoreSocketPort() {
        return (Integer) getFileManager().getConfig().get("core-port");
    }

    public static TimoCloudCord getInstance() {
        return instance;
    }

    public String getPrefix() {
        return prefix + " ";
    }

    public CordFileManager getFileManager() {
        return fileManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public CordSocketClient getSocketClient() {
        return socketClient;
    }

    public CordSocketClientHandler getSocketClientHandler() {
        return socketClientHandler;
    }

    public CordSocketMessageManager getSocketMessageManager() {
        return socketMessageManager;
    }

    public CordSocketServer getSocketServer() {
        return socketServer;
    }

    public MinecraftDecoder getMinecraftDecoder() {
        return minecraftDecoder;
    }

    public CordStringHandler getStringHandler() {
        return stringHandler;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    @Override
    public ModuleType getModuleType() {
        return ModuleType.CORD;
    }
}
