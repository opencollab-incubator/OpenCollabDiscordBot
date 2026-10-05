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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.commands.filter.FilteredSlashCommand;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.PropertiesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TagsCommand extends FilteredSlashCommand {
    public TagsCommand() {
        this.name = "tags";
        this.arguments = "[search]";
        this.help = "List all the known (non-alias) tags";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};

        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "search", "The term you want to search for")
        );
    }

    @Override
    protected void executeFiltered(SlashCommandEvent event) {
        String search = event.optString("search", "");

        event.replyEmbeds(handle(search, event.getGuild())).queue();
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        List<String> args = new ArrayList<>(Arrays.asList(event.getArgs().split(" ")));

        // Check they specified an ip
        String search = "";
        if (!args.get(0).isEmpty()) {
            search = args.get(0);
        }

        event.getMessage().replyEmbeds(handle(search, event.getGuild())).queue();
    }

    protected MessageEmbed handle(String search, Guild guild) {
        EmbedBuilder embed = new EmbedBuilder();

        // Get tag names based on search
        List<String> tagNames = new ArrayList<>();
        for (String tag : GeyserBot.tagManager.getTags(guild).keySet()) {
            if (!tag.equals("alias") && tag.contains(search)) {
                tagNames.add(tag);
            }
        }

        // Sort the tag names
        Collections.sort(tagNames);

        if (tagNames.isEmpty()) {
            embed.setColor(BotColors.FAILURE.getColor());
            embed.setTitle("No tags found");
            embed.setDescription("No tags were found for your search.");
            embed.setFooter("Use `" + PropertiesManager.getPrefix() + "tag <name>` to show a tag");
        } else {
            embed.setColor(BotColors.SUCCESS.getColor());
            embed.setTitle("Tags (" + tagNames.size() + ")");
            embed.setDescription("`" + String.join("`, `", tagNames) + "`");
            embed.setFooter("Use `" + PropertiesManager.getPrefix() + "tag <name>` to show a tag");
        }

        return embed.build();
    }
}
