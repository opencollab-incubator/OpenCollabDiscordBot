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

package org.geysermc.discordbot.util.ticket;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

public enum TicketType {
    DEFAULT(
            ButtonStyle.PRIMARY,
            "ticket-create",
            "Create Ticket",
            new TicketOptions(
                    false,
                    null
            )
    ),
    EXPLOIT(
            ButtonStyle.DANGER,
            "ticket-create-exploit",
            "Create Exploit Ticket",
            new TicketOptions(
                    true,
                    "exploit"
            )
    );

    private final ButtonStyle style;
    private final String buttonId;
    private final String label;
    private final TicketOptions options;

    TicketType(ButtonStyle style, String buttonId, String label, TicketOptions options) {
        this.style = style;
        this.buttonId = buttonId;
        this.label = label;
        this.options = options;
    }

    public String buttonId() {
        return this.buttonId;
    }

    public TicketOptions options() {
        return this.options;
    }

    public static ActionRow getButtons() {
        List<Button> buttons = new ArrayList<>();

        for (TicketType type : values()) {
            buttons.add(Button.of(type.style, type.buttonId, type.label));
        }
        return ActionRow.of(buttons);
    }

    public static TicketType getFromName(String name) {
        for (TicketType type : values()) {
            if (type.name().equals(name)) return type;
        }

        return null;
    }

    public static TicketType getTicketType(String id) {
        for (TicketType type : values()) {
            if (type.buttonId.equals(id) || (type.buttonId + "-client-select").equals(id)) return type;
        }

        return null;
    }
}
