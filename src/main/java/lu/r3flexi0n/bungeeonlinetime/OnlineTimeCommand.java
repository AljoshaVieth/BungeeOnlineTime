package lu.r3flexi0n.bungeeonlinetime;

import lu.r3flexi0n.bungeeonlinetime.utils.Language;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class OnlineTimeCommand extends Command {

    public OnlineTimeCommand(String command, String permission, String... aliases) {
        super(command, permission, aliases);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

//        if (!(sender instanceof ProxiedPlayer)) {
//            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ONLY_PLAYER)));
//            return;
//        }
//        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {

            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ONLY_PLAYER)));
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (!sender.hasPermission("onlinetime.own")) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.NO_PERMISSION)));
                return;
            }

            ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
                try {

                    OnlineTime onlineTime = BungeeOnlineTime.SQL.getOnlineTime(player.getUniqueId());
                    if (onlineTime == null) {
                        player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            Language.PLAYER_NOT_FOUND.replace("%PLAYER%", player.getName()))));
                        return;
                    }

                    long seconds = onlineTime.getTime() / 1000;
                    int hours = (int) (seconds / 3600);
                    int minutes = (int) ((seconds % 3600) / 60);
                    player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                        Language.ONLINE_TIME
                            .replace("%PLAYER%", player.getName())
                            .replace("%HOURS%", String.valueOf(hours))
                            .replace("%MINUTES%", String.valueOf(minutes)))));

                } catch (Exception ex) {
                    player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ERROR)));
                    ex.printStackTrace();
                }
            });

        } else if (args.length == 2 && args[0].equals("get")) {

            if (!sender.hasPermission("onlinetime.others")) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.NO_PERMISSION)));
                return;
            }

            ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
                try {

                    OnlineTime onlineTime = BungeeOnlineTime.SQL.getOnlineTime(args[1]);
                    if (onlineTime == null) {
                        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            Language.PLAYER_NOT_FOUND
                                .replace("%PLAYER%", args[1]))));
                        return;
                    }

                    long seconds = onlineTime.getTime() / 1000;
                    int hours = (int) (seconds / 3600);
                    int minutes = (int) ((seconds % 3600) / 60);

                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                        Language.ONLINE_TIME
                            .replace("%PLAYER%", onlineTime.getName())
                            .replace("%HOURS%", String.valueOf(hours))
                            .replace("%MINUTES%", String.valueOf(minutes)))));

                } catch (Exception ex) {
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ERROR)));
                    ex.printStackTrace();
                }
            });

        } else if (args.length == 1 && args[0].equalsIgnoreCase("top")) {

            if (!sender.hasPermission("onlinetime.top")) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.NO_PERMISSION)));
                return;
            }

            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.TOP_TIME_LOADING)));

            ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
                try {

                    StringBuilder builder = new StringBuilder();
                    builder.append(Language.TOP_TIME_ABOVE);
                    builder.append("\n");
                    for (OnlineTime onlineTimes : BungeeOnlineTime.SQL.getTopOnlineTimes(BungeeOnlineTime.TOP_ONLINETIMES_LIMIT)) {

                        long seconds = onlineTimes.getTime() / 1000;
                        int hours = (int) (seconds / 3600);
                        int minutes = (int) ((seconds % 3600) / 60);

                        builder.append(Language.TOP_TIME
                                .replace("%PLAYER%", onlineTimes.getName())
                                .replace("%HOURS%", String.valueOf(hours))
                                .replace("%MINUTES%", String.valueOf(minutes)));
                        builder.append("\n");
                    }
                    builder.append(Language.TOP_TIME_BELOW);

                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',builder.toString())));

                } catch (Exception ex) {
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ERROR)));
                    ex.printStackTrace();
                }
            });

        } else if (args.length == 1 && args[0].equalsIgnoreCase("resetall")) {

            if (sender instanceof ProxiedPlayer) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ONLY_CONSOLE)));
                return;
            }

            if (!sender.hasPermission("onlinetime.resetall")) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.NO_PERMISSION)));
                return;
            }

            ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
                try {

                    BungeeOnlineTime.SQL.resetAll();
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.RESET_ALL)));

                } catch (Exception ex) {
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ERROR)));
                    ex.printStackTrace();
                }
            });

        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {

            if (!sender.hasPermission("onlinetime.reset")) {
                sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.NO_PERMISSION)));
                return;
            }

            ProxyServer.getInstance().getScheduler().runAsync(BungeeOnlineTime.INSTANCE, () -> {
                try {

                    BungeeOnlineTime.SQL.reset(args[1]);
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.RESET_PLAYER
                            .replace("%PLAYER%", args[1]))));

                } catch (Exception ex) {
                    sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',Language.ERROR)));
                    ex.printStackTrace();
                }
            });

        } else {
            String sb = "§7Usage: \n" +
                "§7/onlinetime\n" +
                "§7/onlinetime get <player>\n" +
                "§7/onlinetime top\n" +
                "§7/onlinetime reset <player>\n" +
                "§7/onlinetime resetall\n";
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', sb)));
        }
    }
}
