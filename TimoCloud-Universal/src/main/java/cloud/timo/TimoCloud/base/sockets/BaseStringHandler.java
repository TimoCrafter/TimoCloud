package cloud.timo.TimoCloud.base.sockets;

import cloud.timo.TimoCloud.base.TimoCloudBase;
import cloud.timo.TimoCloud.base.objects.BaseProxyObject;
import cloud.timo.TimoCloud.base.objects.BaseServerObject;
import cloud.timo.TimoCloud.lib.messages.Message;
import cloud.timo.TimoCloud.lib.sockets.BasicStringHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.apache.commons.io.FileDeleteStrategy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

@ChannelHandler.Sharable
public class BaseStringHandler extends BasicStringHandler {

    @Override
    public void handleMessage(Message message, String originalMessage, Channel channel) {
        String type = (String) message.get("type");
        Object data = message.get("data");
        switch (type) {
            case "HANDSHAKE_SUCCESS":
                TimoCloudBase.getInstance().onHandshakeSuccess();
                break;
            case "START_SERVER": {
                String serverName = (String) message.get("name");
                String id = (String) message.get("id");
                int ram = ((Number) message.get("ram")).intValue();
                boolean isStatic = (Boolean) message.get("static");
                String group = (String) message.get("group");
                String map = (String) message.get("map");
                Map<String, Object> templateHash = (Map<String, Object>) message.get("templateHash");
                Map<String, Object> mapHash = message.containsKey("mapHash") ? (Map<String, Object>) message.get("mapHash") : null;
                Map<String, Object> globalHash = (Map<String, Object>) message.get("globalHash");
                TimoCloudBase.getInstance().getInstanceManager().addToServerQueue(new BaseServerObject(serverName, id, ram, isStatic, map, group, templateHash, mapHash, globalHash));
                TimoCloudBase.getInstance().info("Added server " + serverName + " to queue.");
                break;
            }
            case "START_PROXY": {
                String proxyName = (String) message.get("name");
                String id = (String) message.get("id");
                int ram = ((Number) message.get("ram")).intValue();
                boolean isStatic = (Boolean) message.get("static");
                String group = (String) message.get("group");
                String motd = (String) message.get("motd");
                int maxPlayers = ((Number) message.get("maxplayers")).intValue();
                int maxPlayersPerProxy = ((Number) message.get("maxplayersperproxy")).intValue();
                Map<String, Object> templateHash = (Map<String, Object>) message.get("templateHash");
                Map<String, Object> globalHash = (Map<String, Object>) message.get("globalHash");
                TimoCloudBase.getInstance().getInstanceManager().addToProxyQueue(new BaseProxyObject(proxyName, id, ram, isStatic, group, motd, maxPlayers, maxPlayersPerProxy, templateHash, globalHash));
                TimoCloudBase.getInstance().info("Added proxy " + proxyName + " to queue.");
                break;
            }
            case "SERVER_STARTED":
                break;
            case "SERVER_STOPPED":
                TimoCloudBase.getInstance().getInstanceManager().onServerStopped((String) data);
                break;
            case "PROXY_STOPPED":
                TimoCloudBase.getInstance().getInstanceManager().onProxyStopped((String) data);
                break;
            case "DELETE_DIRECTORY":
                File dir = new File((String) data);
                if (dir.exists() && dir.isDirectory()) FileDeleteStrategy.FORCE.deleteQuietly(dir);
                break;
            case "TRANSFER":
                try {
                    InputStream inputStream = new ByteArrayInputStream(stringToByteArray((String) message.get("file")));
                    switch ((String) message.get("transferType")) {
                        case "SERVER_TEMPLATE":
                            TimoCloudBase.getInstance().getTemplateManager().extractFiles(inputStream, new File(TimoCloudBase.getInstance().getFileManager().getServerTemplatesDirectory(), (String) message.get("template")));
                            break;
                        case "SERVER_GLOBAL_TEMPLATE":
                            TimoCloudBase.getInstance().getTemplateManager().extractFiles(inputStream, TimoCloudBase.getInstance().getFileManager().getServerGlobalDirectory());
                            break;
                        case "PROXY_TEMPLATE":
                            TimoCloudBase.getInstance().getTemplateManager().extractFiles(inputStream, new File(TimoCloudBase.getInstance().getFileManager().getProxyTemplatesDirectory(), (String) message.get("template")));
                            break;
                        case "PROXY_GLOBAL_TEMPLATE":
                            TimoCloudBase.getInstance().getTemplateManager().extractFiles(inputStream, TimoCloudBase.getInstance().getFileManager().getProxyGlobalDirectory());
                            break;
                    }
                    TimoCloudBase.getInstance().getSocketMessageManager().sendMessage(Message.create().setType("TRANSFER_FINISHED").setTarget(message.getTarget()));
                    TimoCloudBase.getInstance().getInstanceManager().setDownloadingTemplate(false);
                    break;
                } catch (Exception e) {
                    TimoCloudBase.getInstance().severe("Error while unpacking transferred files: ");
                    TimoCloudBase.getInstance().severe(e);
                }
                break;
            case "SHUTDOWN":
            	TimoCloudBase.getInstance().info("Shutting down Base!");
            	System.exit(0);
            	break;
            default:
                TimoCloudBase.getInstance().severe("Could not categorize json message: " + originalMessage);
        }
    }

    private byte[] stringToByteArray(String input) {
        return Base64.getDecoder().decode(input.getBytes());
    }

}
