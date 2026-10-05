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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.permissions.UserRank;
import org.geysermc.discordbot.util.PropertiesManager;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SqliteStorageManager extends MySQLStorageManager {

    @Override
    public void setupStorage() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + PropertiesManager.getDatabase());

        Statement createTables = connection.createStatement();
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `preferences` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `server` INTEGER NOT NULL, `key` VARCHAR(32), `value` TEXT, CONSTRAINT `pref_constraint` UNIQUE (`server`,`key`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `persistent_roles` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `server` INTEGER NOT NULL, `user` INTEGER NOT NULL, `role` INTEGER NOT NULL, CONSTRAINT `role_constraint` UNIQUE (`server`,`user`,`role`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `mod_log` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `server` INTEGER NOT NULL, `time` INTEGER NOT NULL, `user` INTEGER NOT NULL, `action` VARCHAR(32) NOT NULL, `target` INTEGER NOT NULL, `reason` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `levels` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `server` INTEGER NOT NULL, `user` INTEGER NOT NULL, `level` INT NOT NULL, `xp` INT NOT NULL, `messages` INT NOT NULL, CONSTRAINT `level_constraint` UNIQUE (`server`,`user`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `slow_mode` (`channel` INTEGER NOT NULL PRIMARY KEY, `server` INTEGER NOT NULL, `delay` INT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `tickets` (`client_id` VARCHAR(32) NOT NULL PRIMARY KEY, `id` INTEGER NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `appeals` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `action_time` INTEGER NOT NULL, `time` INTEGER NOT NULL, `user` INTEGER NOT NULL, `message` TEXT NOT NULL, `state` INTEGER NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_permissions` (`user` INTEGER NOT NULL PRIMARY KEY, `permissions` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_role_permissions` (`guild_snowflake` TEXT NOT NULL PRIMARY KEY, `permissions` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `user_ranks` (`user` INTEGER NOT NULL PRIMARY KEY, `ranks` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `member_ranks` (`guild_member` TEXT NOT NULL PRIMARY KEY, `ranks` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `auth_sessions` (`token_hash` VARCHAR(64) NOT NULL PRIMARY KEY, `user` INTEGER NOT NULL, `attributes` TEXT NOT NULL, `expires_at` INTEGER NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `bans` (`user` INTEGER NOT NULL PRIMARY KEY, `issued_on` INTEGER NOT NULL, `expires_at` INTEGER NOT NULL, `reason` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_metadata` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `guild_id` INTEGER NOT NULL, `channel_id` INTEGER, `created_at` INTEGER NOT NULL, `ticket_number` VARCHAR(32) NOT NULL, `client_id` VARCHAR(64) NOT NULL, `client_name` VARCHAR(255) NOT NULL, `client_role_id` VARCHAR(32) NOT NULL, `type` VARCHAR(32) NOT NULL, `state` INTEGER NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_log` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `ticket_id` INTEGER NOT NULL, `time` INTEGER NOT NULL, `line` TEXT NOT NULL);");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `ticket_messages` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `ticket_id` INTEGER NOT NULL, `message_id` INTEGER NOT NULL, `author_id` INTEGER NOT NULL, `author_name` VARCHAR(255) NOT NULL, `author_avatar_url` TEXT, `content` TEXT NOT NULL, `attachments` TEXT NOT NULL, `sent_at` INTEGER NOT NULL, `edited` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, CONSTRAINT `ticket_message_constraint` UNIQUE (`ticket_id`,`message_id`));");
        createTables.executeUpdate("CREATE TABLE IF NOT EXISTS `bad_images` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `similarity_hash` TEXT NOT NULL, `b64_source` TEXT NOT NULL);");
        createTables.close();
    }

    @Override
    protected void checkConnection() {
        // No need in SQLite
    }

    @Override
    public void closeStorage() {
        try {
            connection.close();
        } catch (SQLException ignored) { }
    }

    @Override
    public String getServerPreference(long serverID, String preference) {
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
        try (PreparedStatement updatePreferenceValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `preferences` (`server`, `key`, `value`) VALUES (?, ?, ?);")) {
            updatePreferenceValue.setLong(1, serverID);
            updatePreferenceValue.setString(2, preference);
            updatePreferenceValue.setString(3, value);
            updatePreferenceValue.executeUpdate();
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    public void addPersistentRole(Member member, Role role) {
        try (PreparedStatement addPersistentRole = connection.prepareStatement(
                "INSERT OR REPLACE INTO `persistent_roles` (`server`, `user`, `role`) VALUES (?, ?, ?);")) {
            addPersistentRole.setLong(1, member.getGuild().getIdLong());
            addPersistentRole.setLong(2, member.getIdLong());
            addPersistentRole.setLong(3, role.getIdLong());
            addPersistentRole.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void removePersistentRole(Member member, Role role) {
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
        try {
            long time = Instant.now().getEpochSecond();

            try (PreparedStatement addLogEntry = connection.prepareStatement(
                    "INSERT OR REPLACE INTO `mod_log` (`server`, `time`, `user`, `action`, `target`, `reason`) VALUES (?, ?, ?, ?, ?, ?);")) {
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
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "UPDATE `mod_log` SET `reason`=? WHERE `id`=?;")) {
            updateLevelValue.setString(1, reason);
            updateLevelValue.setInt(2, id);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public LevelInfo getLevel(Member user) {
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
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `levels` (`server`, `user`, `level`, `xp`, `messages`) VALUES (?, ?, ?, ?, ?);")) {
            updateLevelValue.setLong(1, user.getGuild().getIdLong());
            updateLevelValue.setLong(2, user.getIdLong());
            updateLevelValue.setInt(3, levelInfo.getLevel());
            updateLevelValue.setInt(4, levelInfo.getXp());
            updateLevelValue.setInt(5, levelInfo.getMessages());
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public List<LevelInfo> getLevels(long guild) {
        List<LevelInfo> levels = new ArrayList<>();
        try (PreparedStatement getLevelValue = connection.prepareStatement(
                "SELECT `user`, `level`, `xp`, `messages` FROM `levels` WHERE `server`=? ORDER BY xp DESC LIMIT 100;")) {
            getLevelValue.setLong(1, guild);

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
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `slow_mode` (`channel`, `server`, `delay`) VALUES (?, ?, ?);")) {
            updateLevelValue.setLong(1, channel.getIdLong());
            updateLevelValue.setLong(2, channel.getGuild().getIdLong());
            updateLevelValue.setInt(3, delay);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public int getAndIncrementTicketId(String clientId) {
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
            if (id == -1) id = 0;

            try (PreparedStatement setCurrentIdEntry = connection.prepareStatement(
                    "INSERT OR REPLACE INTO `tickets` (`client_id`, `id`) VALUES (?, ?);")) {
                setCurrentIdEntry.setString(1, clientId);
                setCurrentIdEntry.setInt(2, id + 1);
                setCurrentIdEntry.execute();
            }
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }

        return id;
    }

    @Override
    public void saveTicketMessage(TicketChatMessage message) {
        try (PreparedStatement saveMessage = connection.prepareStatement(
                "INSERT OR REPLACE INTO `ticket_messages` (`ticket_id`, `message_id`, `author_id`, `author_name`, `author_avatar_url`, `content`, `attachments`, `sent_at`, `edited`, `deleted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            saveMessage.setInt(1, message.ticketId());
            saveMessage.setLong(2, message.messageId());
            saveMessage.setLong(3, message.authorId());
            saveMessage.setString(4, message.authorName());
            saveMessage.setString(5, message.authorAvatarUrl());
            saveMessage.setString(6, message.content());
            saveMessage.setString(7, new Gson().toJson(message.attachments()));
            saveMessage.setLong(8, message.sentAt().getEpochSecond());
            saveMessage.setInt(9, message.edited() ? 1 : 0);
            saveMessage.setInt(10, message.deleted() ? 1 : 0);
            saveMessage.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setMemberPermissions(Member member, MemberRolePermissions[] permissions) {
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `member_role_permissions` (`guild_snowflake`, `permissions`) VALUES (?, ?);")) {
            String value = Arrays.stream(permissions).map(MemberRolePermissions::name).collect(Collectors.joining(","));
            updateLevelValue.setString(1, member.getGuild().getId() + "_" + member.getId());
            updateLevelValue.setString(2, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setRolePermissions(Role role, MemberRolePermissions[] permissions) {
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `member_role_permissions` (`guild_snowflake`, `permissions`) VALUES (?, ?);")) {
            String value = Arrays.stream(permissions).map(MemberRolePermissions::name).collect(Collectors.joining(","));
            updateLevelValue.setString(1, role.getGuild().getId() + "_" + role.getId());
            updateLevelValue.setString(2, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setUserPermissions(User user, UserPermissions[] permissions) {
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `user_permissions` (`user`, `permissions`) VALUES (?, ?);")) {
            String value = Arrays.stream(permissions).map(UserPermissions::name).collect(Collectors.joining(","));
            updateLevelValue.setLong(1, user.getIdLong());
            updateLevelValue.setString(2, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setUserRanks(User user, UserRank[] ranks) {
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `user_ranks` (`user`, `ranks`) VALUES (?, ?);")) {
            String value = Arrays.stream(ranks).map(UserRank::name).collect(Collectors.joining(","));
            updateLevelValue.setLong(1, user.getIdLong());
            updateLevelValue.setString(2, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void setMemberRanks(Member member, MemberRank[] ranks) {
        try (PreparedStatement updateLevelValue = connection.prepareStatement(
                "INSERT OR REPLACE INTO `member_ranks` (`guild_member`, `ranks`) VALUES (?, ?);")) {
            String value = Arrays.stream(ranks).map(MemberRank::name).collect(Collectors.joining(","));
            updateLevelValue.setString(1, member.getGuild().getId() + "_" + member.getId());
            updateLevelValue.setString(2, value);
            updateLevelValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void saveAuthSession(String tokenHash, long discordUserId, String attributesJson, long expiresAt) {
        try (PreparedStatement saveAuthSession = connection.prepareStatement(
                "INSERT OR REPLACE INTO `auth_sessions` (`token_hash`, `user`, `attributes`, `expires_at`) VALUES (?, ?, ?, ?);")) {
            saveAuthSession.setString(1, tokenHash);
            saveAuthSession.setLong(2, discordUserId);
            saveAuthSession.setString(3, attributesJson);
            saveAuthSession.setLong(4, expiresAt);
            saveAuthSession.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public AuthSession getAuthSession(String tokenHash) {
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
        try (PreparedStatement deleteAuthSession = connection.prepareStatement("DELETE FROM `auth_sessions` WHERE `token_hash`=?;")) {
            deleteAuthSession.setString(1, tokenHash);
            deleteAuthSession.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public int addAppeal(UserSnowflake user, String message) {
        try {
            long time = Instant.now().getEpochSecond();

            try (PreparedStatement addAppealEntry = connection.prepareStatement(
                    "INSERT OR REPLACE INTO `appeals` (`user`, `action_time`, `time`, `message`, `state`) VALUES (?, ?, ?, ?, ?);")) {
                addAppealEntry.setLong(1, user.getIdLong());
                addAppealEntry.setLong(2, -1);
                addAppealEntry.setLong(3, time);
                addAppealEntry.setString(4, message);
                addAppealEntry.setInt(5, Appeal.State.PENDING.ordinal());
                addAppealEntry.executeUpdate();
            }

            try (PreparedStatement getAppealEntry = connection.prepareStatement(
                    "SELECT `id` FROM `appeals` WHERE `user`=? AND `action_time`=? AND `time`=? AND `message`=? AND `state`=? LIMIT 1;")) {
                getAppealEntry.setLong(1, user.getIdLong());
                getAppealEntry.setLong(2, -1);
                getAppealEntry.setLong(3, time);
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
        try (PreparedStatement getLogEntry = connection.prepareStatement(
                "SELECT `action_time`, `time`, `user`, `message`, `state` FROM `appeals` WHERE `id`=?;")) {
            getLogEntry.setInt(1, id);

            try (ResultSet rs = getLogEntry.executeQuery()) {
                if (rs.next()) {
                    long actionTimeEpoch = rs.getLong("action_time");
                    Instant actionTime = Instant.ofEpochSecond(actionTimeEpoch);
                    Instant time = Instant.ofEpochSecond(rs.getLong("time"));
                    UserSnowflake user = User.fromId(rs.getLong("user"));

                    return new Appeal(id, user, time, actionTime, rs.getString("message"), Appeal.State.values()[rs.getInt("state")]);
                }
            }
        } catch (SQLException ignored) { }

        return null;
    }

    @Override
    public void updateAppeal(int id, Appeal.State state) {
        try (PreparedStatement updateAppealValue = connection.prepareStatement(
                "UPDATE `appeals` SET `state`=?, `action_time`=? WHERE `id`=?;")) {
            updateAppealValue.setInt(1, state.ordinal());
            updateAppealValue.setLong(2, Instant.now().getEpochSecond());
            updateAppealValue.setInt(3, id);
            updateAppealValue.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public MemberRolePermissions[] getMemberPermissions(Member member) {
        try (PreparedStatement getPermissions = connection.prepareStatement(
                "SELECT `permissions` FROM `member_role_permissions` WHERE `guild_snowflake`=?;")) {
            getPermissions.setString(1, member.getGuild().getId() + "_" + member.getId());

            try (ResultSet rs = getPermissions.executeQuery()) {
                if (rs.next()) {
                    String permissions = rs.getString("permissions");
                    if (permissions.isEmpty()) return new MemberRolePermissions[0];
                    return Arrays.stream(permissions.split(",")).map(MemberRolePermissions::valueOf).toArray(MemberRolePermissions[]::new);
                }
            }
        } catch (SQLException ignored) { }

        return new MemberRolePermissions[0];
    }

    @Override
    public MemberRolePermissions[] getRolePermissions(Role role) {
        try (PreparedStatement getPermissions = connection.prepareStatement(
                "SELECT `permissions` FROM `member_role_permissions` WHERE `guild_snowflake`=?;")) {
            getPermissions.setString(1, role.getGuild().getId() + "_" + role.getId());

            try (ResultSet rs = getPermissions.executeQuery()) {
                if (rs.next()) {
                    String permissions = rs.getString("permissions");
                    if (permissions.isEmpty()) return new MemberRolePermissions[0];
                    return Arrays.stream(permissions.split(",")).map(MemberRolePermissions::valueOf).toArray(MemberRolePermissions[]::new);
                }
            }
        } catch (SQLException ignored) { }

        return new MemberRolePermissions[0];
    }

    @Override
    public UserPermissions[] getUserPermissions(User user) {
        try (PreparedStatement getPermissions = connection.prepareStatement(
                "SELECT `permissions` FROM `user_permissions` WHERE `user`=?;")) {
            getPermissions.setLong(1, user.getIdLong());

            try (ResultSet rs = getPermissions.executeQuery()) {
                if (rs.next()) {
                    String permissions = rs.getString("permissions");
                    if (permissions.isEmpty()) return new UserPermissions[0];
                    return Arrays.stream(permissions.split(",")).map(UserPermissions::valueOf).toArray(UserPermissions[]::new);
                }
            }
        } catch (SQLException ignored) { }

        return new UserPermissions[0];
    }

    @Override
    public void addBan(BanData data) {
        try (PreparedStatement addBan = connection.prepareStatement(
                "INSERT OR REPLACE INTO `bans` (`user`, `issued_on`, `expires_at`, `reason`) VALUES (?, ?, ?, ?);")) {
            addBan.setLong(1, data.userId());
            addBan.setLong(2, data.issuedOn().getEpochSecond());
            addBan.setLong(3, data.expiryTime() == null ? -1 : data.expiryTime().getEpochSecond());
            addBan.setString(4, data.reason());
            addBan.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public void removeBan(long userId) {
        try (PreparedStatement removeBan = connection.prepareStatement("DELETE FROM `bans` WHERE `user`=?;")) {
            removeBan.setLong(1, userId);
            removeBan.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public BanData getBan(long userId) {
        try (PreparedStatement getBan = connection.prepareStatement(
                "SELECT `issued_on`, `expires_at`, `reason` FROM `bans` WHERE `user`=?;")) {
            getBan.setLong(1, userId);

            try (ResultSet rs = getBan.executeQuery()) {
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
        List<BanData> bans = new ArrayList<>();
        try (PreparedStatement getBans = connection.prepareStatement("SELECT * FROM `bans`;")) {
            try (ResultSet rs = getBans.executeQuery()) {
                while (rs.next()) {
                    long time = rs.getLong("expires_at");
                    bans.add(new BanData(rs.getLong("user"), Instant.ofEpochSecond(rs.getLong("issued_on")), time == -1 ? null : Instant.ofEpochSecond(time), rs.getString("reason")));
                }
            }
        } catch (SQLException ignored) { }

        return bans;
    }
}
