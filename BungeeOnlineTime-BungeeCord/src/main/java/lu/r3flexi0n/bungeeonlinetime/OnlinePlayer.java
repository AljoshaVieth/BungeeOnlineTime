package lu.r3flexi0n.bungeeonlinetime;

public class OnlinePlayer {

    private final long joinTime;

    private long afkTime;

    private long afkJoin;

    public OnlinePlayer() {
        this.joinTime = currentSeconds();
    }

    public long getNoAFKTime() {
        return (currentSeconds() - joinTime - afkTime - getCurrentAFKTime());
    }

    public void joinAFK() {
        if (afkJoin == 0) {
            afkJoin = currentSeconds();
        }
    }

    public void leaveAFK() {
        if (afkJoin > 0) {
            afkTime += currentSeconds() - afkJoin;
            afkJoin = 0;
        }
    }

    private long getCurrentAFKTime() {
        if (afkJoin > 0) {
            return currentSeconds() - afkJoin;
        }
        return 0;
    }

    private static long currentSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
