package cloud.timo.TimoCloud.core.api;

import cloud.timo.TimoCloud.api.implementations.ProxyObjectBasicImplementation;
import cloud.timo.TimoCloud.api.objects.PlayerObject;
import cloud.timo.TimoCloud.api.objects.ProxyObject;
import cloud.timo.TimoCloud.core.TimoCloudCore;
import cloud.timo.TimoCloud.core.objects.Proxy;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.util.List;

@NoArgsConstructor
public class ProxyObjectCoreImplementation extends ProxyObjectBasicImplementation implements ProxyObject {

    public ProxyObjectCoreImplementation(String name, String group, String token, List<PlayerObject> onlinePlayers, int onlinePlayerCount, String base, InetSocketAddress inetSocketAddress) {
        super(name, group, token, onlinePlayers, onlinePlayerCount, base, inetSocketAddress);
    }

    private Proxy getProxy() {
        return TimoCloudCore.getInstance().getServerManager().getProxyByToken(getToken());
    }

    @Override
    public void executeCommand(String command) {
        getProxy().executeCommand(command);
    }

    @Override
    public void stop() {
        getProxy().stop();
    }
}
