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
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.geysermc.discordbot.commands.BotSlashCommand;
import org.geysermc.discordbot.storage.ServerModule;
import org.geysermc.discordbot.util.BotColors;

import java.util.List;

public class ModulesCommand extends BotSlashCommand {
    public ModulesCommand() {
        this.name = "modules";
        this.hidden = true;
        this.help = "Enable and disable certain server modules here.";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};

        this.children = new SlashCommand[]{
                new Enable(),
                new Disable()
        };
    }

    @Override
    protected void executeCommand(SlashCommandEvent event) {}

    public static class Enable extends SlashCommand {
        public Enable() {
            this.name = "enable";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "module", "The module to enable", true)
                            .addChoices(getModules())
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String moduleKey = event.optString("module");
            ServerModule module = ServerModule.get(moduleKey);
            if (module == null) {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module does not exist.")
                        .setDescription("`%s` does not exist, so cannot be enabled.".formatted(moduleKey))
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).queue();

                return;
            }

            if (module.setEnabled(event.getGuild(), true)) {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module enabled!")
                        .setDescription("`%s` was enabled successfully.".formatted(moduleKey))
                        .setColor(BotColors.SUCCESS.getColor())
                        .build()).queue();
            } else {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module is already enabled!")
                        .setDescription("`%s` is already enabled.".formatted(moduleKey))
                        .setColor(BotColors.NEUTRAL.getColor())
                        .build()).queue();
            }
        }
    }

    public static class Disable extends SlashCommand {
        public Disable() {
            this.name = "disable";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "module", "The module to disable", true)
                            .addChoices(getModules())
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            String moduleKey = event.optString("module");
            ServerModule module = ServerModule.get(moduleKey);
            if (module == null) {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module does not exist.")
                        .setDescription("`%s` does not exist, so cannot be disabled.".formatted(moduleKey))
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).queue();

                return;
            }

            if (module.setEnabled(event.getGuild(), false)) {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module disabled!")
                        .setDescription("`%s` was disabled successfully.".formatted(moduleKey))
                        .setColor(BotColors.SUCCESS.getColor())
                        .build()).queue();
            } else {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module is already disabled!")
                        .setDescription("`%s` is already disabled.".formatted(moduleKey))
                        .setColor(BotColors.NEUTRAL.getColor())
                        .build()).queue();
            }
        }
    }

    private static List<Command.Choice> getModules() {
        return ServerModule.getRegisteredModules().entrySet()
                .stream().map(entry ->
                        new Command.Choice(entry.getValue().getName(), entry.getKey())).toList();
    }
}
