package lu.r3flexi0n.bungeeonlinetime.database;

import java.util.UUID;

public class TimeResult extends Result {
    private final long seconds;

    public TimeResult(long seconds, String name, UUID uuid) {
        super(name, uuid);
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }

}
