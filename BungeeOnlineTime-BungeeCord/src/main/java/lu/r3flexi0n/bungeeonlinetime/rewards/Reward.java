package lu.r3flexi0n.bungeeonlinetime.rewards;

import org.jetbrains.annotations.NotNull;

public class Reward implements Comparable<Reward> {
    private final int id;
    private final long requiredHours;
    private final RewardAction action;

    public Reward(int id, long requiredHours, RewardAction action) {
        this.id = id;
        this.requiredHours = requiredHours;
        this.action = action;
    }

    public int getId() {
        return id;
    }

    public long getRequiredHours() {
        return requiredHours;
    }

    public RewardAction getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reward reward = (Reward) o;

        if (id != reward.id) return false;
        if (requiredHours != reward.requiredHours) return false;
        return action.equals(reward.action);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (requiredHours ^ (requiredHours >>> 32));
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull Reward o) {
        return Integer.compare(id, o.id);
    }
}
