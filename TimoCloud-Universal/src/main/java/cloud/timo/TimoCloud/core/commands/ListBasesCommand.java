package cloud.timo.TimoCloud.core.commands;

import cloud.timo.TimoCloud.api.core.commands.CommandHandler;
import cloud.timo.TimoCloud.api.core.commands.CommandSender;
import cloud.timo.TimoCloud.core.TimoCloudCore;
import cloud.timo.TimoCloud.core.commands.utils.CommandFormatUtil;
import cloud.timo.TimoCloud.core.objects.Base;

import java.util.List;
import java.util.stream.Collectors;

public class ListBasesCommand extends CommandFormatUtil implements CommandHandler {

    @Override
    public void onCommand(String command, CommandSender sender, String... args) {
        List<Base> bases = TimoCloudCore.getInstance().getInstanceManager().getBases().stream().filter(Base::isConnected).collect(Collectors.toList());
        sender.sendMessage("&6Bases (&3" + bases.size() + "&6):");
        for (Base base : bases) {
            displayBase(base, sender);
        }
    }

}
