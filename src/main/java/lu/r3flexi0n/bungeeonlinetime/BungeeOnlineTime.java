package lu.r3flexi0n.bungeeonlinetime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lu.r3flexi0n.bungeeonlinetime.database.MySQL;
import lu.r3flexi0n.bungeeonlinetime.database.SQL;
import lu.r3flexi0n.bungeeonlinetime.database.SQLite;
import lu.r3flexi0n.bungeeonlinetime.utils.JarUtil;
import lu.r3flexi0n.bungeeonlinetime.utils.Language;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BungeeOnlineTime extends Plugin {

    public static BungeeOnlineTime INSTANCE;

    public static SQL SQL;

    public static boolean MYSQL_ENABLED = false;
    public static String COMMAND_ALIASES = "ot,pt,playtime";
    public static List<String> DISABLED_SERVERS = Arrays.asList("lobby");
    public static int TOP_ONLINETIMES_LIMIT = 10;

    private String host, database, username, password;
    private Integer port;

    public static File CONFIG_FILE;
    public static File PLAYER_FILE;
    public static File REWARD_FILE;
    public static ConfigurationProvider CONFIG_PROVIDER = ConfigurationProvider.getProvider(YamlConfiguration.class);

    public static HashMap<Integer, String> rewards;

    public static final String CHANNEL = "bungeeonlinetime:get";

    public static final Map<UUID, OnlinePlayer> ONLINE_PLAYERS = new HashMap<>();

    @Override
    public void onLoad() {
        try {
            loadJars(new File(getDataFolder(), "external"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Could not load required deps.", ex);
        }
    }

    private void loadJars(File jarsFolder, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException {
        if (jarsFolder.exists() && !jarsFolder.isDirectory()) {
            Files.delete(jarsFolder.toPath());
        }
        if (!jarsFolder.exists()) {
            if (!jarsFolder.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        try {
            Class.forName("org.sqlite.JDBC", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            System.out.println("[BungeeOnlineTime] Downloading SQLite...");
            JarUtil.loadJar("http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.25.2/sqlite-jdbc-3.25.2.jar",
                    new File(jarsFolder, "sqlite-jdbc-3.25.2.jar"),
                    classLoader);
        }

        try {
            DriverManager.getDriver("org.sqlite.JDBC");
        } catch (SQLException ignored) {
            try {
                DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, classLoader).newInstance());
            } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
                System.out.println("[BungeeOnlineTime] Error while loading SQLite...");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        try {
            createConfig();
            loadConfig();
        } catch (IOException ex) {
            System.out.println("[BungeeOnlineTime] Error while creating/loading config.");
            ex.printStackTrace();
            return;
        }

        if (MYSQL_ENABLED) {

            SQL = new MySQL(host, port, database, username, password);

        } else {
            File dbFile = new File(getDataFolder(), "BungeeOnlineTime.db");
            SQL = new SQLite(dbFile);
        }

        try {
            System.out.println("[BungeeOnlineTime] Connecting to SQL...");
            SQL.openConnection();
            SQL.createTable();
            System.out.println("[BungeeOnlineTime] Connected to SQL.");
        } catch (Exception ex) {
            System.out.println("[BungeeOnlineTime] Error while connecting to SQL.");
            ex.printStackTrace();
            return;
        }

        getProxy().getPluginManager().registerCommand(this, new OnlineTimeCommand("onlinetime", null, COMMAND_ALIASES.split(",")));
        getProxy().getPluginManager().registerListener(this, new OnlineTimeListener());
        getProxy().registerChannel(CHANNEL);

        // Scheduler to execute RewardManager.checkReward() every 5 minutes
        startRewardChecker();
    }

    private void createConfig() throws IOException {

        File folder = new File(getDataFolder().getPath());
        if (!folder.exists()) {
            folder.mkdir();
        }

        CONFIG_FILE = new File(getDataFolder(), "config.yml");
        PLAYER_FILE = new File(getDataFolder(), "players.yml");
        REWARD_FILE = new File(getDataFolder(), "rewards.yml");


        if (!CONFIG_FILE.exists()) {
            CONFIG_FILE.createNewFile();
        }
        if (!PLAYER_FILE.exists()) {
            PLAYER_FILE.createNewFile();
        }
        if (!REWARD_FILE.exists()) {
            REWARD_FILE.createNewFile();
        }

        Configuration config = CONFIG_PROVIDER.load(CONFIG_FILE);

        addDefault(config, "Settings.mysql", MYSQL_ENABLED);
        addDefault(config, "Settings.commandAliases", COMMAND_ALIASES);
        addDefault(config, "Settings.disabledServers", DISABLED_SERVERS);
        addDefault(config, "Settings.topOnlineTimesLimit", TOP_ONLINETIMES_LIMIT);

        addDefault(config, "MySQL.host", "localhost");
        addDefault(config, "MySQL.port", 3306);
        addDefault(config, "MySQL.database", "minecraft");
        addDefault(config, "MySQL.username", "player");
        addDefault(config, "MySQL.password", "abc123");

        Language.create(config);
        CONFIG_PROVIDER.save(config, CONFIG_FILE);

        Configuration rewardConfig = CONFIG_PROVIDER.load(REWARD_FILE);
        addDefault(rewardConfig, "rewards.24", "/lp user [user] promote mainTrack");
        CONFIG_PROVIDER.save(rewardConfig, REWARD_FILE);

        for(String key : rewardConfig.getSection("rewards").getKeys()){
            rewards.put(Integer.parseInt(key), rewardConfig.getString(key));
        }
    }

    private void loadConfig() throws IOException {
        Configuration config = CONFIG_PROVIDER.load(CONFIG_FILE);

        MYSQL_ENABLED = config.getBoolean("Settings.mysql");
        COMMAND_ALIASES = config.getString("Settings.commandAliases");
        DISABLED_SERVERS = config.getStringList("Settings.disabledServers");
        TOP_ONLINETIMES_LIMIT = config.getInt("Settings.topOnlineTimesLimit");

        if (MYSQL_ENABLED) {
            host = config.getString("MySQL.host");
            port = config.getInt("MySQL.port");
            database = config.getString("MySQL.database");
            username = config.getString("MySQL.username");
            password = config.getString("MySQL.password");
        }

        Language.load(config);

    }

    private void addDefault(Configuration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }

    private void startRewardChecker(){
        // Check every 5 minutes all Player for new Rewards
        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                RewardManager.checkRewards();
            }
        },5, 5, TimeUnit.MINUTES);
    }
}
