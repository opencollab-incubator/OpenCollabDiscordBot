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

package org.geysermc.discordbot.tag;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.PropertiesManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TagManager extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(TagManager.class);
    private final Map<Long, Map<String, Tag>> TAGS = new HashMap<>();

    public boolean refreshTags(Guild guild) throws IOException {
        String repoUrl = ServerConfiguration.TAG_REPO.get(guild);
        if (repoUrl == null) return false;

        log.info("[{} Tags] Fetching new tags from {}.", guild.getName(), repoUrl);
        Process gitProcess = Runtime.getRuntime().exec(new String[]{"git", "clone", repoUrl, "tags/" + guild.getId()});

        Thread t = new Thread(() -> {
            log.info("[{} Tags] Waiting for fetch to complete.", guild.getName());
            while (gitProcess.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("[{} Tags] Completed fetch! Loading tags...", guild.getName());

            try {
                Map<String, Tag> tags = fromPath(Path.of("tags", guild.getId()));
                TAGS.put(guild.getIdLong(), tags);

                FileUtils.deleteDirectory(new File("tags", guild.getId()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();

        return true;
    }

    public Tag getTag(Guild guild, String tagName) {
        Map<String, Tag> tags = TAGS.get(guild.getIdLong());
        if (tags == null || tags.isEmpty()) return null;

        return tags.get(tagName.toLowerCase());
    }

    public Map<String, Tag> getTags(Guild guild) {
        if (!TAGS.containsKey(guild.getIdLong())) return Map.of();
        else return TAGS.get(guild.getIdLong());
    }

    private Map<String, Tag> fromPath(Path path) throws IOException {
        Map<String, Tag> tags = new HashMap<>();

        if (Files.notExists(path)) return tags;

        if (Files.isDirectory(path)) {
            if (path.getFileName().toString().equals(".git")) return tags;
            Files.list(path).forEach(p -> {
                try {
                    tags.putAll(fromPath(p));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else if (Files.isRegularFile(path)) {
            String filename = path.getFileName().toString();
            if (!filename.endsWith(".tag")) {
                log.warn("{} is in the tag directory but is not a valid tag!", filename);
                return tags;
            }

            String name = filename.substring(0, filename.length() - 4);

            String data = Files.readString(path);

            String[] splitData = data.split("---", 2);
            if (splitData.length == 1) {
                log.warn("{} has no header, cannot load this tag!", name);
                return tags;
            }

            Map<String, String> headerData = new HashMap<>();
            String header = splitData[0];
            String rawContent = splitData[1].trim();
            List<Button> buttons = new ArrayList<>();
            List<String> aliases = new ArrayList<>();

            int lineNum = 1;
            for (String headerLine : header.split("\n")) {
                if (!headerLine.contains(":") && !headerLine.isBlank()) {
                    log.warn("Invalid header line at line {}.", lineNum);
                    continue;
                }

                String[] headerLineParts = headerLine.split(":", 2);

                switch (headerLineParts[0].trim()) {
                    case "type" -> {
                        headerData.put(headerLineParts[0].trim(), headerLineParts[1].trim().toLowerCase());
                    }
                    case "aliases" -> {
                        aliases.addAll(Arrays.stream(headerLineParts[1].split(",")).map(String::trim).toList());
                    }
                    case "button" -> {
                        String[] buttonData = headerLineParts[1].trim().replace("[", "").replace(")", "").split("]\\(");
                        buttons.add(Button.link(buttonData[1], buttonData[0]));
                    }
                    case "tagButton" -> {
                        String[] buttonData = headerLineParts[1].trim().split(",");
                        boolean ephermal = buttonData[3].equals("true");
                        buttons.add(Button.of(
                                ButtonStyle.fromKey(Integer.parseInt(buttonData[2])),
                                "tag-" + (ephermal ? "silent-" : "") + buttonData[1],
                                buttonData[0]
                        ));
                    }
                    default -> {
                        headerData.put(headerLineParts[0], headerLineParts[1].trim());
                    }
                }

                lineNum++;
            }

            Tag tag = switch (headerData.get("type")) {
                case "embed" -> new EmbedTag(
                        buttons, headerData.get("title"), rawContent,
                        headerData.get("color"), headerData.get("titleUrl"),
                        headerData.get("image")
                );
                case "text" -> new ContentTag(buttons, rawContent);
                default -> throw new IllegalStateException("Unexpected tag type: " + headerData.get("type"));
            };

            tags.put(name.toLowerCase(), tag);
            for (String alias : aliases) {
                tags.put(alias.toLowerCase(), tag);
            }
        }

        return tags;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {
            try {
                if (!refreshTags(guild)) {
                    log.warn("Unable to load tags of %s, the repo URL is invalid.".formatted(guild.getName()));
                }
            } catch (IOException e) {
                log.error("Error while refreshing tags for {}.", guild.getName(), e);
            }
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot()) return;

        Map<String, Tag> tags = TAGS.get(event.getGuild().getIdLong());

        if (tags == null || tags.isEmpty()) return;

        String rawContent = event.getMessage().getContentRaw();
        String prefix = PropertiesManager.getPrefix();
        String tagName = null;
        if (rawContent.startsWith(prefix.repeat(2))) {
            tagName = rawContent.substring(prefix.length() * 2);
        } else if (rawContent.startsWith(prefix + "tag ")) {
            tagName = rawContent.substring((prefix + "tag ").length());
        }

        if (tagName == null) return;

        tagName = tagName.split(" ")[0];

        if (tags.containsKey(tagName.toLowerCase())) {
            event.getMessage().reply(tags.get(tagName).create()).queue();
        } else {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setDescription("Unknown tag name!")
                    .setColor(BotColors.FAILURE.getColor())
                    .build()).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;
        String id = event.getInteraction().getCustomId();
        if (id.startsWith("tag-")) {
            String tagName;
            boolean ephermal = false;
            if (id.startsWith("tag-silent-")) {
                tagName = id.substring(11);
                ephermal = true;
            } else {
                tagName = id.substring(4);
            }

            Map<String, Tag> tags = TAGS.get(event.getGuild().getIdLong());

            if (tags == null || tags.isEmpty() || !tags.containsKey(tagName)) {
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("Unknown tag name!")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(ephermal).queue();
                return;
            }

            event.reply(tags.get(tagName).create()).setEphemeral(ephermal).queue();
        }
    }
}
