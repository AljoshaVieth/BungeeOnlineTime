package lu.r3flexi0n.bungeeonlinetime.database;

import java.util.UUID;

public abstract class Result {
    protected final String name;
    protected final UUID uuid;

    public Result(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uuid;
    }
}
