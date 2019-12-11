package lu.r3flexi0n.bungeeonlinetime.rewards.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lu.r3flexi0n.bungeeonlinetime.rewards.RewardAction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class RewardActionDeserializer implements JsonDeserializer<RewardAction> {

    @Override
    public RewardAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String className = json.getAsJsonObject().getAsJsonPrimitive("class").getAsString();
        try {
            Class<? extends RewardAction> clazz = (Class<? extends RewardAction>) Class.forName(className);
            Method deserialize = clazz.getMethod("deserialize", JsonElement.class);
            return (RewardAction) deserialize.invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException
                | IllegalAccessException | InvocationTargetException e) {
            throw new JsonParseException(e);
        }
    }
}
