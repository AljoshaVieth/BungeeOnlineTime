package lu.r3flexi0n.bungeeonlinetime.rewards;

public abstract class CommandRewardAction implements RewardAction {
    protected final String command;

    public CommandRewardAction(String command) {
        this.command = command;
    }
}
