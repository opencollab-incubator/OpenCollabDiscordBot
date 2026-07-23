/*
 * Copyright (c) 2020-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.discordbot.storage;


import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.permissions.UserRank;
import org.geysermc.discordbot.util.PropertiesManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MySQLStorageManager extends AbstractStorageManager {

    protected Connection connection;

    @Override
    public void setupStorage() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        connection = DriverManager.getConnection("jdbc:mysql://" + PropertiesManager.getHost() + "/" + PropertiesManager.getDatabase(), PropertiesManager.getUser(), PropertiesManager.getPass());

        Statement createTables = connection.createStatement();
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `preferences` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `key` VARCHAR(32), `value` TEXT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `pref_constraint` (`server`,`key`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `persistent_roles` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `user` BIGINT NOT NULL, `role` BIGINT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `role_constraint` (`server`,`user`,`role`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `mod_log` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `time` BIGINT NOT NULL, `user` BIGINT NOT NULL, `action` VARCHAR(32) NOT NULL, `target` BIGINT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `levels` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `user` BIGINT NOT NULL, `level` INT NOT NULL, `xp` INT NOT NULL, `messages` INT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `level_constraint` (`server`,`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `slow_mode` (`channel` BIGINT NOT NULL, `server` BIGINT NOT NULL, `delay` INT NOT NULL, PRIMARY KEY(`channel`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `tickets` (`client_id` VARCHAR(32) NOT NULL, `id` INT NOT NULL, PRIMARY KEY(`client_id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `appeals` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `time` BIGINT NOT NULL, `user` BIGINT NOT NULL, `message` TEXT NOT NULL, `state` INT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_permissions` (`user` BIGINT NOT NULL, `permissions` INT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_role_permissions` (`snowflake` BIGINT NOT NULL, `permissions` INT NOT NULL, PRIMARY KEY(`snowflake`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_ranks` (`user` BIGINT NOT NULL, `ordinal` INT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_ranks` (`user` BIGINT NOT NULL, `ordinal` INT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `auth_sessions` (`token_hash` VARCHAR(64) NOT NULL, `user` BIGINT NOT NULL, `attributes` TEXT NOT NULL, `expires_at` BIGINT NOT NULL, PRIMARY KEY(`token_hash`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `bans` (`user` BIGINT NOT NULL, `issued_on` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL, PRIMARY KEY(`user`))");
        createTables.close();
    }

    @Override
    public void closeStorage() {
        try {
            connection.close();
        } catch (SQLException ignored) { }
    }

    private void checkConnection() {
        try {
            if (connection.isValid(0)) return;
        } catch (SQLException e) { }

        try {
            setupStorage();
        } catch (Exception e) {
            GeyserBot.LOGGER.error("Failed to reconnect to MySQL database", e);
        }
    }

    @Override
    public String getServerPreference(long serverID, String preference) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `value` FROM `preferences` WHERE `server`=? AND `key`=?;")) {
            getPreferenceValue.setLong(1, serverID);
            getPreferenceValue.setString(2, preference);

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void setServerPreference(long serverID, String preference, String value) {
        checkConnection();
        try (PreparedStatement updatePreferenceValue = connection.prepareStatement(
                "INSERT INTO `preferences` (`server`, `key`, `value`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `value`=?;")) {
            updatePreferenceValue.setLong(1, serverID);
            updatePreferenceValue.setString(2, preference);
            updatePreferenceValue.setString(3, value);
            updatePreferenceValue.setString(4, value);
            updatePreferenceValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void addPersistentRole(Member member, Role role) {
        checkConnection();
        try (PreparedStatement addPersistentRole = connection.prepareStatement(
                "INSERT INTO `persistent_roles` (`server`, `user`, `role`) VALUES (?, ?, ?);")) {
            addPersistentRole.setLong(1, member.getGuild().getIdLong());
            addPersistentRole.setLong(2, member.getIdLong());
            addPersistentRole.setLong(3, role.getIdLong());
            addPersistentRole.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void removePersistentRole(Member member, Role role) {
        checkConnection();
        try (PreparedStatement removePersistentRole = connection.prepareStatement(
                "DELETE FROM `persistent_roles` WHERE `server`=? AND `user`=? AND `role`=?;")) {
            removePersistentRole.setLong(1, member.getGuild().getIdLong());
            removePersistentRole.setLong(2, member.getIdLong());
            removePersistentRole.setLong(3, role.getIdLong());
            removePersistentRole.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<Role> getPersistentRoles(Member member) {
        checkConnection();
        List<Role> roles = new ArrayList<>();

        try (PreparedStatement getPersistentRoles = connection.prepareStatement(
                "SELECT `role` FROM `persistent_roles` WHERE `server`=? AND `user`=?;")) {
            getPersistentRoles.setLong(1, member.getGuild().getIdLong());
            getPersistentRoles.setLong(2, member.getIdLong());

            try (ResultSet rs = getPersistentRoles.executeQuery()) {
                while (rs.next()) {
                    roles.add(member.getGuild().getRoleById(rs.getString("role")));
                }
            }
        } catch (SQLException ignored) { }

        return roles;
    }

    @Override
    public int addLog(User user, Guild guild, String action, UserSnowflake target, String reason) {
        checkConnection();
        try {
            long time = Instant.now().getEpochSecond();

            try (PreparedStatement addLogEntry = connection.prepareStatement(
                    "INSERT INTO `mod_log` (`server`, `time`, `user`, `action`, `target`, `reason`) VALUES (?, ?, ?, ?, ?, ?);")) {
                addLogEntry.setLong(1, guild.getIdLong());
                addLogEntry.setLong(2, time);
                addLogEntry.setLong(3, user.getIdLong());
                addLogEntry.setString(4, action);
                addLogEntry.setLong(5, target.getIdLong());
                addLogEntry.setString(6, reason);
                addLogEntry.executeUpdate();
            }

            try (PreparedStatement getLogEntry = connection.prepareStatement(
                    "SELECT `id` FROM `mod_log` WHERE `server`=? AND `time`=? AND `user`=? AND `action`=? AND `target`=? AND `reason`=? LIMIT 1;")) {
                getLogEntry.setLong(1, guild.getIdLong());
                getLogEntry.setLong(2, time);
                getLogEntry.setLong(3, user.getIdLong());
                getLogEntry.setString(4, action);
                getLogEntry.setLong(5, target.getIdLong());
                getLogEntry.setString(6, reason);

                try (ResultSet rs = getLogEntry.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (SQLException ignored) { }

        return -1;
    }

    @Override
    public List<ModLog> getLogs(Guild guild, UserSnowflake target, int limit) {
        checkConnection();
        List<ModLog> logs = new ArrayList<>();

        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `id`, `time`, `user`, `action`, `reason` FROM `mod_log` WHERE `server`=? AND `target`=? ORDER BY `time` ASC LIMIT ?;")) {
            getLogEntry.setLong(1, guild.getIdLong());
            getLogEntry.setLong(2, target.getIdLong());
            getLogEntry.setInt(3, limit);

            try (ResultSet rs = getLogEntry.executeQuery()) {
                while (rs.next()) {
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));
                    Member user = guild.getMemberById(rs.getLong("user"));

                    logs.add(new ModLog(rs.getInt("id"), time, user, rs.getString("action"), target, rs.getString("reason")));
                }
            }
        } catch (SQLException ignored) { }

        return logs;
    }

    @Override
    public ModLog getLog(Guild guild, int id) {
        checkConnection();
        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `id`, `time`, `user`, `action`, `target`, `reason` FROM `mod_log` WHERE `server`=? AND `id`=?;")) {
            getLogEntry.setLong(1, guild.getIdLong());
            getLogEntry.setInt(2, id);

            try (ResultSet rs = getLogEntry.executeQuery()) {
                if (rs.next()) {
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));
                    Member user = guild.getMemberById(rs.getLong("user"));
                    UserSnowflake target = guild.getJDA().getUserById(rs.getLong("target"));

                    // Construct a user from the id
                    if (target == null) {
                        target = User.fromId(rs.getLong("target"));
                    }

                    return new ModLog(rs.getInt("id"), time, user, rs.getString("action"), target, rs.getString("reason"));
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void updateLog(Guild guild, int id, String reason) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "UPDATE `mod_log` SET `reason`=? WHERE `id`=?;")) {
            updateLevelValue.setString(1, reason);
            updateLevelValue.setInt(2, id);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public LevelInfo getLevel(Member user) {
        checkConnection();
        try (PreparedStatement getLevelValue = connection.prepareStatement(
                "SELECT `level`, `xp`, `messages` FROM `levels` WHERE `server`=? AND `user`=?;")) {
            getLevelValue.setLong(1, user.getGuild().getIdLong());
            getLevelValue.setLong(2, user.getIdLong());

            try (ResultSet rs = getLevelValue.executeQuery()) {
                if (rs.next()) {
                    return new LevelInfo(user.getIdLong(), rs.getInt("level"), rs.getInt("xp"), rs.getInt("messages"));
                }
            }

            return new LevelInfo(0, 0, 0, 0);
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void setLevel(Member user, LevelInfo levelInfo) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `levels` (`server`, `user`, `level`, `xp`, `messages`) VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `level`=?, `xp`=?, `messages`=?;")) {
            updateLevelValue.setLong(1, user.getGuild().getIdLong());
            updateLevelValue.setLong(2, user.getIdLong());
            updateLevelValue.setInt(3, levelInfo.getLevel());
            updateLevelValue.setInt(4, levelInfo.getXp());
            updateLevelValue.setInt(5, levelInfo.getMessages());
            updateLevelValue.setInt(6, levelInfo.getLevel());
            updateLevelValue.setInt(7, levelInfo.getXp());
            updateLevelValue.setInt(8, levelInfo.getMessages());
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<LevelInfo> getLevels(long guildId) {
        checkConnection();
        List<LevelInfo> levels = new ArrayList<>();
        try (PreparedStatement getLevelValue = connection.prepareStatement(
                "SELECT `user`, `level`, `xp`, `messages` FROM `levels` WHERE `server`=? ORDER BY xp DESC LIMIT 100;")) {
            getLevelValue.setLong(1, guildId);

            try (ResultSet rs = getLevelValue.executeQuery()) {
                while (rs.next()) {
                    levels.add(new LevelInfo(rs.getLong("user"), rs.getInt("level"), rs.getInt("xp"), rs.getInt("messages")));
                }
            }
        } catch (SQLException ignored) { }

        return levels;
    }

    @Override
    public List<SlowModeInfo> getSlowModeChannels(Guild guild) {
        checkConnection();
        List<SlowModeInfo> infos = new ArrayList<>();

        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `channel`, `server`, `delay` FROM `slow_mode` WHERE `server`=?;")) {
            getLogEntry.setLong(1, guild.getIdLong());

            try (ResultSet rs = getLogEntry.executeQuery()) {
                while (rs.next()) {
                    infos.add(new SlowModeInfo(rs.getLong("server"), rs.getLong("channel"), rs.getInt("delay")));
                }
            }
        } catch (SQLException ignored) { }

        return infos;
    }

    @Override
    public void setSlowModeChannel(TextChannel channel, int delay) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `slow_mode` (`channel`, `server`, `delay`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `delay`=?;")) {
            updateLevelValue.setLong(1, channel.getIdLong());
            updateLevelValue.setLong(2, channel.getGuild().getIdLong());
            updateLevelValue.setInt(3, delay);
            updateLevelValue.setInt(4, delay);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public int getAndIncrementTicketId(String clientId) {
        checkConnection();

        int id = -1;

        try {
            try (PreparedStatement getCurrentIdEntry = connection.prepareStatement(
                    "SELECT `id` FROM `tickets` WHERE `client_id`=?;")) {
                getCurrentIdEntry.setString(1, clientId);

                try (ResultSet rs = getCurrentIdEntry.executeQuery()) {
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                }
            }

            if (id != -1) {
                try (PreparedStatement setCurrentIdEntry = connection.prepareStatement(
                        "UPDATE `tickets` SET `id` = ? WHERE `client_id`=?;")) {
                    setCurrentIdEntry.setInt(1, id + 1);
                    setCurrentIdEntry.setString(2, clientId);
                    setCurrentIdEntry.execute();
                }
            } else {
                try (PreparedStatement setCurrentIdEntry = connection.prepareStatement(
                        "INSERT INTO `tickets` (`client_id`, `id`) VALUES (?, 0);")) {
                    setCurrentIdEntry.setString(1, clientId);
                    setCurrentIdEntry.execute();
                }

                id = 0;
            }
        } catch (SQLException ignored) { }

        return id;
    }

    @Override
    public CompletableFuture<Map<User, List<Appeal>>> getCurrentAppeals() {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            Map<User, List<Appeal>> appeals = new LinkedHashMap<>();

            try {
                Statement getLogEntry = connection.createStatement();
                ResultSet rs = getLogEntry.executeQuery("SELECT `id`, `server`, `time`, `user`, `message`, `state` FROM `appeals` ORDER BY `time`;");

                while (rs.next()) {
                    Guild guild = GeyserBot.getJDA().getGuildById(rs.getString("server"));
                    String userId = rs.getString("user");
                    User user = GeyserBot.getJDA().getUserById(userId);
                    if (user == null) {
                        user = GeyserBot.getJDA().retrieveUserById(userId).complete();
                    }

                    appeals.computeIfAbsent(user, ignored -> new ArrayList<>()).add(
                            new Appeal(Instant.ofEpochSecond(rs.getLong("time")), guild, rs.getString("message"), Appeal.State.values()[rs.getInt("state")])
                    );
                }

                getLogEntry.close();
            } catch (SQLException ignored) { }

            return appeals;
        });
    }

    @Override
    public List<Appeal> getCurrentAppeals(User user) {
        checkConnection();
        List<Appeal> appeals = new ArrayList<>();

        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `id`, `server`, `time`, `user`, `message`, `state` FROM `appeals` WHERE `user`=? ORDER BY `time`;")) {
            getLogEntry.setLong(1, user.getIdLong());

            try (ResultSet rs = getLogEntry.executeQuery()) {
                while (rs.next()) {
                    Guild guild = GeyserBot.getJDA().getGuildById(rs.getString("server"));

                    appeals.add(new Appeal(Instant.ofEpochSecond(rs.getLong("time")), guild, rs.getString("message"), Appeal.State.values()[rs.getInt("state")]));
                }
            }
        } catch (SQLException ignored) { }

        return appeals;
    }

    @Override
    public void addAppeal(User user, Appeal appeal) {
        checkConnection();
        try (PreparedStatement addPersistentRole = connection.prepareStatement(
                "INSERT INTO `appeals` (`server`, `time`, `user`, `message`, `state`) VALUES (?, ?, ?, ?, ?);")) {
            long time = appeal.creationTime().getEpochSecond();
            addPersistentRole.setLong(1, appeal.appealingFrom().getIdLong());
            addPersistentRole.setLong(2, time);
            addPersistentRole.setLong(3, user.getIdLong());
            addPersistentRole.setString(4, appeal.message());
            addPersistentRole.setInt(5, appeal.state().ordinal());
            addPersistentRole.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public MemberRolePermissions[] getMemberPermissions(Member member) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `permissions` FROM `member_role_permissions` WHERE `snowflake`=?;")) {
            getPreferenceValue.setLong(1, member.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return MemberRolePermissions.fromRawValue(rs.getInt("permissions"));
                }
            }
        } catch (SQLException ignored) { }

        return new MemberRolePermissions[0];
    }

    @Override
    public MemberRolePermissions[] getRolePermissions(Role role) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `permissions` FROM `member_role_permissions` WHERE `snowflake`=?;")) {
            getPreferenceValue.setLong(1, role.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return MemberRolePermissions.fromRawValue(rs.getInt("permissions"));
                }
            }
        } catch (SQLException ignored) { }

        return new MemberRolePermissions[0];
    }

    @Override
    public UserPermissions[] getUserPermissions(User user) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `permissions` FROM `user_permissions` WHERE `user`=?;")) {
            getPreferenceValue.setLong(1, user.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return UserPermissions.fromRawValue(rs.getInt("permissions"));
                }
            }
        } catch (SQLException ignored) { }

        return new UserPermissions[0];
    }

    @Override
    public UserRank getUserRank(User user) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `ordinal` FROM `user_ranks` WHERE `user`=?;")) {
            getPreferenceValue.setLong(1, user.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return UserRank.values()[rs.getInt("ordinal")];
                }
            }
        } catch (SQLException ignored) { }

        return UserRank.NONE;
    }

    @Override
    public MemberRank getMemberRank(Member member) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `ordinal` FROM `member_ranks` WHERE `user`=?;")) {
            getPreferenceValue.setLong(1, member.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    return MemberRank.values()[rs.getInt("ordinal")];
                }
            }
        } catch (SQLException ignored) { }

        return MemberRank.MEMBER;
    }

    @Override
    public void setMemberPermissions(Member member, MemberRolePermissions[] permissions) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_role_permissions` (`snowflake`, `permissions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `permissions`=?;")) {
            int value = MemberRolePermissions.getRawValue(permissions);
            updateLevelValue.setLong(1, member.getIdLong());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setRolePermissions(Role role, MemberRolePermissions[] permissions) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_role_permissions` (`snowflake`, `permissions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `permissions`=?;")) {
            int value = MemberRolePermissions.getRawValue(permissions);
            updateLevelValue.setLong(1, role.getIdLong());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setUserPermissions(User user, UserPermissions[] permissions) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `user_permissions` (`user`, `permissions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `permissions`=?;")) {
            int value = UserPermissions.getRawValue(permissions);
            updateLevelValue.setLong(1, user.getIdLong());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setUserRank(User user, UserRank rank) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `user_ranks` (`user`, `ordinal`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `ordinal`=?;")) {
            int value = rank.ordinal();
            updateLevelValue.setLong(1, user.getIdLong());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setMemberRank(Member member, MemberRank rank) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_ranks` (`user`, `ordinal`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `ordinal`=?;")) {
            int value = rank.ordinal();
            updateLevelValue.setLong(1, member.getIdLong());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void saveAuthSession(String tokenHash, long discordUserId, String attributesJson, long expiresAt) {
        checkConnection();
        try (PreparedStatement saveAuthSession = connection.prepareStatement(
                "INSERT INTO `auth_sessions` (`token_hash`, `user`, `attributes`, `expires_at`) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `user`=?, `attributes`=?, `expires_at`=?;")) {
            saveAuthSession.setString(1, tokenHash);
            saveAuthSession.setLong(2, discordUserId);
            saveAuthSession.setString(3, attributesJson);
            saveAuthSession.setLong(4, expiresAt);
            saveAuthSession.setLong(5, discordUserId);
            saveAuthSession.setString(6, attributesJson);
            saveAuthSession.setLong(7, expiresAt);
            saveAuthSession.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public AuthSession getAuthSession(String tokenHash) {
        checkConnection();
        try (PreparedStatement getAuthSession = connection.prepareStatement(
                "SELECT `user`, `attributes`, `expires_at` FROM `auth_sessions` WHERE `token_hash`=?;")) {
            getAuthSession.setString(1, tokenHash);

            try (ResultSet rs = getAuthSession.executeQuery()) {
                if (rs.next()) {
                    long expiresAt = rs.getLong("expires_at");
                    if (expiresAt < System.currentTimeMillis()) {
                        deleteAuthSession(tokenHash);
                        return null;
                    }

                    return new AuthSession(rs.getLong("user"), rs.getString("attributes"), expiresAt);
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void deleteAuthSession(String tokenHash) {
        checkConnection();
        try (PreparedStatement deleteAuthSession = connection.prepareStatement("DELETE FROM `auth_sessions` WHERE `token_hash`=?;")) {
            deleteAuthSession.setString(1, tokenHash);
            deleteAuthSession.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void addBan(BanData data) {
        checkConnection();
        try (PreparedStatement addBan = connection.prepareStatement(
                "INSERT INTO `bans` (`user`, `issued_on`, `expires_at`) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `expires_at`=?;")) {
            long expiresAt = data.expiryTime() == null ? -1 : data.expiryTime().getEpochSecond();
            addBan.setLong(1, data.userId());
            addBan.setLong(2, data.issuedOn().getEpochSecond());
            addBan.setLong(3, expiresAt);
            addBan.setLong(4, expiresAt);
            addBan.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void removeBan(long userId) {
        checkConnection();
        try (PreparedStatement removeBan = connection.prepareStatement("DELETE FROM `bans` WHERE `user`=?;")) {
            removeBan.setLong(1, userId);
            removeBan.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public BanData getBan(long userId) {
        checkConnection();
        try (PreparedStatement getAuthSession = connection.prepareStatement(
                "SELECT (`issued_on`, `expires_at`) FROM `bans` WHERE `user`=?;")) {
            getAuthSession.setLong(1, userId);

            try (ResultSet rs = getAuthSession.executeQuery()) {
                if (rs.next()) {
                    long time = rs.getLong("expires_at");
                    return new BanData(userId, Instant.ofEpochSecond(rs.getLong("issued_on")), time == -1 ? null : Instant.ofEpochSecond(time));
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public List<BanData> getBans() {
        checkConnection();
        List<BanData> bans = new ArrayList<>();
        try (PreparedStatement getAuthSession = connection.prepareStatement("SELECT * FROM `bans`;")) {
            try (ResultSet rs = getAuthSession.executeQuery()) {
                while (rs.next()) {
                    long time = rs.getLong("expires_at");
                    bans.add(new BanData(rs.getLong("user"), Instant.ofEpochSecond(rs.getLong("issued_on")), time == -1 ? null : Instant.ofEpochSecond(time)));
                }
            }
        } catch (SQLException ignored) { }

        return bans;
    }
}
