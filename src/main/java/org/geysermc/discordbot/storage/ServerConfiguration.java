/*
 * Copyright (c) 2026 Open Collaboration. https://opencollaboration.dev, GeyserMC. https://geysermc.org
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
 * @author OpenCollab, GeyserMC
 * @link https://github.com/OpenCollaboration/OpenCollabDiscordBot
 */

package org.geysermc.discordbot.storage;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.geysermc.discordbot.GeyserBot;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ServerConfiguration {
    private static final List<Configuration<?>> ALL_CONFIGS = new ArrayList<>();

    public static List<Configuration<?>> getAllConfigs() {
        return Collections.unmodifiableList(ALL_CONFIGS);
    }

    public static Configuration<?> get(String key) {
        return ALL_CONFIGS.stream().filter(c -> c.getKey().equals(key)).findFirst().orElse(null);
    }

    public static final Configuration<List<String>> ALLOWED_INVITES = registerStrings("allowed-invites", "Allowed Invites");
    public static final Configuration<Role> AUDIT_ROLE = registerRole("audit-role", "Audit Role");
    public static final Configuration<List<String>> BLOCKED_DOMAINS = registerStrings("banned-domains", "Banned Domains");
    public static final Configuration<List<String>> BLOCKED_IPS = registerStrings("banned-ips", "Banned IPs");
    public static final Configuration<List<String>> CHECK_DOMAINS = registerStrings("check-domains", "Check Domains");
    public static final Configuration<List<String>> CONVERT_EXTENSIONS = registerStrings("convert-extensions", "Convert Extensions");
    public static final Configuration<List<Role>> CLIENT_ROLES = registerRoles("client-roles", "Client Roles");
    public static final Configuration<TextChannel> DONATIONS_FEEDS_CHANNEL = registerChannel("donations-feeds-channel", "Donation Feeds Channel", TextChannel.class);
    public static final Configuration<List<TextChannel>> DONT_CHECK_ERROR = registerChannels("dont-check-error", "Don't Check Error", TextChannel.class);
    public static final Configuration<List<TextChannel>> DONT_LEVEL = registerChannels("dont-level", "Don't Level", TextChannel.class);
    public static final Configuration<List<TextChannel>> DONT_LOG = registerChannels("dont-log", "Don't Log", TextChannel.class);
    public static final Configuration<TextChannel> EXPLOIT_REPORTS_NOTIFY_CHANNEL = registerChannel("exploit-reports-notify-channel", "Exploit Reports Notify Channel", TextChannel.class);
    public static final Configuration<ForumChannel> FORUM_CHANNEL = registerChannel("forum-channel", "Forum Channel", ForumChannel.class);
    public static final Configuration<TextChannel> HEALTH_CHECK_CHANNEL = registerChannel("health-check-channel", "Health Check Channel", TextChannel.class);
    public static final Configuration<List<String>> HEALTH_CHECK_URLS = registerStrings("health-check-urls", "Health Check URLs");
    public static final Configuration<TextChannel> HONEY_POT_CHANNEL = registerChannel("honey-pot-channel", "Honey Pot Channel", TextChannel.class);
    public static final Configuration<TextChannel> LOG_CHANNEL = registerChannel("log-channel", "Log Channel", TextChannel.class);
    public static final Configuration<TextChannel> MOD_CHANNEL = registerChannel("moderation-channel", "Moderation Channel", TextChannel.class);
    public static final Configuration<Role> MOD_ROLE = registerRole("moderation-role", "Moderation Role");
    public static final Configuration<ForumChannel> PREVIEW_CHANNEL = registerChannel("preview-channel", "Preview Channel", ForumChannel.class);
    public static final Configuration<NewsChannel> PREVIEW_FEEDS_CHANNEL = registerChannel("preview-feeds-channel", "Preview Feeds Channel", NewsChannel.class);
    public static final Configuration<String> PUNISHMENT_MESSAGE = registerString("punishment-message", "Punishment Message");
    public static final Configuration<List<Role>> RANK_ROLES = registerRoles("rank-roles", "Rank Roles");
    public static final Configuration<List<TextChannel>> RSS_FEED_CHANNELS = registerChannels("rss-feeds-channels", "RSS Feeds Channels", TextChannel.class);
    public static final Configuration<List<String>> RSS_FEED_LINKS = registerStrings("rss-feeds-links", "RSS Feeds Links");
    public static final Configuration<String> TAG_REPO = registerString("tag-repo", "Tag Repo");
    public static final Configuration<List<Role>> TICKET_ALLOWED_ROLES = registerRoles("ticket-allowed-roles", "Ticket Allowed Roles");
    public static final Configuration<TextChannel> TICKET_ARCHIVE_CHANNEL = registerChannel("ticket-archive-channel", "Ticket Archive Channel", TextChannel.class);
    public static final Configuration<List<Category>> TICKET_CATEGORIES = registerChannels("ticket-categories", "Ticket Categories", Category.class);
    public static final Configuration<List<Role>> TICKET_PING_ROLES = registerRoles("ticket-ping-roles", "Ticket Ping Roles");

    private static <T extends GuildChannel> Configuration<T> registerChannel(String key, String name, Class<T> type) {
        Configuration<T> config = new Configuration<>() {
            @Override
            public T get(Guild guild) {
                String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
                if (id == null) return null;
                return guild.getChannelById(type, id);
            }

            @Override
            public void set(Guild guild, T val) {
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, val == null ? null : val.getId());
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public String getDisplay(Guild guild) {
                T channel = get(guild);
                return channel == null ? "null" : channel.getAsMention();
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    private static <T extends GuildChannel> Configuration<List<T>> registerChannels(String key, String name, Class<T> type) {
        Configuration<List<T>> config = new Configuration<>() {
            @Override
            public List<T> get(Guild guild) {
                String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
                if (id == null) return null;
                List<T> channels = new ArrayList<>();
                for (String part : id.split(",")) {
                    T channel = guild.getChannelById(type, part);
                    if (channel != null) channels.add(channel);
                }
                return channels;
            }

            @Override
            public void set(Guild guild, List<T> val) {
                String storeValue = val == null ? null : val.stream()
                                                         .filter(Objects::nonNull)
                                                         .map(T::getId)
                                                         .collect(Collectors.joining(","));
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, (storeValue == null || storeValue.isEmpty()) ? null : storeValue);
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public ConfigType getConfigType() {
                return ConfigType.MULTIPLE;
            }

            @Override
            public String getDisplay(Guild guild) {
                List<T> val = get(guild);
                String displayValue = val == null ? null : val.stream()
                                                         .filter(Objects::nonNull)
                                                         .map(T::getAsMention)
                                                         .collect(Collectors.joining(", "));
                return displayValue == null ? "null" : displayValue;
            }

            @Override
            public boolean isOrContains(Guild guild, Object val) {
                List<T> list = get(guild);
                if (list == null) return false;
                return list.contains(val);
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    private static Configuration<Role> registerRole(String key, String name) {
        Configuration<Role> config = new Configuration<>() {
            @Override
            public Role get(Guild guild) {
                String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
                if (id == null) return null;
                return guild.getRoleById(id);
            }

            @Override
            public void set(Guild guild, Role val) {
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, val == null ? null : val.getId());
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<Role> getType() {
                return Role.class;
            }

            @Override
            public String getDisplay(Guild guild) {
                Role role = get(guild);
                return role == null ? "null" : role.getAsMention();
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    private static Configuration<List<Role>> registerRoles(String key, String name) {
        Configuration<List<Role>> config = new Configuration<>() {
            @Override
            public List<Role> get(Guild guild) {
                String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
                if (id == null) return null;
                List<Role> roles = new ArrayList<>();
                for (String part : id.split(",")) {
                    Role role = guild.getRoleById(part);
                    if (role != null) roles.add(role);
                }
                return roles;
            }

            @Override
            public void set(Guild guild, List<Role> val) {
                String storeValue = val == null ? null : val.stream()
                        .filter(Objects::nonNull)
                        .map(Role::getId)
                        .collect(Collectors.joining(","));
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, (storeValue == null || storeValue.isEmpty()) ? null : storeValue);
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<Role> getType() {
                return Role.class;
            }

            @Override
            public ConfigType getConfigType() {
                return ConfigType.MULTIPLE;
            }

            @Override
            public String getDisplay(Guild guild) {
                List<Role> val = get(guild);
                String displayValue = val == null ? null : val.stream()
                                                           .filter(Objects::nonNull)
                                                           .map(Role::getAsMention)
                                                           .collect(Collectors.joining(", "));
                return displayValue == null ? "null" : displayValue;
            }

            @Override
            public boolean isOrContains(Guild guild, Object val) {
                List<Role> list = get(guild);
                if (list == null) return false;
                return list.contains(val);
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    private static Configuration<String> registerString(String key, String name) {
        Configuration<String> config = new Configuration<>() {
            @Override
            public String get(Guild guild) {
                return GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
            }

            @Override
            public void set(Guild guild, String val) {
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, val);
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    private static Configuration<List<String>> registerStrings(String key, String name) {
        Configuration<List<String>> config = new Configuration<>() {
            @Override
            public List<String> get(Guild guild) {
                String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), key);
                if (id == null) return null;
                return List.of(id.split(","));
            }

            @Override
            public void set(Guild guild, List<String> val) {
                String storeValue = val == null ? null : val.stream()
                                                         .filter(Objects::nonNull)
                                                         .collect(Collectors.joining(","));
                GeyserBot.storageManager.setServerPreference(guild.getIdLong(), key, (storeValue == null || storeValue.isEmpty()) ? null : storeValue);
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }

            @Override
            public ConfigType getConfigType() {
                return ConfigType.MULTIPLE;
            }

            @Override
            public String getDisplay(Guild guild) {
                List<String> val = get(guild);
                String displayValue = val == null ? null : val.stream()
                                                           .filter(Objects::nonNull)
                                                           .collect(Collectors.joining(", "));
                return displayValue == null ? "null" : displayValue;
            }

            @Override
            public boolean isOrContains(Guild guild, Object val) {
                List<String> list = get(guild);
                if (list == null) return false;
                return list.contains(val);
            }
        };
        ALL_CONFIGS.add(config);
        return config;
    }

    public interface Configuration<T> {
        T get(Guild guild);

        void set(Guild guild, T val);

        String getKey();

        String getName();

        Class<?> getType();

        default ConfigType getConfigType() {
            return ConfigType.SINGLE;
        }

        default String getDisplay(Guild guild) {
            T val = get(guild);
            if (val == null) return "null";
            return val.toString();
        }

        default boolean isOrContains(Guild guild, Object val) {
            return val.equals(get(guild));
        }

        default boolean isOrContains(Channel channel) {
            if (channel instanceof GuildChannel guildChannel) {
                return isOrContains(guildChannel.getGuild(), channel);
            }
            return false;
        }

        default boolean isConfigured(Guild guild) {
            return get(guild) != null;
        }

        default void ifPresent(Guild guild, Consumer<T> handler) {
            T val = get(guild);
            if (val != null) handler.accept(val);
        }
    }

    public enum ConfigType {
        SINGLE,
        MULTIPLE
    }
}
