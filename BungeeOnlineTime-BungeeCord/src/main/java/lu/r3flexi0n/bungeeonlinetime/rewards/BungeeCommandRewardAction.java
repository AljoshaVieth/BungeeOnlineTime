package lu.r3flexi0n.bungeeonlinetime.rewards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.regex.Pattern;

public class BungeeCommandRewardAction extends CommandRewardAction {
    private static final Pattern PLAYER_REPLACE_PATTERN = Pattern.compile("(\\[player]|\\[user])");

    public BungeeCommandRewardAction(String command) {
        super(command);
    }

    public static BungeeCommandRewardAction deserialize(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        return new BungeeCommandRewardAction(object.getAsJsonPrimitive("command").getAsString());
    }

    @Override
    public void perform(ProxiedPlayer target) {
        executeBungeeCommand(PLAYER_REPLACE_PATTERN.matcher(command).replaceAll(target.getName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BungeeCommandRewardAction that = (BungeeCommandRewardAction) o;

        return command.equals(that.command);
    }

    @Override
    public int hashCode() {
        return command.hashCode();
    }

    private void executeBungeeCommand(String command) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
    }
}
