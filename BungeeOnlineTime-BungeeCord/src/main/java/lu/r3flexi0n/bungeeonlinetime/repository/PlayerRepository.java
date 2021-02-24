package lu.r3flexi0n.bungeeonlinetime.repository;

import lu.r3flexi0n.bungeeonlinetime.database.RewardResult;
import lu.r3flexi0n.bungeeonlinetime.database.ThrowingFunction;
import lu.r3flexi0n.bungeeonlinetime.database.TimeResult;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlayerRepository extends Repository {

    public PlayerRepository(DataSource dataSource) {
        super(dataSource);
    }

    public void addPlayer(ProxiedPlayer player) {
        String sql = "CALL addUser(?, ?)";
        try (CallableStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getName());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<RewardResult> getRewards(String name) {
        String sql = "CALL getRewardsByName(?, ?);";
        try (CallableStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, name);
            statement.registerOutParameter(2, Types.CHAR);
            return getRewardResult(statement, s -> name, s -> UUID.fromString(s.getString(2)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<RewardResult> getRewards(UUID uuid) {
        String sql = "CALL getRewardsByUUID(?, ?);";
        try (CallableStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, uuid.toString());
            statement.registerOutParameter(2, Types.VARCHAR);
            return getRewardResult(statement, s -> s.getString(2), s -> uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void addReward(UUID uuid, int rewardId) {
        String sql = "CALL addRewardForUUID(?, ?)";
        try (PreparedStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, rewardId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<TimeResult> getOnlineTime(String name) {
        String sql = "CALL getOnlineTimeByName(?, ?, ?);";
        try (CallableStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, name);
            statement.registerOutParameter(2, Types.CHAR);
            statement.registerOutParameter(3, Types.BIGINT);
            statement.execute();
            return Optional.of(new TimeResult(statement.getLong(3), name,
                    UUID.fromString(statement.getString(2))));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<TimeResult> getOnlineTime(UUID uuid) {
        String sql = "{CALL getOnlineTimeByUUID(?, ?, ?)}";
        try (CallableStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, uuid.toString());
            statement.registerOutParameter(2, Types.VARCHAR);
            statement.registerOutParameter(3, Types.BIGINT);
            statement.execute();
            return Optional.of(new TimeResult(statement.getLong(3), statement.getString(2),
                    uuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void updateOnlineTime(UUID uuid, long time) {
        String sql = "CALL addTimeForUUID(?, ?)";
        try (PreparedStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, time);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<List<TimeResult>> getTopTimes(int limit) {
        String sql = "CALL getTopTimes(?);";
        try (PreparedStatement statement = dataSource.getConnection().prepareCall(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TimeResult> results = new ArrayList<>((int) (limit * 1.75));
                while (resultSet.next()) {
                    results.add(new TimeResult(resultSet.getLong("time"), resultSet.getString("name"),
                            UUID.fromString(resultSet.getString("uuid"))));
                }
                return Optional.of(results);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    protected void init() {
        String sql = "CREATE TABLE IF NOT EXISTS users\n"
                + "(\n"
                + "    id   INT AUTO_INCREMENT\n"
                + "        PRIMARY KEY,\n"
                + "    uuid CHAR(36)    NOT NULL,\n"
                + "    name VARCHAR(16) NOT NULL,\n"
                + "    CONSTRAINT users_uuid_uindex\n"
                + "        UNIQUE (uuid)\n"
                + ");\n"
                + "\n"
                + "CREATE TABLE IF NOT EXISTS times\n"
                + "(\n"
                + "    user_id INT    NOT NULL\n"
                + "        PRIMARY KEY,\n"
                + "    time    BIGINT NOT NULL,\n"
                + "    CONSTRAINT times_users_id_fk\n"
                + "        FOREIGN KEY (user_id) REFERENCES users (id)\n"
                + ");\n"
                + "\n"
                + "CREATE TABLE IF NOT EXISTS rewards\n"
                + "(\n"
                + "    id        INT AUTO_INCREMENT\n"
                + "        PRIMARY KEY,\n"
                + "    user_id   INT NOT NULL,\n"
                + "    reward_id INT NOT NULL,\n"
                + "    CONSTRAINT rewards_users_id_fk\n"
                + "        FOREIGN KEY (user_id) REFERENCES users (id)\n"
                + ");\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS addUser;\n"
                + "CREATE PROCEDURE addUser(uuid_in CHAR(36), name_in VARCHAR(16))\n"
                + "BEGIN\n"
                + "    INSERT INTO users (uuid, name)\n"
                + "    VALUES (uuid_in, name_in)\n"
                + "    ON DUPLICATE KEY UPDATE name = name;\n"
                + "END;\n"
                + "\n"
                + "DROP FUNCTION IF EXISTS getIDByUUID;\n"
                + "CREATE FUNCTION getIDByUUID(uuid_in CHAR(36))\n"
                + "    RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN (SELECT id FROM users WHERE uuid = uuid_in LIMIT 1);\n"
                + "END;\n"
                + "\n"
                + "DROP FUNCTION IF EXISTS getIDByName;\n"
                + "CREATE FUNCTION getIDByName(name_in VARCHAR(16))\n"
                + "    RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN (SELECT id FROM users WHERE name = name_in LIMIT 1);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS getRewardsByUUID;\n"
                + "CREATE PROCEDURE getRewardsByUUID(IN uuid_in CHAR(36), OUT name_out VARCHAR(16))\n"
                + "BEGIN\n"
                + "    SELECT name INTO name_out FROM users WHERE uuid = uuid_in;\n"
                + "    SELECT reward_id FROM rewards WHERE user_id = getIDByUUID(uuid_in);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS getRewardsByName;\n"
                + "CREATE PROCEDURE getRewardsByName(IN name_in VARCHAR(16), OUT uuid_out CHAR(36))\n"
                + "BEGIN\n"
                + "    SELECT uuid INTO uuid_out FROM users WHERE name = name_in;\n"
                + "    SELECT reward_id FROM rewards WHERE user_id = getIDByUUID(name_in);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS getOnlineTimeByUUID;\n"
                + "CREATE PROCEDURE getOnlineTimeByUUID(IN uuid_in CHAR(36), OUT name_out VARCHAR(16), OUT time_out BIGINT)\n"
                + "BEGIN\n"
                + "    SELECT name INTO name_out FROM users WHERE uuid = uuid_in;\n"
                + "    SELECT time INTO time_out FROM times WHERE user_id = getIDByUUID(uuid_in);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS getOnlineTimeByName;\n"
                + "CREATE PROCEDURE getOnlineTimeByName(IN name_in VARCHAR(16), OUT uuid_out CHAR(36), OUT time_out BIGINT)\n"
                + "BEGIN\n"
                + "    SELECT uuid INTO uuid_out FROM users WHERE name = name_in;\n"
                + "    SELECT time INTO time_out FROM times WHERE user_id = getIDByName(name_in);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS addTimeForUUID;\n"
                + "CREATE PROCEDURE addTimeForUUID(uuid_in CHAR(36), time_in BIGINT)\n"
                + "BEGIN\n"
                + "    INSERT INTO times\n"
                + "    SET time    = time_in,\n"
                + "        user_id = getIDByUUID(uuid_in)\n"
                + "    ON DUPLICATE KEY UPDATE time = time + time_in;\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS addRewardForUUID;\n"
                + "CREATE PROCEDURE addRewardForUUID(uuid_in CHAR(36), reward_in INT)\n"
                + "BEGIN\n"
                + "    INSERT INTO rewards\n"
                + "    SET reward_id = reward_in,\n"
                + "        user_id   = getIDByUUID(uuid_in);\n"
                + "END;\n"
                + "\n"
                + "DROP PROCEDURE IF EXISTS getTopTimes;\n"
                + "CREATE PROCEDURE getTopTimes(limit_in INT)\n"
                + "BEGIN\n"
                + "    SELECT time,name,uuid FROM times \n"
                + "        LEFT JOIN users u ON times.user_id = u.id ORDER BY time DESC LIMIT limit_in;\n"
                + "END;";
        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Optional<RewardResult> getRewardResult(
            CallableStatement statement,
            ThrowingFunction<CallableStatement, String, SQLException> name,
            ThrowingFunction<CallableStatement, UUID, SQLException> uuid) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            Set<Integer> rewards = new HashSet<>();
            String rName = name.apply(statement);
            UUID rUUID = uuid.apply(statement);
            while (resultSet.next()) {
                rewards.add(resultSet.getInt("reward_id"));
            }
            return Optional.of(new RewardResult(rName, rUUID, rewards));
        }
    }
}
