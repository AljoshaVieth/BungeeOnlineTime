package de.sirywell.localexecute;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LocalExecutePlugin extends JavaPlugin {
    public static final String CHANNEL = "bungeeonlinetime:command";

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, new CommandRewardExecutor(getLogger()));
    }
}
