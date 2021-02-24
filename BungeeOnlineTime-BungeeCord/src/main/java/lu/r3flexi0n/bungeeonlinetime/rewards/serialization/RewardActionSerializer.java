package lu.r3flexi0n.bungeeonlinetime.rewards.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lu.r3flexi0n.bungeeonlinetime.rewards.RewardAction;

import java.lang.reflect.Type;

public class RewardActionSerializer implements JsonSerializer<RewardAction> {
    @Override
    public JsonElement serialize(RewardAction rewardAction, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonElement serialize = jsonSerializationContext.serialize(rewardAction);
        serialize.getAsJsonObject().addProperty("class", rewardAction.getClass().getName());
        return serialize;
    }
}
