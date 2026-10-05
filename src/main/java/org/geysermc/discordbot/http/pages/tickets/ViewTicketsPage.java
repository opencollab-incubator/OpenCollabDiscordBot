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

package org.geysermc.discordbot.http.pages.tickets;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.pages.elements.TicketCard;
import org.geysermc.discordbot.util.TicketHelper;
import org.geysermc.discordbot.util.ticket.GuildTicket;
import org.geysermc.discordbot.util.ticket.GuildTickets;
import org.geysermc.discordbot.util.ticket.TicketState;

import java.util.List;

public class ViewTicketsPage extends Base {
    private final Guild guild;

    public ViewTicketsPage(User user, Guild guild) {
        super(user);
        this.guild = guild;
    }

    @Override
    public void addContent(Element container) {
        GuildTickets tickets = TicketHelper.getTickets(guild);

        if (tickets.tickets().isEmpty()) {
            container.addInnerElement(Element.h2().addInnerElement("So empty..."));
            container.addInnerElement(Element.p().addInnerElement("This server has no tickets currently."));
            return;
        }

        addSection(container, "Open Tickets", tickets.getOrderedTickets(TicketState.OPEN), "No tickets are currently open.");
        addSection(container, "Closed Tickets", tickets.getOrderedTickets(TicketState.CLOSED), "No tickets have been closed yet.");
    }

    private void addSection(Element container, String title, List<GuildTicket> ticketList, String emptyMessage) {
        container.addInnerElement(Element.h2().addInnerElement(title));

        if (ticketList.isEmpty()) {
            container.addInnerElement(Element.p()
                            .addClass("mb-8")
                    .addInnerElement(emptyMessage));
            return;
        }

        Element cardContainer = Element.div("grid gap-4 justify-center mb-6");
        cardContainer.addField("style", "grid-template-columns: repeat(auto-fill, minmax(400px, 0.25fr));");

        for (GuildTicket ticket : ticketList) {
            cardContainer.addInnerElement(new TicketCard(guild.getId(), ticket.metadata()));
        }

        container.addInnerElement(cardContainer);
    }
}
