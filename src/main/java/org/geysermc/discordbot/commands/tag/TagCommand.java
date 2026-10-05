/*
 * Copyright (c) 2020-2026 Open Collaboration. https://opencollaboration.dev, GeyserMC. https://geysermc.org
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

package org.geysermc.discordbot.commands.tag;

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.commands.filter.FilteredSlashCommand;
import org.geysermc.discordbot.tag.Tag;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.DicesCoefficient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagCommand extends FilteredSlashCommand {

    private static final Logger log = LoggerFactory.getLogger(TagCommand.class);

    public TagCommand() {
        this.name = "tag";
        this.help = "Fetch a tag";
        this.arguments = "<name>";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};

        this.options = Collections.singletonList(
            new OptionData(OptionType.STRING, "name", "The tag to get (Supports aliases)", true)
                    .setAutoComplete(true)
        );
    }

    @Override
    protected void executeFiltered(SlashCommandEvent event) {
        if (!event.isFromGuild()) return;
        String tagName = event.getOption("name").getAsString();
        Tag tag = GeyserBot.tagManager.getTag(event.getGuild(), tagName);

        if (tag != null) {
            event.reply(tag.create()).queue();
        } else {
            event.replyEmbeds(new EmbedBuilder()
                    .setColor(BotColors.FAILURE.getColor())
                    .setTitle("Invalid tag")
                    .setDescription("Missing requested tag")
                    .build()).queue();
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        //if (!event.isFromGuild()) return;
        // Get the query
        String query = event.getFocusedOption().getValue();

        // Get the providers
        List<String> tags = potentialTags(query, event.getGuild());

        event.replyChoices(tags.stream()
                        .distinct()
                        .map(tag -> new Command.Choice(tag, tag))
                        .limit(25)
                        .toArray(Command.Choice[]::new))
                .queue();
    }

    List<String> potentialTags(String query, Guild guild) {
        List<String> potential = new ArrayList<>();

        for (String name : GeyserBot.tagManager.getTags(guild).keySet()) {
            // Check if the name starts with the query
            if (name.toLowerCase().startsWith(query.toLowerCase())) {
                potential.add(name);
                continue;
            }

            // Check if the name is similar
            double similar = DicesCoefficient.diceCoefficientOptimized(query.toLowerCase(), name.toLowerCase());
            if (similar > 0.2d) {
                potential.add(name);
            }
        }

        log.info(potential.toString());

        return potential;
    }
}
