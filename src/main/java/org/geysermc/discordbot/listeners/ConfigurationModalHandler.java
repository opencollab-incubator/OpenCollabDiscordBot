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

package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.util.BotColors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationModalHandler extends Handler {
    private final String prefix = "config-set-";

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getInteraction().getCustomId().startsWith(prefix)) return;

        String configKey = event.getInteraction().getCustomId().substring(prefix.length());

        ServerConfiguration.Configuration<?> configuration = ServerConfiguration.get(configKey);
        if (configuration == null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Error!")
                    .setDescription("Unable to find configuration specified.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build()).setEphemeral(true).queue();

            return;
        }

        String oldDisplay = configuration.getDisplay(event.getGuild());

        switch (configuration.getConfigType()) {
            case SINGLE -> {
                if (configuration.getType() == String.class) {
                    ((ServerConfiguration.Configuration<String>) configuration)
                            .set(event.getGuild(), event.getInteraction().getValue("single-text-input").getAsString());
                } else if (configuration.getType() == Role.class) {
                    ((ServerConfiguration.Configuration<Role>) configuration)
                            .set(event.getGuild(), event.getInteraction().getValue("single-role-input").getAsMentions()
                                    .getRoles().getFirst());
                } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                    ((ServerConfiguration.Configuration<GuildChannel>) configuration)
                            .set(event.getGuild(), event.getInteraction().getValue("single-channel-input").getAsMentions()
                                    .getChannels().getFirst());
                }
            }
            case MULTIPLE -> {
                if (configuration.getType() == String.class) {
                    ((ServerConfiguration.Configuration<List<String>>) configuration)
                            .set(event.getGuild(), List.of(event.getInteraction().getValue("multi-text-input").getAsString()
                                    .split(",")));
                } else if (configuration.getType() == Role.class) {
                    ((ServerConfiguration.Configuration<List<Role>>) configuration)
                            .set(event.getGuild(), event.getInteraction().getValue("multi-role-input").getAsMentions()
                                    .getRoles());
                } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                    ((ServerConfiguration.Configuration<List<GuildChannel>>) configuration)
                            .set(event.getGuild(), event.getInteraction().getValue("multi-channel-input").getAsMentions()
                                    .getChannels());
                }
            }
        }

        event.replyEmbeds(new EmbedBuilder()
                .setTitle("Configured Value")
                .addField("Old Value", oldDisplay, false)
                .addField("New Value", configuration.getDisplay(event.getGuild()), false)
                .setColor(BotColors.SUCCESS.getColor())
                .build()).queue();
    }
}
