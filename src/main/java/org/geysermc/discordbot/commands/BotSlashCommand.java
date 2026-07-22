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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import org.geysermc.discordbot.storage.ServerModule;
import org.geysermc.discordbot.util.BotServers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BotSlashCommand extends SlashCommand {
    private static final Logger log = LoggerFactory.getLogger(BotSlashCommand.class);
    protected BotServers[] validServers = new BotServers[]{};
    protected ServerModule requiredModule = null;

    public BotServers[] getValidServers() {
        return validServers;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (requiredModule != null && !requiredModule.isEnabled(event.getGuild())) {
            event.reply("The %s module is not enabled in this server!".formatted(requiredModule.getName()))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (event.getGuild() == null || validServers.length == 0) {
            executeCommand(event);
            return;
        }

        for (BotServers servers : validServers) {
            if (servers.getServerId().equals(event.getGuild().getId())) {
                executeCommand(event);
                return;
            }
        }

        event.reply("This command cannot be executed in this server!")
                .setEphemeral(true)
                .queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (requiredModule != null && !requiredModule.isEnabled(event.getGuild())) {
            return; // Silently drop instead, the command "doesn't exist"
        }

        if (event.getGuild() == null || validServers.length == 0) {
            executeCommand(event);
            return;
        }

        for (BotServers servers : validServers) {
            if (servers.getServerId().equals(event.getGuild().getId())) {
                executeCommand(event);
                return;
            }
        }

        // Silently drop instead, the command "doesn't exist"
    }

    protected abstract void executeCommand(SlashCommandEvent event);

    protected void executeCommand(CommandEvent event) {}
}
