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

package org.geysermc.discordbot.commands.administration;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.modals.Modal;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.commands.BotSlashCommand;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.util.BotColors;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationCommand extends BotSlashCommand {
    public ConfigurationCommand() {
        this.name = "configuration";
        this.hidden = true;
        this.aliases = new String[]{"config", "settings"};
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};

        this.children = new SlashCommand[]{
                new Get(),
                new Set()
        };
    }

    @Override
    protected void executeCommand(SlashCommandEvent event) {} // Sub-commands means this cannot be called

    private static void handleOptionAutoComplete(CommandAutoCompleteInteractionEvent event) {
        String query = event.getFocusedOption().getValue().toLowerCase();

        List<Command.Choice> choices = new ArrayList<>();

        List<Command.Choice> startWithChoices = new ArrayList<>();
        List<Command.Choice> containChoices = new ArrayList<>();

        for (ServerConfiguration.Configuration<?> configuration : ServerConfiguration.getAllConfigs()) {
            Command.Choice choice = new Command.Choice(configuration.getName(), configuration.getKey());
            if (configuration.getName().toLowerCase().startsWith(query) ||
                    configuration.getKey().toLowerCase().startsWith(query)) startWithChoices.add(choice);
            else if (configuration.getName().toLowerCase().contains(query) ||
                    configuration.getKey().toLowerCase().contains(query)) containChoices.add(choice);
        }

        choices.addAll(startWithChoices);
        choices.addAll(containChoices);

        if (choices.size() > 25) {
            List<Command.Choice> newChoices = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                newChoices.add(i, choices.get(i));
            }
            choices = newChoices;
        }

        event.replyChoices(choices).queue();
    }

    public static class Get extends SlashCommand {
        public Get() {
            this.name = "get";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "key", "The option to get", true)
                            .setAutoComplete(true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String key = event.optString("key");

            for (ServerConfiguration.Configuration<?> configuration : ServerConfiguration.getAllConfigs()) {
                if (!configuration.getKey().equals(key)) continue;

                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Setting value")
                        .addField("Key", "`%s`".formatted(key), false)
                        .addField("Value", configuration.getDisplay(event.getGuild()), false)
                        .build()).queue();

                return;
            }

            // Fallback, try pull from StorageManager
            String value = GeyserBot.storageManager.getServerPreference(event.getGuild().getIdLong(), key);

            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Setting value")
                    .addField("Key", "`%s`".formatted(key), false)
                    .addField("Value", value == null ? "`null`" : "`%s`".formatted(value), false)
                    .build()).queue();
        }

        @Override
        public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
            handleOptionAutoComplete(event);
        }
    }

    public static class Set extends SlashCommand {
        public Set() {
            this.name = "set";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "key", "The option to set", true)
                            .setAutoComplete(true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String key = event.optString("key");

            ServerConfiguration.Configuration<?> configuration = ServerConfiguration.get(key);

            if (configuration == null) {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Error!")
                        .setDescription("Unable to find configuration specified.")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(true).queue();

                return;
            }

            Modal.Builder builder = Modal.create("config-set-%s".formatted(key), configuration.getName());

            switch (configuration.getConfigType()) {
                case SINGLE -> {
                    if (configuration.getType() == String.class) {
                        builder.addComponents(
                                Label.of(
                                        "Enter value:",
                                        TextInput.of("single-text-input", TextInputStyle.SHORT)
                                )
                        );
                    } else if (configuration.getType() == Role.class) {
                        builder.addComponents(
                                Label.of(
                                        "Select role:",
                                        EntitySelectMenu.create("single-role-input", EntitySelectMenu.SelectTarget.ROLE)
                                                .setMinValues(1).setMaxValues(1)
                                                .build()
                                )
                        );
                    } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                        Class<? extends GuildChannel> channelClazz = (Class<? extends GuildChannel>) configuration.getType();
                        builder.addComponents(
                                Label.of(
                                        "Select channel:",
                                        EntitySelectMenu.create("single-channel-input", EntitySelectMenu.SelectTarget.CHANNEL)
                                                .setChannelTypes(typeFromClass(channelClazz))
                                                .setMinValues(1).setMaxValues(1)
                                                .build()
                                )
                        );
                    }
                }
                case MULTIPLE -> {
                    if (configuration.getType() == String.class) {
                        builder.addComponents(
                                Label.of(
                                        "Enter values: (Separated by a comma)",
                                        TextInput.of("multi-text-input", TextInputStyle.PARAGRAPH)
                                )
                        );
                    } else if (configuration.getType() == Role.class) {
                        builder.addComponents(
                                Label.of(
                                        "Select roles:",
                                        EntitySelectMenu.create("multi-role-input", EntitySelectMenu.SelectTarget.ROLE)
                                                .setMinValues(1)
                                                .build()
                                )
                        );
                    } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                        Class<? extends GuildChannel> channelClazz = (Class<? extends GuildChannel>) configuration.getType();
                        builder.addComponents(
                                Label.of(
                                        "Select channels:",
                                        EntitySelectMenu.create("multi-channel-input", EntitySelectMenu.SelectTarget.CHANNEL)
                                                .setChannelTypes(typeFromClass(channelClazz))
                                                .setMinValues(1)
                                                .build()
                                )
                        );
                    }
                }
                case MAP -> {
                    builder.addComponents(Label.of(
                            "Enter keys: (Separated by a comma)",
                            TextInput.of("key-input", TextInputStyle.PARAGRAPH)
                    ));
                    if (configuration.getType() == String.class) {
                        builder.addComponents(
                                Label.of(
                                        "Enter values: (Separated by a comma)",
                                        TextInput.of("map-text-input", TextInputStyle.SHORT)
                                )
                        );
                    } else if (configuration.getType() == Role.class) {
                        builder.addComponents(
                                Label.of(
                                        "Select roles:",
                                        EntitySelectMenu.create("map-role-input", EntitySelectMenu.SelectTarget.ROLE)
                                                .setMinValues(1)
                                                .build()
                                )
                        );
                    } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                        Class<? extends GuildChannel> channelClazz = (Class<? extends GuildChannel>) configuration.getType();
                        builder.addComponents(
                                Label.of(
                                        "Select channels:",
                                        EntitySelectMenu.create("map-channel-input", EntitySelectMenu.SelectTarget.CHANNEL)
                                                .setChannelTypes(typeFromClass(channelClazz))
                                                .setMinValues(1)
                                                .build()
                                )
                        );
                    }
                }
            }

            event.replyModal(builder.build()).queue();
        }

        private ChannelType typeFromClass(Class<? extends GuildChannel> clazz) {
            for (ChannelType type : ChannelType.values()) {
                if (type.getInterface() == clazz) return type;
            }

            return null;
        }

        @Override
        public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
            handleOptionAutoComplete(event);
        }
    }
}
