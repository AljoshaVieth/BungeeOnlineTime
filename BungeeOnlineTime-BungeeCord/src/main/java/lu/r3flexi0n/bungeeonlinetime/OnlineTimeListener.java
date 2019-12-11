package lu.r3flexi0n.bungeeonlinetime;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lu.r3flexi0n.bungeeonlinetime.database.TimeResult;
import lu.r3flexi0n.bungeeonlinetime.repository.PlayerRepository;
import lu.r3flexi0n.bungeeonlinetime.utils.Language;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Optional;
import java.util.UUID;

public class OnlineTimeListener implements Listener {
    private final PlayerRepository repository;

    public OnlineTimeListener(PlayerRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!player.hasPermission("onlinetime.save")) {
            return;
        }
        repository.addPlayer(e.getPlayer());
        BungeeOnlineTime.ONLINE_PLAYERS.put(uuid, new OnlinePlayer());
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        ProxiedPlayer player = e.getPlayer();

        OnlinePlayer onlinePlayer = BungeeOnlineTime.ONLINE_PLAYERS.get(player.getUniqueId());
        if (onlinePlayer == null) {
            return;
        }

        ServerInfo server = player.getServer().getInfo();
        if (BungeeOnlineTime.DISABLED_SERVERS.contains(server.getName())) {
            onlinePlayer.joinAFK();
        } else {
            onlinePlayer.leaveAFK();
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        OnlinePlayer onlinePlayer = BungeeOnlineTime.ONLINE_PLAYERS.get(uuid);
        if (onlinePlayer == null) {
            return;
        }
        BungeeOnlineTime.ONLINE_PLAYERS.remove(uuid);

        onlinePlayer.leaveAFK();

        long time = onlinePlayer.getNoAFKTime();
        if (time < 5) {
            return;
        }

        ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
            try {
                repository.updateOnlineTime(uuid, time);
            } catch (Exception ex) {
                player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', Language.ERROR_SAVING)));
                ex.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {

        if (!e.getTag().equals(BungeeOnlineTime.CHANNEL)) {
            return;
        }

        if (!(e.getReceiver() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) e.getReceiver();
        UUID uuid = player.getUniqueId();
        Server server = player.getServer();

        ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
            Optional<TimeResult> time = repository.getOnlineTime(uuid);
            time.ifPresent(timeResult -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeLong(timeResult.getSeconds());
                server.sendData(BungeeOnlineTime.CHANNEL, out.toByteArray());
            });
        });
    }


}
