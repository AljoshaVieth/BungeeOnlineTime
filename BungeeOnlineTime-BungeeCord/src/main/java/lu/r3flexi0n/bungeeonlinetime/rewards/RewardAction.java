package lu.r3flexi0n.bungeeonlinetime.rewards;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface RewardAction {

    void perform(ProxiedPlayer target);
}
