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
                    new OptionData(OptionType.STRING, "color", "The color of the embed (In hex format)"),
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
            String titleUrl = event.optString("title-url");
            builder.setTitle(title, titleUrl);

            String description = event.optString("description");
            builder.setDescription(description);

            String author = event.optString("author");
            String authorUrl = event.optString("author-url");
            String authorIconUrl = event.optString("author-icon-url");
            builder.setAuthor(author, authorUrl, authorIconUrl);

            String footer = event.optString("footer");
            String footerIconUrl = event.optString("footer-icon-url");
            builder.setFooter(footer, footerIconUrl);

            String color = event.optString("color");
            Color c;
            try {
                c = color == null ? null : Color.decode(color);
            } catch (NumberFormatException e) {
                event.reply("Invalid color was provided, either input a raw ARGB number, or a hex color code.")
                        .setEphemeral(true).queue();
                return;
            }
            builder.setColor(c);

            String thumbnailUrl = event.optString("thumbnail-url");
            builder.setThumbnail(thumbnailUrl);

            String imageUrl = event.optString("image-url");
            builder.setImage(imageUrl);

            MessageEmbed embed;
            try {
                embed = builder.build();
            } catch (IllegalStateException e) {
                event.reply(e.getMessage()).setEphemeral(true).queue();
                return;
            }

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
