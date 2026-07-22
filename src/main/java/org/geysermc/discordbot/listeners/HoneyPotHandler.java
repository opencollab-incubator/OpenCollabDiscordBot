/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.util.ModerationHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HoneyPotHandler extends Handler {
    private final Map<String, Message> honeypotMessages = new HashMap<>();

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {
            TextChannel channel = ServerConfiguration.HONEY_POT_CHANNEL.get(guild);
            if (channel == null) continue;

            channel.getHistory().retrievePast(100).queue(messages -> {
                // Add a honey pot message if the bot has no messages in the channel
                boolean foundBotMessage = false;

                for (Message message : messages) {
                    if (message.getAuthor().getId().equals(guild.getSelfMember().getId())) {
                        honeypotMessages.put(guild.getId(), message);
                        foundBotMessage = true;
                        continue;
                    }

                    ModerationHelper.quarantineMember(message.getMember(), message.getGuild(), "Messaged in the honey pot channel.", false, null, message, true);
                }

                if (!foundBotMessage) {
                    channel.sendMessage("""
                            # DO NOT POST ANY MESSAGES HERE
                            
                            This is a honey pot channel designed to catch scam accounts. Sending a message here will lead to a temporary restriction of your account in this server.
                            """)
                            .setComponents(ActionRow.of(Button.primary("nothing-burger", "Successful Temptations: 0").withEmoji(Emoji.fromUnicode("\uD83C\uDF6F")).asDisabled()))
                            .queue(msg -> honeypotMessages.put(guild.getId(), msg));
                }
            });
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        if (isHoneyPot(event.getGuild(), event.getChannel())) {
            ModerationHelper.quarantineMember(event.getMember(), event.getGuild(), "Messaged in the honey pot channel.", false, null, event.getMessage(), true);

            Message message = honeypotMessages.get(event.getGuild().getId());
            if (message == null) return;

            String buttonText = ((Button) ((ActionRow) message.getComponents().getFirst()).getComponents().getFirst()).getLabel();
            int temptations = Integer.parseInt(buttonText.substring(24));

            ActionRow row = ActionRow.of(
                    Button.primary("nothing-burger", "Successful Temptations: " + (temptations + 1)
                    ).withEmoji(Emoji.fromUnicode("\uD83C\uDF6F")).asDisabled());

            message.editMessageComponents(row).queue();
        }
    }

    public static boolean isHoneyPot(Guild guild, Channel channel) {
        TextChannel honeyPotChannel = ServerConfiguration.HONEY_POT_CHANNEL.get(guild);
        if (honeyPotChannel == null) return false;
        return channel.getId().equals(honeyPotChannel.getId());
    }
}
