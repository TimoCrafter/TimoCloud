package cloud.timo.TimoCloud.core.commands;

import cloud.timo.TimoCloud.api.core.commands.CommandHandler;
import cloud.timo.TimoCloud.api.core.commands.CommandSender;
import cloud.timo.TimoCloud.core.TimoCloudCore;
import cloud.timo.TimoCloud.core.objects.Base;
import cloud.timo.TimoCloud.core.objects.Cord;
import cloud.timo.TimoCloud.core.objects.Proxy;
import cloud.timo.TimoCloud.core.objects.Server;

public class StopCommandHandler implements CommandHandler {

	@Override
	public void onCommand(String command, CommandSender sender, String... args) {
		sender.sendMessage("Stopping TimoCloudCore...");
		for (Base base : TimoCloudCore.getInstance().getInstanceManager().getBases()) {
			Server[] servers = base.getServers().toArray(new Server[] {});
			for (Server serverOnBase : servers)
				serverOnBase.stop();
			Proxy[] proxies = base.getProxies().toArray(new Proxy[] {});
			for (Proxy proxyOnBase : proxies)
				proxyOnBase.stop();
			base.stop();
		}
		for(Cord cord : TimoCloudCore.getInstance().getInstanceManager().getCords()) {
			cord.stop();
		}
		System.exit(0);
	}

}
