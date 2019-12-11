package lu.r3flexi0n.bungeeonlinetime.rewards;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.regex.Pattern;

public class SpigotCommandRewardAction extends CommandRewardAction {
    private static final Pattern PLAYER_REPLACE_PATTERN = Pattern.compile("(\\[player]|\\[user])");
    public SpigotCommandRewardAction(String command) {
        super(command);
    }

    public static SpigotCommandRewardAction deserialize(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        return new SpigotCommandRewardAction(object.getAsJsonPrimitive("command").getAsString());
    }

    @Override
    public void perform(ProxiedPlayer target) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PLAYER_REPLACE_PATTERN.matcher(command).replaceAll(target.getName()));
        target.getServer().sendData("bungeeonlinetime:command", out.toByteArray());
    }
}
