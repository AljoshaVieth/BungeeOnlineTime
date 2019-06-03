package lu.r3flexi0n.bungeeonlinetime;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static lu.r3flexi0n.bungeeonlinetime.BungeeOnlineTime.*;


/**
 * Created by Aljosha on 02.06.2019
 */
public class RewardManager {


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
        Configuration players = null;
        try {
            players = CONFIG_PROVIDER.load(PLAYER_FILE);
        } catch (IOException e) {
            e.printStackTrace();
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
        if(players != null){
            Integer currentReward = players.getInt((player.getUniqueId().toString()));
            Iterator it = rewards.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                int currentKey = (Integer) pair.getKey();
                if(currentKey > currentReward && currentKey < hours){
                    // Execute reward command
                    String command = pair.getValue().toString();
                    command = command.replaceAll("[user]", player.getName());
                    command = command.replaceAll("[player]", player.getName());

                    sendToSpigot("command", command, player.getServer().getInfo());
                    players.set(player.getUniqueId().toString(), currentKey);
                    try {
                        CONFIG_PROVIDER.save(players, PLAYER_FILE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                it.remove(); // avoids a ConcurrentModificationException
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
