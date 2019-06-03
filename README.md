# BungeeOnlineTimeRewards
This is an extended version of the spigot plugin BungeeOnlineTime from [R3fleXi0n](https://github.com/R3fleXi0n/BungeeOnlineTime). It brings a new reward system, where players can be rewarded for their playing time with commands or ranks.

The installation and configuration hardly differs from the original one. Further details can be found [here](https://www.spigotmc.org/resources/bungeeonlinetime.795/) .

The extended version checks every 5 minutes and during joining, if a player gets a new reward. It checks how many hours a player has already played on the network. In a Config ( rewards.yml ) a command can be deposited to a number of hours, which is executed, if the player reaches this number.

### Structure of rewards.yml

```
rewards:
    24: "command"
```

Note that [user] and [player] are replaced by the name of the player in the command.


To execute commands from the bungee on a spigot server, [this additional plugin](https://www.spigotmc.org/resources/globalexecute.15732/) is required.
However only the spigot part ( GlobalExecute[Spigot].jar ) is needed.

#### Please note that this plugin has not yet been tested!

##### This plugin was written in a short time as an extension and therefore does not meet the highest demands of beautiful source code.
