package lu.r3flexi0n.bungeeonlinetime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import lu.r3flexi0n.bungeeonlinetime.repository.PlayerRepository;
import lu.r3flexi0n.bungeeonlinetime.rewards.Reward;
import lu.r3flexi0n.bungeeonlinetime.rewards.RewardAction;
import lu.r3flexi0n.bungeeonlinetime.rewards.RewardManager;
import lu.r3flexi0n.bungeeonlinetime.rewards.serialization.RewardActionDeserializer;
import lu.r3flexi0n.bungeeonlinetime.rewards.serialization.RewardActionSerializer;
import lu.r3flexi0n.bungeeonlinetime.utils.JarUtil;
import lu.r3flexi0n.bungeeonlinetime.utils.Language;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

public class BungeeOnlineTime extends Plugin {

    public static final String CHANNEL = "bungeeonlinetime:get";
    public static final String COMMAND_CHANNEL = "bungeeonlinetime:command";

    public static final Map<UUID, OnlinePlayer> ONLINE_PLAYERS = new HashMap<>();

    public static BungeeOnlineTime INSTANCE;

    public static boolean MYSQL_ENABLED = false;
    public static String COMMAND_ALIASES = "ot,pt,playtime";
    public static List<String> DISABLED_SERVERS = Collections.singletonList("lobby");
    public static int TOP_ONLINETIMES_LIMIT = 10;

    public static File CONFIG_FILE;
    public static File PLAYER_FILE;
    public static File REWARD_FILE;
    public static ConfigurationProvider CONFIG_PROVIDER = ConfigurationProvider.getProvider(YamlConfiguration.class);

    public static Map<Integer, String> rewards = new HashMap<>();

    public static boolean DEBUG = true;

    private String host, database, username, password;
    private int port;

    private PlayerRepository repository;

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
        DataSource dataSource;
        if (MYSQL_ENABLED) {
            MysqlConnectionPoolDataSource mysqlSource = new MysqlConnectionPoolDataSource();
            mysqlSource.setServerName(host);
            mysqlSource.setPort(port);
            mysqlSource.setUser(username);
            mysqlSource.setPassword(password);
            mysqlSource.setDatabaseName(database);
            try {
                mysqlSource.setAllowMultiQueries(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dataSource = mysqlSource;
        } else {
            File dbFile = new File(getDataFolder(), "BungeeOnlineTime.db");
            SQLiteDataSource sqLiteDataSource = new SQLiteDataSource(new SQLiteConfig());
            sqLiteDataSource.setUrl(dbFile.getPath());
            dataSource = sqLiteDataSource;
        }

        try {
            System.out.println("[BungeeOnlineTime] Connecting to SQL...");
            repository = new PlayerRepository(dataSource);
            System.out.println("[BungeeOnlineTime] Connected to SQL.");
        } catch (Exception ex) {
            System.out.println("[BungeeOnlineTime] Error while connecting to SQL.");
            ex.printStackTrace();
            return;
        }

        getProxy().getPluginManager().registerCommand(this, new OnlineTimeCommand(repository, "onlinetime", null, COMMAND_ALIASES.split(",")));
        getProxy().getPluginManager().registerListener(this, new OnlineTimeListener(repository));
        getProxy().registerChannel(CHANNEL);

        // Scheduler to execute RewardManager.checkReward() every 5 minutes
        try {
            startRewardChecker();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            repository.updateOnlineTime(player.getUniqueId(), ONLINE_PLAYERS.get(player.getUniqueId()).getNoAFKTime());
        }
    }

    private void createConfig() throws IOException {

        File folder = new File(getDataFolder().getPath());
        if (!folder.exists()) {
            folder.mkdir();
        }

        CONFIG_FILE = new File(getDataFolder(), "config.yml");
        PLAYER_FILE = new File(getDataFolder(), "players.yml");
        REWARD_FILE = new File(getDataFolder(), "rewards.json");


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

        addDefault(config, "debug", true);

        Language.create(config);
        CONFIG_PROVIDER.save(config, CONFIG_FILE);
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

        DEBUG = config.getBoolean("debug");

        Language.load(config);

    }

    private void addDefault(Configuration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }

    private void startRewardChecker() throws FileNotFoundException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(RewardAction.class, new RewardActionDeserializer())
                .registerTypeAdapter(RewardAction.class, new RewardActionSerializer())
                .create();
        Type collectionType = new TypeToken<List<Reward>>(){}.getType();
        List<Reward> rewards = gson.fromJson(new FileReader(REWARD_FILE), collectionType);
        rewards = rewards == null ? Collections.emptyList() : rewards;
        System.out.printf("Loaded %d rewards.", rewards.size());
        RewardManager rewardManager = new RewardManager(rewards, repository);
        // Check every 5 minutes all Player for new Rewards
        getProxy().getScheduler().schedule(this, rewardManager, 5, 5, TimeUnit.MINUTES);
    }
}
