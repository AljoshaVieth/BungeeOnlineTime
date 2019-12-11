package lu.r3flexi0n.bungeeonlinetime.rewards;

import lu.r3flexi0n.bungeeonlinetime.BungeeOnlineTime;
import lu.r3flexi0n.bungeeonlinetime.database.RewardResult;
import lu.r3flexi0n.bungeeonlinetime.database.TimeResult;
import lu.r3flexi0n.bungeeonlinetime.repository.PlayerRepository;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * Created by Aljosha on 02.06.2019
 */
public class RewardManager implements Runnable {
    private final List<Reward> rewards;
    private final PlayerRepository repository;

    public RewardManager(List<Reward> rewards, PlayerRepository repository) {
        this.repository = repository;
        this.rewards = new ArrayList<>(rewards);
        Collections.sort(this.rewards);
    }

    public Reward fromId(int id) {
        for (Reward reward : rewards) {
            if (reward.getId() == id) {
                return reward;
            }
        }
        return null;
    }

    @Override
    public void run() {
        for (ProxiedPlayer player : BungeeOnlineTime.INSTANCE.getProxy().getPlayers()) {
            checkReward(player);
        }
    }

    public void checkReward(ProxiedPlayer player) {
        Optional<TimeResult> timeResult = repository.getOnlineTime(player.getUniqueId());
        long seconds = 0;
        if (timeResult.isPresent()) {
            seconds += timeResult.get().getSeconds();
        }

        Optional<RewardResult> result = repository.getRewards(player.getUniqueId());
        if (!result.isPresent()) {
            return;
        }
        long total = seconds + BungeeOnlineTime.ONLINE_PLAYERS.get(player.getUniqueId()).getNoAFKTime();
        long totalHours = total / 3600;
        Set<Reward> playerRewards = result.get().getRewards(this::fromId);
        for (Reward reward : rewards) {
            if (reward.getRequiredHours() <= totalHours && !playerRewards.contains(reward)) {
                reward.getAction().perform(player);
                repository.addReward(player.getUniqueId(), reward.getId());
            }
        }
    }
}
