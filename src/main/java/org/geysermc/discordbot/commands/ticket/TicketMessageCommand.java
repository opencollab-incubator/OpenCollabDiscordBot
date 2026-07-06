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

package org.geysermc.discordbot.commands.ticket;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.ticket.TicketType;

public class TicketMessageCommand extends SlashCommand {

    public TicketMessageCommand() {
        this.name = "ticket-message";
        this.help = "Sends the message allowing users to create tickets";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        event.getChannel().sendMessageComponents(Container.of(
                        TextDisplay.of("## Create a ticket"),
                        TextDisplay.of("To request assistance on an adopted project, please open a ticket here!"),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TicketType.getButtons()
                ).withAccentColor(BotColors.SUCCESS.getColor()))
                .useComponentsV2()
                .queue();

        event.reply("Setup ticket message.").setEphemeral(true).queue();
    }
}
