package cloud.timo.TimoCloud.core.objects;

import cloud.timo.TimoCloud.api.events.ProxyRegisterEvent;
import cloud.timo.TimoCloud.api.events.ProxyUnregisterEvent;
import cloud.timo.TimoCloud.api.objects.PlayerObject;
import cloud.timo.TimoCloud.api.objects.ProxyObject;
import cloud.timo.TimoCloud.core.TimoCloudCore;
import cloud.timo.TimoCloud.core.api.ProxyObjectCoreImplementation;
import cloud.timo.TimoCloud.core.cloudflare.DnsRecord;
import cloud.timo.TimoCloud.core.sockets.Communicatable;
import cloud.timo.TimoCloud.lib.objects.JSONBuilder;
import cloud.timo.TimoCloud.lib.utils.HashUtil;
import io.netty.channel.Channel;
import org.json.simple.JSONObject;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Proxy implements Communicatable {

    private String name;
    private ProxyGroup group;
    private int port;
    private InetSocketAddress address;
    private Base base;
    private String token;
    private int onlinePlayerCount;
    private List<PlayerObject> onlinePlayers;
    private Channel channel;
    private boolean starting;
    private boolean registered;
    private DnsRecord dnsRecord;
    private List<Server> registeredServers;

    public Proxy(String name, ProxyGroup group, Base base, String token) {
        this.name = name;
        this.group = group;
        this.base = base;
        this.token = token;
        this.address = new InetSocketAddress(base.getAddress(), 0);
        this.onlinePlayers = new ArrayList<>();
        this.registeredServers = new ArrayList<>();
    }

    public void register() {
        if (isRegistered()) return;
        getGroup().onProxyConnect(this);
        this.starting = false;
        this.registered = true;
        for (Server server : getGroup().getRegisteredServers()) registerServer(server);
        TimoCloudCore.getInstance().getEventManager().fireEvent(new ProxyRegisterEvent(toProxyObject()));
    }

    public void unregister() {
        this.registered = false;
        TimoCloudCore.getInstance().getEventManager().fireEvent(new ProxyUnregisterEvent(toProxyObject()));
        getGroup().removeProxy(this);
        getBase().getProxies().remove(this);

        TimoCloudCore.getInstance().getSocketServerHandler().sendMessage(getBase().getChannel(), getName(), "PROXY_STOPPED", getToken());
    }

    public void start() {
        try {
            starting = true;
            JSONBuilder json = JSONBuilder.create()
                    .setType("START_PROXY")
                    .set("name", getName())
                    .set("group", getGroup().getName())
                    .set("ram", getGroup().getRam())
                    .set("static", getGroup().isStatic())
                    .set("token", getToken())
                    .set("motd", getGroup().getMotd())
                    .set("maxplayers", getGroup().getMaxPlayerCount())
                    .set("maxplayersperproxy", getGroup().getMaxPlayerCountPerProxy())
                    .set("globalHash", HashUtil.getHashes(TimoCloudCore.getInstance().getFileManager().getProxyGlobalDirectory()));
            if (!getGroup().isStatic()) {
                File templateDirectory = new File(TimoCloudCore.getInstance().getFileManager().getProxyTemplatesDirectory(), getGroup().getName());
                try {
                    templateDirectory.mkdirs();
                    json.set("templateHash", HashUtil.getHashes(templateDirectory));
                } catch (Exception e) {
                    TimoCloudCore.getInstance().severe("Error while hashing files while starting proxy " + getName() + ": ");
                    e.printStackTrace();
                    return;
                }
            }
            getBase().sendMessage(json.toJson());
            getBase().setReady(false);
            getBase().setAvailableRam(getBase().getAvailableRam() - getGroup().getRam());
            TimoCloudCore.getInstance().info("Told base " + getBase().getName() + " to start proxy " + getName() + ".");
        } catch (Exception e) {
            TimoCloudCore.getInstance().severe("Error while starting proxy " + getName() + ": TimoCloudBase " + getBase().getName() + " not connected.");
            return;
        }
        getBase().getProxies().add(this);
        getGroup().addStartingProxy(this);
    }

    public void stop() {
        if (channel == null) unregister();
        else channel.close();
    }

    public void registerServer(Server server) {
        sendMessage(JSONBuilder.create()
                .setType("ADD_SERVER")
                .set("name", server.getName())
                .set("address", server.getAddress().getAddress().getHostAddress())
                .set("port", server.getPort())
                .toJson());
        if (!registeredServers.contains(server)) registeredServers.add(server);
    }

    public void unregisterServer(Server server) {
        sendMessage(TimoCloudCore.getInstance().getSocketMessageManager().getMessage("REMOVE_SERVER", server.getName()));
        if (registeredServers.contains(server)) registeredServers.remove(server);
    }

    public void onPlayerConnect(PlayerObject playerObject) {
        if (!getOnlinePlayers().contains(playerObject)) getOnlinePlayers().add(playerObject);
    }

    public void onPlayerDisconnect(PlayerObject playerObject) {
        if (getOnlinePlayers().contains(playerObject)) getOnlinePlayers().remove(playerObject);
    }

    @Override
    public void onMessage(JSONObject message) {
        String type = (String) message.get("type");
        Object data = message.get("data");
        switch (type) {
            case "STOP_PROXY":
                stop();
                break;
            case "PROXY_STARTED":
                setPort(((Number) message.get("port")).intValue());
                break;
            case "PROXY_NOT_STARTED":
                //unregister();
                break;
            case "EXECUTE_COMMAND":
                executeCommand((String) data);
                break;
            case "SET_PLAYER_COUNT":
                this.onlinePlayerCount = ((Number) data).intValue();
                break;
            default:
                sendMessage(message);
        }
    }

    @Override
    public void sendMessage(JSONObject message) {
        if (getChannel() != null) getChannel().writeAndFlush(message.toJSONString());
    }

    @Override
    public void onConnect(Channel channel) {
        setChannel(channel);
        register();
        TimoCloudCore.getInstance().info("Proxy " + getName() + " connected.");
    }

    @Override
    public void onDisconnect() {
        setChannel(null);
        TimoCloudCore.getInstance().info("Proxy " + getName() + " disconnected.");
        unregister();
    }

    @Override
    public void onHandshakeSuccess() {
        TimoCloudCore.getInstance().getSocketServerHandler().sendMessage(getChannel(), "HANDSHAKE_SUCCESS", null);
    }

    public void executeCommand(String command) {
        sendMessage(TimoCloudCore.getInstance().getSocketMessageManager().getMessage("EXECUTE_COMMAND", command));
    }

    public String getName() {
        return name;
    }

    public ProxyGroup getGroup() {
        return group;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        this.address = new InetSocketAddress(getAddress().getAddress(), port);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Base getBase() {
        return base;
    }

    public String getToken() {
        return token;
    }

    public int getOnlinePlayerCount() {
        return onlinePlayerCount;
    }

    public List<PlayerObject> getOnlinePlayers() {
        return onlinePlayers;
    }

    @Override
    public Channel getChannel() {
        return this.channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isStarting() {
        return starting;
    }

    public boolean isRegistered() {
        return registered;
    }

    public DnsRecord getDnsRecord() {
        return dnsRecord;
    }

    public void setDnsRecord(DnsRecord dnsRecord) {
        this.dnsRecord = dnsRecord;
    }

    public List<Server> getRegisteredServers() {
        return registeredServers;
    }

    public ProxyObject toProxyObject() {
        return new ProxyObjectCoreImplementation(
                getName(),
                getGroup().getName(),
                getToken(),
                getOnlinePlayers(),
                getOnlinePlayerCount(),
                getBase().getName(),
                getAddress()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        if (onlinePlayerCount != proxy.onlinePlayerCount) return false;
        if (name != null ? !name.equals(proxy.name) : proxy.name != null) return false;
        if (group != null ? !group.equals(proxy.group) : proxy.group != null) return false;
        if (base != null ? !base.equals(proxy.base) : proxy.base != null) return false;
        if (token != null ? !token.equals(proxy.token) : proxy.token != null) return false;
        return channel != null ? channel.equals(proxy.channel) : proxy.channel == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (base != null ? base.hashCode() : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + onlinePlayerCount;
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getName();
    }
}
