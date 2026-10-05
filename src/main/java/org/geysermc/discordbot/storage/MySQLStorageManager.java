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


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.geysermc.discordbot.util.ticket.TicketState;
import org.geysermc.discordbot.util.ticket.TicketType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLStorageManager extends AbstractStorageManager {
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type STRING_LIST_TYPE = new TypeToken<List<String>>(){}.getType();

    protected Connection connection;

    @Override
    public void setupStorage() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        connection = DriverManager.getConnection("jdbc:mysql://" + PropertiesManager.getHost() + "/" + PropertiesManager.getDatabase(), PropertiesManager.getUser(), PropertiesManager.getPass());

        Statement createTables = connection.createStatement();
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `preferences` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `key` VARCHAR(32), `value` TEXT, PRIMARY KEY(`id`), UNIQUE KEY `pref_constraint` (`server`,`key`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `persistent_roles` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `user` BIGINT NOT NULL, `role` BIGINT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `role_constraint` (`server`,`user`,`role`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `mod_log` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `time` BIGINT NOT NULL, `user` BIGINT NOT NULL, `action` VARCHAR(32) NOT NULL, `target` BIGINT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `levels` (`id` INT NOT NULL AUTO_INCREMENT, `server` BIGINT NOT NULL, `user` BIGINT NOT NULL, `level` INT NOT NULL, `xp` INT NOT NULL, `messages` INT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `level_constraint` (`server`,`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `slow_mode` (`channel` BIGINT NOT NULL, `server` BIGINT NOT NULL, `delay` INT NOT NULL, PRIMARY KEY(`channel`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `tickets` (`client_id` VARCHAR(32) NOT NULL, `id` INT NOT NULL, PRIMARY KEY(`client_id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `appeals` (`id` INT NOT NULL AUTO_INCREMENT, `action_time` BIGINT NOT NULL, `time` BIGINT NOT NULL, `user` BIGINT NOT NULL, `message` TEXT NOT NULL, `state` INT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_permissions` (`user` BIGINT NOT NULL, `permissions` TEXT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_role_permissions` (`guild_snowflake` TEXT NOT NULL, `permissions` TEXT NOT NULL, PRIMARY KEY(`guild_snowflake`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_ranks` (`user` BIGINT NOT NULL, `ranks` TEXT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_ranks` (`guild_member` TEXT NOT NULL, `ranks` TEXT NOT NULL, PRIMARY KEY(`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `auth_sessions` (`token_hash` VARCHAR(64) NOT NULL, `user` BIGINT NOT NULL, `attributes` TEXT NOT NULL, `expires_at` BIGINT NOT NULL, PRIMARY KEY(`token_hash`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `bans` (`user` BIGINT NOT NULL, `issued_on` BIGINT NOT NULL, `expires_at` BIGINT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`user`))");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_metadata` (`id` INT NOT NULL AUTO_INCREMENT, `guild_id` BIGINT NOT NULL, `channel_id` BIGINT NULL, `created_at` BIGINT NOT NULL, `ticket_number` VARCHAR(32) NOT NULL, `client_id` VARCHAR(64) NOT NULL, `client_name` VARCHAR(255) NOT NULL, `client_role_id` VARCHAR(32) NOT NULL, `type` VARCHAR(32) NOT NULL, `state` INT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_log` (`id` INT NOT NULL AUTO_INCREMENT, `ticket_id` INT NOT NULL, `time` BIGINT NOT NULL, `line` TEXT NOT NULL, PRIMARY KEY(`id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_messages` (`id` INT NOT NULL AUTO_INCREMENT, `ticket_id` INT NOT NULL, `message_id` BIGINT NOT NULL, `author_id` BIGINT NOT NULL, `author_name` VARCHAR(255) NOT NULL, `author_avatar_url` TEXT, `content` MEDIUMTEXT NOT NULL, `attachments` TEXT NOT NULL, `sent_at` BIGINT NOT NULL, `edited` INT NOT NULL, `deleted` INT NOT NULL, PRIMARY KEY(`id`), UNIQUE KEY `ticket_message_constraint` (`ticket_id`,`message_id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `bad_images` (`id` INT NOT NULL AUTO_INCREMENT, `similarity_hash` TEXT NOT NULL, `b64_source` TEXT NOT NULL, PRIMARY KEY(`id`));");
        createTables.close();
    }

    @Override
    public void closeStorage() {
        try {
            connection.close();
        } catch (SQLException ignored) { }
    }

    protected void checkConnection() {
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
    public int addAppeal(UserSnowflake user, String message) {
        checkConnection();
        try {
            try (PreparedStatement addAppealEntry = connection.prepareStatement(
                    "INSERT INTO `appeals` (`user`, `action_time`, `time`, `message`, `state`) VALUES (?, ?, ?, ?, ?);")) {
                addAppealEntry.setLong(1, user.getIdLong());
                addAppealEntry.setLong(2, -1);
                addAppealEntry.setLong(3, Instant.now().getEpochSecond());
                addAppealEntry.setString(4, message);
                addAppealEntry.setInt(5, Appeal.State.PENDING.ordinal());
                addAppealEntry.executeUpdate();
            }

            try (PreparedStatement getAppealEntry = connection.prepareStatement(
                    "SELECT `id` FROM `appeals` WHERE `user`=? AND `action_time`=? AND `time`=? AND `message`=? AND `state`=? LIMIT 1;")) {
                getAppealEntry.setLong(1, user.getIdLong());
                getAppealEntry.setLong(2, -1);
                getAppealEntry.setLong(3, Instant.now().getEpochSecond());
                getAppealEntry.setString(4, message);
                getAppealEntry.setInt(5, Appeal.State.PENDING.ordinal());

                try (ResultSet rs = getAppealEntry.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (SQLException ignored) { }

        return -1;
    }

    @Override
    public List<Appeal> getAppeals(UserSnowflake user, int limit) {
        checkConnection();
        List<Appeal> appeals = new ArrayList<>();

        try (PreparedStatement getAppealEntry = connection.prepareStatement(
                "SELECT `id`, `action_time`, `time`, `message`, `state` FROM `appeals` WHERE `user`=? ORDER BY `time` ASC LIMIT ?;")) {
            getAppealEntry.setLong(1, user.getIdLong());
            getAppealEntry.setInt(2, limit);

            try (ResultSet rs = getAppealEntry.executeQuery()) {
                while (rs.next()) {
                    long actionTimeEpoch = rs.getLong("action_time");
                    Instant actionTime = Instant.ofEpochSecond(actionTimeEpoch);
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));

                    appeals.add(new Appeal(rs.getInt("id"), user, time, actionTime, rs.getString("message"), Appeal.State.values()[rs.getInt("state")]));
                }
            }
        } catch (SQLException ignored) { }

        return appeals;
    }

    @Override
    public List<Appeal> getAppeals(int limit) {
        checkConnection();
        List<Appeal> appeals = new ArrayList<>();

        try (PreparedStatement getAppealEntry = connection.prepareStatement(
                "SELECT `id`, `user`, `action_time`, `time`, `message`, `state` FROM `appeals` ORDER BY `time` ASC LIMIT ?;")) {
            getAppealEntry.setInt(1, limit);

            try (ResultSet rs = getAppealEntry.executeQuery()) {
                while (rs.next()) {
                    long actionTimeEpoch = rs.getLong("action_time");
                    Instant actionTime = Instant.ofEpochSecond(actionTimeEpoch);
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));

                    appeals.add(new Appeal(rs.getInt("id"), User.fromId(rs.getLong("user")), time, actionTime, rs.getString("message"), Appeal.State.values()[rs.getInt("state")]));
                }
            }
        } catch (SQLException ignored) { }

        return appeals;
    }

    @Override
    public Appeal getAppeal(int id) {
        checkConnection();
        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `action_time`, `time`, `user`, `message`, `state` FROM `appeals` WHERE `id`=?;")) {
            getLogEntry.setInt(1, id);

            try (ResultSet rs = getLogEntry.executeQuery()) {
                if (rs.next()) {
                    long actionTimeEpoch = rs.getLong("action_time");
                    Instant actionTime = Instant.ofEpochSecond(actionTimeEpoch);
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));
                    UserSnowflake target = User.fromId(rs.getLong("target"));

                    return new Appeal(id, target, time, actionTime, rs.getString("message"), Appeal.State.values()[rs.getInt("state")]);
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void updateAppeal(int id, Appeal.State state) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "UPDATE `appeals` SET `state`=?, `action_time`=? WHERE `id`=?;")) {
            updateLevelValue.setInt(1, state.ordinal());
            updateLevelValue.setLong(2, Instant.now().getEpochSecond());
            updateLevelValue.setInt(3, id);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public MemberRolePermissions[] getMemberPermissions(Member member) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `permissions` FROM `member_role_permissions` WHERE `guild_snowflake`=?;")) {
            getPreferenceValue.setString(1, member.getGuild().getId() + "_" + member.getId());

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
    public UserRank[] getUserRanks(User user) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `ranks` FROM `user_ranks` WHERE `user`=?;")) {
            getPreferenceValue.setLong(1, user.getIdLong());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    String ranks = rs.getString("ranks");
                    if (ranks.isEmpty()) return new UserRank[0];
                    return Arrays.stream(ranks.split(",")).map(UserRank::valueOf).toArray(UserRank[]::new);
                }
            }
        } catch (SQLException ignored) { }

        return new UserRank[0];
    }

    @Override
    public MemberRank[] getMemberRanks(Member member) {
        checkConnection();
        try (PreparedStatement getPreferenceValue = connection.prepareStatement(
                "SELECT `ranks` FROM `member_ranks` WHERE `guild_member`=?;")) {
            getPreferenceValue.setString(1, member.getGuild().getId() + "_" + member.getId());

            try (ResultSet rs = getPreferenceValue.executeQuery()) {
                if (rs.next()) {
                    String ranks = rs.getString("ranks");
                    if (ranks.isEmpty()) return new MemberRank[0];
                    return Arrays.stream(ranks.split(",")).map(MemberRank::valueOf).toArray(MemberRank[]::new);
                }
            }
        } catch (SQLException ignored) { }

        return new MemberRank[0];
    }

    @Override
    public void setMemberPermissions(Member member, MemberRolePermissions[] permissions) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_role_permissions` (`guild_snowflake`, `permissions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `permissions`=?;")) {
            int value = MemberRolePermissions.getRawValue(permissions);
            updateLevelValue.setString(1, member.getGuild().getId() + "_" + member.getId());
            updateLevelValue.setInt(2, value);
            updateLevelValue.setInt(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setRolePermissions(Role role, MemberRolePermissions[] permissions) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_role_permissions` (`guild_snowflake`, `permissions`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `permissions`=?;")) {
            int value = MemberRolePermissions.getRawValue(permissions);
            updateLevelValue.setString(1, role.getGuild().getId() + "_" + role.getId());
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
    public void setUserRanks(User user, UserRank[] ranks) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `user_ranks` (`user`, `ranks`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `ranks`=?;")) {
            String value = Arrays.stream(ranks).map(UserRank::name).collect(Collectors.joining(","));
            updateLevelValue.setLong(1, user.getIdLong());
            updateLevelValue.setString(2, value);
            updateLevelValue.setString(3, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setMemberRanks(Member member, MemberRank[] ranks) {
        checkConnection();
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT INTO `member_ranks` (`guild_member`, `ranks`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `ranks`=?;")) {
            String value = Arrays.stream(ranks).map(MemberRank::name).collect(Collectors.joining(","));
            updateLevelValue.setString(1, member.getGuild().getId() + "_" + member.getId());
            updateLevelValue.setString(2, value);
            updateLevelValue.setString(3, value);
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
                "SELECT (`issued_on`, `expires_at`, `reason`) FROM `bans` WHERE `user`=?;")) {
            getAuthSession.setLong(1, userId);

            try (ResultSet rs = getAuthSession.executeQuery()) {
                if (rs.next()) {
                    long time = rs.getLong("expires_at");
                    return new BanData(userId, Instant.ofEpochSecond(rs.getLong("issued_on")), time == -1 ? null : Instant.ofEpochSecond(time), rs.getString("reason"));
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
                    bans.add(new BanData(rs.getLong("user"), Instant.ofEpochSecond(rs.getLong("issued_on")), time == -1 ? null : Instant.ofEpochSecond(time), rs.getString("reason")));
                }
            }
        } catch (SQLException ignored) { }

        return bans;
    }

    @Override
    public int createTicket(TicketMetadata metadata) {
        checkConnection();
        try (PreparedStatement createTicket = connection.prepareStatement(
                "INSERT INTO `ticket_metadata` (`guild_id`, `channel_id`, `created_at`, `ticket_number`, `client_id`, `client_name`, `client_role_id`, `type`, `state`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS)) {
            createTicket.setLong(1, metadata.guildId());
            if (metadata.channelId() == null) {
                createTicket.setNull(2, Types.BIGINT);
            } else {
                createTicket.setLong(2, metadata.channelId());
            }
            createTicket.setLong(3, metadata.createdAt().getEpochSecond());
            createTicket.setString(4, metadata.id());
            createTicket.setString(5, metadata.clientId());
            createTicket.setString(6, metadata.clientName());
            createTicket.setString(7, metadata.clientRoleId());
            createTicket.setString(8, metadata.type().name());
            createTicket.setInt(9, metadata.state().ordinal());
            createTicket.executeUpdate();

            try (ResultSet rs = createTicket.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) { }

        return -1;
    }

    @Override
    public void updateTicket(TicketMetadata metadata) {
        checkConnection();
        try (PreparedStatement updateTicket = connection.prepareStatement(
                "UPDATE `ticket_metadata` SET `channel_id`=?, `client_name`=?, `client_role_id`=?, `type`=?, `state`=? WHERE `id`=?;")) {
            if (metadata.channelId() == null) {
                updateTicket.setNull(1, Types.BIGINT);
            } else {
                updateTicket.setLong(1, metadata.channelId());
            }
            updateTicket.setString(2, metadata.clientName());
            updateTicket.setString(3, metadata.clientRoleId());
            updateTicket.setString(4, metadata.type().name());
            updateTicket.setInt(5, metadata.state().ordinal());
            updateTicket.setInt(6, metadata.ticketId());
            updateTicket.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public TicketMetadata getTicket(int ticketId) {
        checkConnection();
        try (PreparedStatement getTicket = connection.prepareStatement(
                "SELECT `id`, `guild_id`, `channel_id`, `created_at`, `ticket_number`, `client_id`, `client_name`, `client_role_id`, `type`, `state` FROM `ticket_metadata` WHERE `id`=?;")) {
            getTicket.setInt(1, ticketId);

            try (ResultSet rs = getTicket.executeQuery()) {
                if (rs.next()) {
                    return readTicket(rs);
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public TicketMetadata getTicketByChannel(long channelId) {
        checkConnection();
        try (PreparedStatement getTicket = connection.prepareStatement(
                "SELECT `id`, `guild_id`, `channel_id`, `created_at`, `ticket_number`, `client_id`, `client_name`, `client_role_id`, `type`, `state` FROM `ticket_metadata` WHERE `channel_id`=?;")) {
            getTicket.setLong(1, channelId);

            try (ResultSet rs = getTicket.executeQuery()) {
                if (rs.next()) {
                    return readTicket(rs);
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public List<TicketMetadata> getTickets(long guildId) {
        checkConnection();
        List<TicketMetadata> tickets = new ArrayList<>();
        try (PreparedStatement getTickets = connection.prepareStatement(
                "SELECT `id`, `guild_id`, `channel_id`, `created_at`, `ticket_number`, `client_id`, `client_name`, `client_role_id`, `type`, `state` FROM `ticket_metadata` WHERE `guild_id`=? ORDER BY `created_at` ASC;")) {
            getTickets.setLong(1, guildId);

            try (ResultSet rs = getTickets.executeQuery()) {
                while (rs.next()) {
                    tickets.add(readTicket(rs));
                }
            }
        } catch (SQLException ignored) { }

        return tickets;
    }

    @Override
    public void addTicketLog(int ticketId, String line) {
        checkConnection();
        try (PreparedStatement addLog = connection.prepareStatement(
                "INSERT INTO `ticket_log` (`ticket_id`, `time`, `line`) VALUES (?, ?, ?);")) {
            addLog.setInt(1, ticketId);
            addLog.setLong(2, Instant.now().getEpochSecond());
            addLog.setString(3, line);
            addLog.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<TicketLogEntry> getTicketLog(int ticketId) {
        checkConnection();
        List<TicketLogEntry> log = new ArrayList<>();
        try (PreparedStatement getLog = connection.prepareStatement(
                "SELECT `time`, `line` FROM `ticket_log` WHERE `ticket_id`=? ORDER BY `time` ASC;")) {
            getLog.setInt(1, ticketId);

            try (ResultSet rs = getLog.executeQuery()) {
                while (rs.next()) {
                    log.add(new TicketLogEntry(ticketId, Instant.ofEpochSecond(rs.getLong("time")), rs.getString("line")));
                }
            }
        } catch (SQLException ignored) { }

        return log;
    }

    protected TicketMetadata readTicket(ResultSet rs) throws SQLException {
        long channelId = rs.getLong("channel_id");
        Long channel = rs.wasNull() ? null : channelId;

        return new TicketMetadata(
                rs.getInt("id"),
                rs.getLong("guild_id"),
                channel,
                Instant.ofEpochSecond(rs.getLong("created_at")),
                rs.getString("ticket_number"),
                rs.getString("client_id"),
                rs.getString("client_name"),
                rs.getString("client_role_id"),
                TicketType.getFromName(rs.getString("type")),
                TicketState.values()[rs.getInt("state")]
        );
    }

    @Override
    public void saveTicketMessage(TicketChatMessage message) {
        checkConnection();
        try (PreparedStatement saveMessage = connection.prepareStatement(
                "INSERT INTO `ticket_messages` (`ticket_id`, `message_id`, `author_id`, `author_name`, `author_avatar_url`, `content`, `attachments`, `sent_at`, `edited`, `deleted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `author_name`=?, `author_avatar_url`=?, `content`=?, `attachments`=?, `edited`=?, `deleted`=?;")) {
            saveMessage.setInt(1, message.ticketId());
            saveMessage.setLong(2, message.messageId());
            saveMessage.setLong(3, message.authorId());
            saveMessage.setString(4, message.authorName());
            saveMessage.setString(5, message.authorAvatarUrl());
            saveMessage.setString(6, message.content());
            String attachments = GSON.toJson(message.attachments());
            saveMessage.setString(7, attachments);
            saveMessage.setLong(8, message.sentAt().getEpochSecond());
            saveMessage.setInt(9, message.edited() ? 1 : 0);
            saveMessage.setInt(10, message.deleted() ? 1 : 0);
            saveMessage.setString(11, message.authorName());
            saveMessage.setString(12, message.authorAvatarUrl());
            saveMessage.setString(13, message.content());
            saveMessage.setString(14, attachments);
            saveMessage.setInt(15, message.edited() ? 1 : 0);
            saveMessage.setInt(16, message.deleted() ? 1 : 0);
            saveMessage.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void markTicketMessageDeleted(int ticketId, long messageId) {
        checkConnection();
        try (PreparedStatement markDeleted = connection.prepareStatement(
                "UPDATE `ticket_messages` SET `deleted`=1 WHERE `ticket_id`=? AND `message_id`=?;")) {
            markDeleted.setInt(1, ticketId);
            markDeleted.setLong(2, messageId);
            markDeleted.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<TicketChatMessage> getTicketMessages(int ticketId) {
        checkConnection();
        List<TicketChatMessage> messages = new ArrayList<>();
        try (PreparedStatement getMessages = connection.prepareStatement(
                "SELECT `message_id`, `author_id`, `author_name`, `author_avatar_url`, `content`, `attachments`, `sent_at`, `edited`, `deleted` FROM `ticket_messages` WHERE `ticket_id`=? ORDER BY `sent_at` ASC;")) {
            getMessages.setInt(1, ticketId);

            try (ResultSet rs = getMessages.executeQuery()) {
                while (rs.next()) {
                    messages.add(readTicketMessage(ticketId, rs));
                }
            }
        } catch (SQLException ignored) { }

        return messages;
    }

    @Override
    public int addBadImage(BadImage image) {
        checkConnection();
        try (PreparedStatement addImg = connection.prepareStatement(
                "INSERT INTO `bad_images` (`similarity_hash`, `b64_source`) VALUES (?, ?);")) {
            addImg.setString(1, image.similarityHash());
            addImg.setString(2, image.b64SourceImg());
            addImg.executeUpdate();

            try (ResultSet rs = addImg.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) { }

        return -1;
    }

    @Override
    public void removeBadImage(int id) {
        checkConnection();
        try (PreparedStatement removeImg = connection.prepareStatement("DELETE FROM `bad_images` WHERE `id`=?;")) {
            removeImg.setInt(1, id);
            removeImg.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<BadImage> getBadImages() {
        checkConnection();
        List<BadImage> images = new ArrayList<>();
        try (PreparedStatement getImages = connection.prepareStatement(
                "SELECT `similarity_hash`, `b64_source` FROM `bad_images`;")) {

            try (ResultSet rs = getImages.executeQuery()) {
                while (rs.next()) {
                    images.add(new BadImage(rs.getString("similarity_hash"), rs.getString("b64_source")));
                }
            }
        } catch (SQLException ignored) { }

        return images;
    }

    protected TicketChatMessage readTicketMessage(int ticketId, ResultSet rs) throws SQLException {
        List<String> attachments = GSON.fromJson(rs.getString("attachments"), STRING_LIST_TYPE);

        return new TicketChatMessage(
                ticketId,
                rs.getLong("message_id"),
                rs.getLong("author_id"),
                rs.getString("author_name"),
                rs.getString("author_avatar_url"),
                rs.getString("content"),
                attachments == null ? List.of() : attachments,
                Instant.ofEpochSecond(rs.getLong("sent_at")),
                rs.getInt("edited") != 0,
                rs.getInt("deleted") != 0
        );
    }
}
