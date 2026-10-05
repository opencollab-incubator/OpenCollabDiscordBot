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

package org.geysermc.discordbot.commands.tag;

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.commands.BotSlashCommand;
import org.geysermc.discordbot.util.BotColors;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class RefreshTagsCommand extends BotSlashCommand {
    private static final List<String> MESSAGES = List.of(
            "Sit back and relax, we got this...",
            "Tag, you're it!",
            "Rory foreva",
            "GeyserMC called, they want their bot back.",
            "What happens during a thunderstorm? A cloudburst.",
            "Bukkit. *what?* Bukkit. *you mean Nukkit?* That's what I said, Bukkit. *ok...*",
            "Such a boar when waiting for the refresh...",
            "Good morning, my name is John (Open) Collaboration, I'm here to talk about your car's extended warranty.",
            "Geyser... Floodgate... Hydraulic..., someone would have assumed we use water based names! Oh wait...",
            "To proxy, or to pass, that is the question.",
            "I'm detecting many bugs! Oh wait, bedrock's around. False alarm.",
            "If you keep refreshing, you're gonna find my secret brownie recipe...",
            "Wait, why am I even writing these messages?",
            "Screw it, I'm actually including the instructions to bake brownies in here.",
            "Ingredients, 2 cups white sugar, 1 ¼ cups all-purpose flour, " +
                    "1 cup unsalted butter, melted, 4 large eggs, room temperature, " +
                    "1/2 cup cocoa powder, 1 teaspoon vanilla extract, " +
                    "1/2 teaspoon baking powder, 1/2 teaspoon salt",
            "Step 1: Gather all ingredients.",
            "Step 2: Preheat the oven to 350 degrees F (175 degrees C). Grease a 9x13-inch pan with baking spray.",
            "Step 3: Whisk sugar, flour, melted butter, eggs, cocoa powder, vanilla, baking powder, and salt in a large bowl until combined.",
            "Step 4: Spread the batter into the prepared pan.",
            "Step 5: Bake in the preheated oven until top is crinkled and a toothpick comes out with a few moist crumbs, about 20 to 30 minutes.",
            "Step 6: Let cool completely in the pan on a wire rack before slicing into squares. Enjoy!"
    );

    private static final Random RND = new Random();

    public RefreshTagsCommand() {
        this.name = "refresh-tags";
        this.help = "Refreshes all the tags in this server";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};

        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void executeCommand(SlashCommandEvent event) {
        try {
            GeyserBot.tagManager.refreshTags(event.getGuild());
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Refreshing tags...")
                    .setDescription(MESSAGES.get(RND.nextInt(MESSAGES.size())))
                    .setColor(BotColors.SUCCESS.getColor())
                    .build()).queue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
