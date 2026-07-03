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

package org.geysermc.discordbot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.Arrays;

public class EchoCommand extends SlashCommand {
    public EchoCommand() {
        this.name = "echo";
        this.hidden = true;
        this.help = "Send a message as the bot.";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.children = new SlashCommand[] {
                new Text(),
                new Embed()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // no-op
    }

    public static class Text extends SlashCommand {
        public Text() {
            this.name = "text";
            this.help = "Send a text message as the bot.";

            this.options = Arrays.asList(
                    new OptionData(OptionType.STRING, "message", "The message to send", true),
                    new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message to", true)
                            .setChannelTypes(ChannelType.TEXT),
                    new OptionData(OptionType.STRING, "message_link", "The message to modify")
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String messageLink = event.optString("message_link");
            if (messageLink == null) {
                event.optMessageChannel("channel").sendMessage(event.optString("message")).queue();
                event.reply("Sent message.").setEphemeral(true).queue();
            } else {
                int index = messageLink.lastIndexOf('/');
                long messageId = Long.parseLong(messageLink.substring(index + 1));
                event.optMessageChannel("channel").editMessageById(messageId, event.optString("message")).queue(message -> {
                    event.reply("Edited message.").setEphemeral(true).queue();
                }, throwable -> {
                    event.reply("Unable to send message, likely not from the bot.").setEphemeral(true).queue();
                });
            }
        }
    }

    public static class Embed extends SlashCommand {
        public Embed() {
            this.name = "embed";
            this.help = "Sends an embedded message as the bot.";

            this.options = Arrays.asList(
                    new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message to", true)
                            .setChannelTypes(ChannelType.TEXT),
                    new OptionData(OptionType.STRING, "message_link", "The message to modify"),
                    new OptionData(OptionType.STRING, "title", "The title of the embed"),
                    new OptionData(OptionType.STRING, "description", "The description of the embed"),
                    new OptionData(OptionType.STRING, "author", "The author of the embed"),
                    new OptionData(OptionType.STRING, "footer", "The footer of the embed"),
                    new OptionData(OptionType.STRING, "color", "The color of the embed"),
                    new OptionData(OptionType.STRING, "title-url", "The title URL of the embed"),
                    new OptionData(OptionType.STRING, "thumbnail-url", "The thumbnail URL of the embed"),
                    new OptionData(OptionType.STRING, "footer-icon-url", "The footer icon URL of the embed"),
                    new OptionData(OptionType.STRING, "author-url", "The author URL of the embed"),
                    new OptionData(OptionType.STRING, "author-icon-url", "The author icon URL of the embed"),
                    new OptionData(OptionType.STRING, "image-url", "The image URL of the embed")
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            EmbedBuilder builder = new EmbedBuilder();

            String title = event.optString("title");
            if (title != null) {
                String titleUrl = event.optString("title-url");
                if (titleUrl != null) builder.setTitle(title, titleUrl);
                else builder.setTitle(title);
            }

            String description = event.optString("description");
            if (description != null) {
                builder.setDescription(description);
            }

            String author = event.optString("author");
            if (author != null) {
                String authorUrl = event.optString("author-url");
                if (authorUrl != null) {
                    String authorIconUrl = event.optString("author-icon-url");
                    if (authorIconUrl != null) builder.setAuthor(author, authorUrl, authorIconUrl);
                    else builder.setAuthor(author, authorUrl);
                } else builder.setAuthor(author);
            }

            String footer = event.optString("footer");
            if (footer != null) {
                String footerIconUrl = event.optString("footer-icon-url");
                if (footerIconUrl != null) builder.setFooter(footer, footerIconUrl);
                else builder.setFooter(footer);
            }

            String color = event.optString("color");
            if (color != null) {
                if (color.startsWith("#")) color = color.substring(1);

                int red = Integer.parseInt(color.substring(0, 2), 16);
                int green = Integer.parseInt(color.substring(2, 4), 16);
                int blue = Integer.parseInt(color.substring(4, 6), 16);

                builder.setColor(new Color(red, green, blue));
            }

            String thumbnailUrl = event.optString("thumbnail-url");
            if (thumbnailUrl != null) {
                builder.setThumbnail(thumbnailUrl);
            }

            String imageUrl = event.optString("image-url");
            if (imageUrl != null) {
                builder.setImage(imageUrl);
            }

            if (builder.isEmpty()) {
                event.reply("Cannot send an empty embed.").setEphemeral(true).queue();
                return;
            }

            MessageEmbed embed = builder.build();

            String messageLink = event.optString("message_link");
            if (messageLink == null) {
                event.optMessageChannel("channel").sendMessageEmbeds(embed).queue();
                event.reply("Sent message.").setEphemeral(true).queue();
            } else {
                int index = messageLink.lastIndexOf('/');
                long messageId = Long.parseLong(messageLink.substring(index + 1));
                event.optMessageChannel("channel").editMessageEmbedsById(messageId, embed).queue(message -> {
                    event.reply("Edited message.").setEphemeral(true).queue();
                }, throwable -> {
                    event.reply("Unable to send message, likely not from the bot.").setEphemeral(true).queue();
                });
            }
        }
    }
}
