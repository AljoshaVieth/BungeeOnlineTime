package lu.r3flexi0n.bungeeonlinetime;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import static lu.r3flexi0n.bungeeonlinetime.BungeeOnlineTime.CONFIG_PROVIDER;
import static lu.r3flexi0n.bungeeonlinetime.BungeeOnlineTime.PLAYER_FILE;
import static lu.r3flexi0n.bungeeonlinetime.BungeeOnlineTime.rewards;


/**
 * Created by Aljosha on 02.06.2019
 */
public class RewardManager {
    private static final Pattern PLAYER_REPLACE_PATTERN = Pattern.compile("(\\[player]|\\[user])");

    public static void checkRewards(){
        for (ProxiedPlayer player: BungeeOnlineTime.INSTANCE.getProxy().getPlayers()) {
            checkReward(player);
        }

    }

    public static void checkReward(ProxiedPlayer player){
        OnlineTime onlineTime = null;
        try {
            onlineTime = BungeeOnlineTime.SQL.getOnlineTime(player.getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        long seconds = 0;
        if (onlineTime != null) {
            seconds = onlineTime.getTime() / 1000;
        }
        int hours = (int) (seconds / 3600);
        Configuration players;
        try {
            players = CONFIG_PROVIDER.load(PLAYER_FILE);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if(players.get((player.getUniqueId().toString())) == null){
            players.set(player.getUniqueId().toString(), 0);
            try {
                CONFIG_PROVIDER.save(players, PLAYER_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // compare player time with times in Rewards Config
        int currentReward = players.getInt((player.getUniqueId().toString()));
        for (Map.Entry<Integer, String> pair : rewards.entrySet()) {
            int currentKey = pair.getKey();
            if (currentKey > currentReward && currentKey < hours) {
                // Execute reward command
                String command = PLAYER_REPLACE_PATTERN.matcher(pair.getValue())
                        .replaceAll(pair.getValue());

                sendToSpigot("command", command, player.getServer().getInfo());
                players.set(player.getUniqueId().toString(), currentKey);
                try {
                    CONFIG_PROVIDER.save(players, PLAYER_FILE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }

    private static void sendToSpigot(String channel, String message, ServerInfo server) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF(channel);
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Note the "Return". It is the channel name that we registered in our Main class of Bungee plugin.
        server.sendData("Return", stream.toByteArray());
    }
}
