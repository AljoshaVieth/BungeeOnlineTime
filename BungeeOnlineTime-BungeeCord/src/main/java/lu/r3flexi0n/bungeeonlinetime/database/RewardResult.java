package lu.r3flexi0n.bungeeonlinetime.database;

import lu.r3flexi0n.bungeeonlinetime.rewards.Reward;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RewardResult extends Result {
    private final Set<Integer> rewards;

    public RewardResult(String name, UUID uuid, Set<Integer> rewards) {
        super(name, uuid);
        this.rewards = rewards;
    }

    public Set<Reward> getRewards(Function<Integer, Reward> toReward) {
        return rewards.stream().map(toReward).collect(Collectors.toSet());
    }
}
