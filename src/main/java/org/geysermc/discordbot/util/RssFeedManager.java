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

package org.geysermc.discordbot.util;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RssFeedManager {

    private static final Map<String, List<String>> RSS_POSTS = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(RssFeedManager.class);

    public static void setup() {
        GeyserBot.getGeneralThreadPool().schedule(RssFeedManager::initialCache, 5, TimeUnit.SECONDS);
        GeyserBot.getGeneralThreadPool().scheduleAtFixedRate(RssFeedManager::checkFeeds, 60, 60, TimeUnit.SECONDS);
    }

    private static void initialCache() {
        for (Guild guild : GeyserBot.getJDA().getGuilds()) {
            Map<String, TextChannel> urls = getMap(guild);
            for (Map.Entry<String, TextChannel> feed : urls.entrySet()) {
                cacheFeed(feed.getKey(), feed.getValue());
            }
        }
    }

    /**
     * Cache a feed url against a given channel for checking later
     *
     * @param url URL of the rss feed
     * @param channel Channel id to cache against
     */
    public static void cacheFeed(String url, TextChannel channel) {
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feedData = input.build(new XmlReader(new URL(url)));

            List<String> knownPosts = new ArrayList<>();
            for (SyndEntry entry : feedData.getEntries()) {
                knownPosts.add(entry.getUri());
            }

            RSS_POSTS.put(channel.getId(), knownPosts);
        } catch (FeedException | IOException e) {
            GeyserBot.LOGGER.error("Error caching rss feed on: " + url, e);
        }
    }

    /**
     * Check for new rss posts and notify the channel of the change
     */
    private static void checkFeeds() {
        for (Guild guild : GeyserBot.getJDA().getGuilds()) {
            Map<String, TextChannel> urls = getMap(guild);
            for (Map.Entry<String, TextChannel> feed : urls.entrySet()) {
                try {
                    // If we don't have the feed cached then cache it and continue
                    if (!RSS_POSTS.containsKey(feed.getKey())) {
                        cacheFeed(feed.getKey(), feed.getValue());
                        continue;
                    }

                    // Get the feed data
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feedData = input.build(new XmlReader(new URL(feed.getKey())));

                    // Get the known posts so we don't
                    List<String> knownPosts = RSS_POSTS.get(feed.getValue().getId());

                    // See if there are any new posts
                    for (SyndEntry entry : feedData.getEntries()) {
                        if (knownPosts.contains(entry.getUri())) continue;

                        feed.getValue().sendMessageEmbeds(new EmbedBuilder()
                                .setTitle(entry.getTitle(), entry.getLink())
                                .setAuthor(entry.getAuthor())
                                .setTimestamp(entry.getPublishedDate().toInstant())
                                .setDescription(entry.getDescription().getValue())
                                .setColor(BotColors.NEUTRAL.getColor())
                                .build()).queue();

                        knownPosts.add(entry.getUri());
                    }

                    // Push any changes
                    RSS_POSTS.put(feed.getValue().getId(), knownPosts);
                } catch (FeedException | IOException e) {
                    GeyserBot.LOGGER.error("Error checking rss feed on: " + feed.getValue(), e);
                }
            }
        }
    }

    private static Map<String, TextChannel> getMap(Guild guild) {
        Map<String, TextChannel> map = new HashMap<>();
        List<String> urls = ServerConfiguration.RSS_FEED_LINKS.get(guild);
        List<TextChannel> channels = ServerConfiguration.RSS_FEED_CHANNELS.get(guild);

        if (urls == null && channels == null) return map;

        if (urls == null) {
            log.error("Channels were specified for {},'s RSS feeds, but not URLs, this should be addressed,", guild.getName());
            return map;
        }

        if (channels == null) {
            log.error("URLs were specified for {},'s RSS feeds, but not channels, this should be addressed,", guild.getName());
            return map;
        }

        if (urls.size() != channels.size()) {
            log.error("URL and Channel list sizes do not match for {}'s RSS feeds, this should be addressed!", guild.getName());
            return map;
        }

        int i = 0;
        for (String url : urls) {
            map.put(url, channels.get(i));
            i++;
        }

        return map;
    }
}
