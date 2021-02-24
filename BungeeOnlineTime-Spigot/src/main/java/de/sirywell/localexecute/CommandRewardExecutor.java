package de.sirywell.localexecute;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Logger;

public class CommandRewardExecutor implements PluginMessageListener {
    private final Logger logger;

    public CommandRewardExecutor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals(LocalExecutePlugin.CHANNEL)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String command = in.readUTF();
        logger.info("Execute command remotely: " + command);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
