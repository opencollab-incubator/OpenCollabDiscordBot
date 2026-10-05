/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.discordbot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.Appeal;
import org.geysermc.discordbot.storage.BanData;
import org.geysermc.discordbot.storage.ServerConfiguration;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ModerationHelper {
    private ModerationHelper() {}

    public static void quarantineMember(Member user, Guild guild, String reason, boolean automatic, @Nullable Member staffMember, @Nullable Message referenceMessage, boolean deleteReferenceMessage) {
        if (staffMember == null) staffMember = guild.getSelfMember();

        Member checkMember = guild.getMemberById(user.getId());
        if (checkMember == null) {
            if (referenceMessage != null && deleteReferenceMessage) {
                referenceMessage.delete().queue(v -> {}, throwable -> {});
            }
            return;
        }

        String title;

        if (automatic) {
            title = "You have been automatically timed out from %s!";
        } else {
            title = "You have been timed out from %s!";
        }

        user.getUser().openPrivateChannel().queue((channel) -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(title.formatted(guild.getName()))
                    .addField("Reason", reason, false)
                    .addField("Recommended Actions", "Change your Discord password, enable 2FA, and scan your computer for malware. See [Discord's article](https://support.discord.com/hc/en-us/articles/24160905919511-My-Discord-Account-was-Hacked-or-Compromised) for more info.", false)
                    .addField("Information", "This punishment also applies to all Open Collaboration owned servers. If you believe this was an accident or a false flag, please reach out to a member of staff in order to get this sorted.", false)
                    .setTimestamp(Instant.now())
                    .setColor(BotColors.WARNING.getColor())
                    .build();

            channel.sendMessageEmbeds(embed).queue();
        });

        if (GeyserBot.AUDIT) {
            ServerConfiguration.LOG_CHANNEL.ifPresent(guild,
                    channel -> channel.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setTitle("Audit entry")
                                    .setDescription("Quarantine has been audited, no action has been taken.")
                                    .setColor(BotColors.SUCCESS.getColor())
                                    .build()
                    ).setContent(ServerConfiguration.AUDIT_ROLE.get(guild).getAsMention()).queue());
            return;
        }

        MessageCreateAction action;
        boolean skipLogging = false;

        if (!staffMember.canInteract(user)) {
            action = ServerConfiguration.MOD_CHANNEL.get(guild).sendMessage(
                    new MessageCreateBuilder()
                            .setContent(user.getAsMention())
                            .setEmbeds(new EmbedBuilder()
                                    .setTitle("Unactioned quarantine.")
                                    .setDescription("**Reason**: " + reason + "\n" +
                                            user.getAsMention() + " cannot be quarantined as I do not have permission to timeout the user. Please take manual action!")
                                    .setTimestamp(Instant.now())
                                    .setColor(BotColors.FAILURE.getColor())
                                    .build())
                            .build()
            ).queue(message -> {
                if (referenceMessage != null) {
                    referenceMessage.forwardTo(message.getChannel()).queue(msg -> {
                        if (deleteReferenceMessage) {
                            referenceMessage.delete().queue(v -> {}, throwable -> {});
                        }
                    }, throwable -> {
                        if (deleteReferenceMessage) {
                            referenceMessage.delete().queue(v -> {}, throwable2 -> {});
                        }
                    });
                }
            });

            // Now log it!
            int id = GeyserBot.storageManager.addLog(staffMember, "quarantine", user, reason);

            MessageEmbed quarantinedEmbed = new EmbedBuilder()
                    .setTitle("Quarantined user (Unactioned!)")
                    .addField("User", user.getAsMention(), false)
                    .addField("Staff member", staffMember.getAsMention(), false)
                    .addField("Reason", reason, false)
                    .setFooter("ID: " + id)
                    .setTimestamp(Instant.now())
                    .setColor(BotColors.WARNING.getColor())
                    .build();

            LogHelper.log(guild, quarantinedEmbed);

            skipLogging = true;
        } else {
            Duration duration = Duration.ofSeconds(60 * 60 * 24 * 28); // 28 days
            BotServers.executeFromAll(user, (member) -> member.timeoutFor(duration).queue(ignored -> {}, t -> {}));

            // Send a message in the mod chat with buttons to take action
            ActionRow row = ActionRow.of(
                    StringSelectMenu.create("quarantine-handler")
                            .setPlaceholder("Select an action")
                            .addOption("Unquarantine", "unquarantine", "Unquarantine the user.")
                            .addOption("Honey pot misuse", "honeypot-misuse", "Punish the user for misuse of the honeypot channel. (1 day timeout.)")
                            .addOption("Compromised account", "compromised", "Ban the user for compromised account.")
                            .addOption("Timeout (1 week)", "timeout", "Timeout the user for 1 week.")
                            .addOption("Kick", "kick", "Kick the user.")
                            .addOption("Ban", "ban", "Ban the user.")
                            .build()
            );

            String timestamp = TimeFormat.RELATIVE.format(LocalDateTime.now().plusDays(28).toInstant(ZoneOffset.UTC));

             action = ServerConfiguration.MOD_CHANNEL.get(guild).sendMessage(
                    new MessageCreateBuilder()
                            .setContent(user.getAsMention())
                            .setEmbeds(new EmbedBuilder()
                                    .setTitle("Quarantined user")
                                    .setDescription("**Reason**: " + reason + "\n" +
                                            user.getAsMention() + " has been quarantined. Select an action below to take. Quarantine expires %s.".formatted(timestamp))
                                    .setTimestamp(Instant.now())
                                    .setColor(BotColors.FAILURE.getColor())
                                    .build())
                            .build()
            ).addComponents(row);
        }

        action.queue(message -> {
            if (referenceMessage != null) {
                referenceMessage.forwardTo(message.getChannel()).queue(msg -> {
                    if (deleteReferenceMessage) {
                        referenceMessage.delete().queue(v -> {}, throwable -> {});
                    }
                }, throwable -> {
                    if (deleteReferenceMessage) {
                        referenceMessage.delete().queue(v -> {}, throwable2 -> {});
                    }
                });
            }
        });

        if (!skipLogging) {

            // Now log it!
            int id = GeyserBot.storageManager.addLog(staffMember, "quarantine", user, reason);

            MessageEmbed quarantinedEmbed = new EmbedBuilder()
                    .setTitle("Quarantined user")
                    .addField("User", user.getAsMention(), false)
                    .addField("Staff member", staffMember.getAsMention(), false)
                    .addField("Reason", reason, false)
                    .setFooter("ID: " + id)
                    .setTimestamp(Instant.now())
                    .setColor(BotColors.WARNING.getColor())
                    .build();

            LogHelper.log(guild, quarantinedEmbed);
        }
    }

    public static MessageEmbed timeoutUser(Member member, Member moderator, Guild guild, Duration duration, boolean silent, String reason) {
        if (moderator == null) moderator = guild.getSelfMember();

        // Check the user exists
        if (member == null) {
            return new EmbedBuilder()
                    .setTitle("Invalid user")
                    .setDescription("The user ID specified doesn't link with any valid user in this server.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        // Check we can target the user
        if (!BotHelpers.canTarget(moderator, member)) {
            return new EmbedBuilder()
                    .setTitle("Higher role")
                    .setDescription("Either the bot or you cannot target that user.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        User user = member.getUser();

        Runnable action = () -> {
            if (GeyserBot.AUDIT) {
                ServerConfiguration.LOG_CHANNEL.ifPresent(guild,
                        channel -> channel.sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setTitle("Audit entry")
                                        .setDescription("Timeout has been audited, no action has been taken.")
                                        .setColor(BotColors.SUCCESS.getColor())
                                        .build()
                        ).setContent(ServerConfiguration.AUDIT_ROLE.get(guild).getAsMention()).queue());
                return;
            }

            BotServers.executeFromAll(guild, (server) -> server.timeoutFor(user, duration).reason(reason).queue(ignored -> {}, t -> {}));
        };

        // Let the user know they're timed out if we are not being silent
        if (!silent) {
            user.openPrivateChannel().queue((channel) -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("You have been timed out from %s!".formatted(guild.getName()))
                        .addField("Reason", reason, false)
                        .addField("Source Server", guild.getName(), false)
                        .addField("Information", "This punishment also applies to all Open Collaboration owned servers. If you believe this was an accident or a false flag, please reach out to a member of staff in order to get this sorted.", false)
                        .setTimestamp(Instant.now())
                        .setColor(BotColors.FAILURE.getColor());

                String punishmentMessage = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), "punishment-message");
                if (punishmentMessage != null && !punishmentMessage.isEmpty()) {
                    embedBuilder.addField("Additional Info", punishmentMessage, false);
                }

                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
                    // Timeout user
                    action.run();
                }, throwable -> {
                    // Timeout user
                    action.run();
                });
            }, throwable -> {
                // Timeout user
                action.run();
            });
        } else {
            // Timeout user
            action.run();
        }

        // Log the change
        int id = GeyserBot.storageManager.addLog(moderator, "timeout", user, reason);

        MessageEmbed timedOutEmbed = new EmbedBuilder()
                .setTitle("Timed out user")
                .addField("User", user.getAsMention(), false)
                .addField("Staff member", moderator.getAsMention(), false)
                .addField("Reason", reason, false)
                .setFooter("ID: " + id)
                .setTimestamp(Instant.now())
                .setColor(BotColors.SUCCESS.getColor())
                .build();

        // Send the embed as a reply and to the log
        LogHelper.log(guild, timedOutEmbed);
        return timedOutEmbed;
    }

    public static MessageEmbed kickUser(Member member, Member moderator, Guild guild, boolean silent, String reason, @Nullable Channel originChannel) {
        if (moderator == null) moderator = guild.getSelfMember();

        // Check the user exists
        if (member == null) {
            return new EmbedBuilder()
                    .setTitle("Invalid user")
                    .setDescription("The user ID specified doesn't link with any valid user in this server.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        // Check we can target the user
        if (!BotHelpers.canTarget(moderator, member)) {
            return new EmbedBuilder()
                    .setTitle("Higher role")
                    .setDescription("Either the bot or you cannot target that user.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        User user = member.getUser();

        Runnable action = () -> {
            if (GeyserBot.AUDIT) {
                ServerConfiguration.LOG_CHANNEL.ifPresent(guild,
                        channel -> channel.sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setTitle("Audit entry")
                                        .setDescription("Timeout has been audited, no action has been taken.")
                                        .setColor(BotColors.SUCCESS.getColor())
                                        .build()
                        ).setContent(ServerConfiguration.AUDIT_ROLE.get(guild).getAsMention()).queue());
                return;
            }

            BotServers.executeFromAll(guild, (server) -> server.kick(user).reason(reason).queue(ignored -> {}, t -> {}));
        };

        // Let the user know they're kicked if we are not being silent
        if (!silent) {
            user.openPrivateChannel().queue((channel) -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("You have been kicked from %s!".formatted(guild.getName()))
                        .addField("Reason", reason, false)
                        .addField("Source Server", guild.getName(), false)
                        .addField("Information", "This punishment also applies to all Open Collaboration owned servers. If you believe this was an accident or a false flag, please reach out to a member of staff in order to get this sorted.", false)
                        .setTimestamp(Instant.now())
                        .setColor(BotColors.FAILURE.getColor());

                String punishmentMessage = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), "punishment-message");
                if (punishmentMessage != null && !punishmentMessage.isEmpty()) {
                    embedBuilder.addField("Additional Info", punishmentMessage, false);
                }

                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
                    // Kick user
                    action.run();
                }, throwable -> {
                    // Kick user
                    action.run();
                });
            }, throwable -> {
                // Kick user
                action.run();
            });
        } else {
            // Kick user
            action.run();
        }

        // Log the change
        int id = GeyserBot.storageManager.addLog(moderator, "kick", user, reason);

        MessageEmbed kickedEmbed = new EmbedBuilder()
                .setTitle("Kicked user")
                .addField("User", user.getAsMention(), false)
                .addField("Staff member", moderator.getAsMention(), false)
                .addField("Reason", reason, false)
                .setFooter("ID: " + id)
                .setTimestamp(Instant.now())
                .setColor(BotColors.SUCCESS.getColor())
                .build();

        // Send the embed as a reply and to the log
        LogHelper.log(guild, kickedEmbed);
        if (originChannel == null || !ServerConfiguration.MOD_CHANNEL.isOrContains(guild, originChannel)) {
            ServerConfiguration.MOD_CHANNEL.get(guild).sendMessageEmbeds(kickedEmbed).queue();
        }
        return kickedEmbed;
    }

    public static MessageEmbed banUser(Member member, Member moderator, Guild guild, int days, int daysDeleted, boolean silent, String reason, @Nullable Channel originChannel) {
        if (moderator == null) moderator = guild.getSelfMember();

        // Check the user exists
        if (member == null) {
            return new EmbedBuilder()
                    .setTitle("Invalid user")
                    .setDescription("The user ID specified doesn't link with any valid user in this server.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        // Check we can target the user
        if (!BotHelpers.canTarget(moderator, member)) {
            return new EmbedBuilder()
                    .setTitle("Higher role")
                    .setDescription("Either the bot or you cannot target that user.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build();
        }

        User user = member.getUser();

        MessageEmbed bannedEmbed = new EmbedBuilder()
                .setTitle("Banned user")
                .addField("User", user.getAsMention(), false)
                .addField("Staff member", moderator.getAsMention(), false)
                .addField("Reason", reason, false)
                .addField("Source Server", guild.getName(), false)
                .setTimestamp(Instant.now())
                .setColor(BotColors.SUCCESS.getColor())
                .build();

        Runnable action = () -> {
            if (GeyserBot.AUDIT) {
                ServerConfiguration.LOG_CHANNEL.ifPresent(guild,
                        channel -> channel.sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setTitle("Audit entry")
                                        .setDescription("Ban has been audited, no action has been taken.")
                                        .setColor(BotColors.SUCCESS.getColor())
                                        .build()
                        ).setContent(ServerConfiguration.AUDIT_ROLE.get(guild).getAsMention()).queue());
                return;
            }
            // Ban user
            GeyserBot.storageManager.addBan(new BanData(user, Instant.now(), days == -1 ? null : Instant.now().plus(days, ChronoUnit.DAYS), reason));
            BotServers.executeFromAll(guild, (server) -> {
                server.ban(user, daysDeleted, TimeUnit.DAYS).reason(reason).queue(ignored -> {}, t -> {});
                LogHelper.log(server, bannedEmbed);
            });
        };

        // Let the user know they're banned if we are not being silent
        if (!silent) {
            user.openPrivateChannel().queue((channel) -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("You have been banned from %s!".formatted(guild.getName()))
                        .addField("Reason", reason, false)
                        .addField("Source Server", guild.getName(), false)
                        .addField("Information", "This punishment also applies to all Open Collaboration owned servers. If you believe this was an accident or a false flag, please reach out to a member of staff in order to get this sorted.", false)
                        .setTimestamp(Instant.now())
                        .setColor(BotColors.FAILURE.getColor());

                String punishmentMessage = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), "punishment-message");
                if (punishmentMessage != null && !punishmentMessage.isEmpty()) {
                    embedBuilder.addField("Additional Info", punishmentMessage, false);
                }

                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
                    action.run();
                }, throwable -> {
                    action.run();
                });
            }, throwable -> {
                action.run();
            });
        } else {
            action.run();
        }

        if (originChannel == null || !ServerConfiguration.MOD_CHANNEL.isOrContains(guild, originChannel)) {
            TextChannel channel = ServerConfiguration.MOD_CHANNEL.get(guild);
            if (channel != null) channel.sendMessageEmbeds(bannedEmbed).queue();
        }
        return bannedEmbed;
    }

    public static MessageEmbed revokeBan(long userId, User mod, String reason) {
        UserSnowflake user = UserSnowflake.fromId(userId);

        for (Appeal appeal : GeyserBot.storageManager.getAppeals(user, 5)) {
            if (appeal.state().equals(Appeal.State.PENDING)) {
                GeyserBot.storageManager.updateAppeal(appeal.id(), Appeal.State.APPROVED);
            }
        }

        if (mod == null) mod = GeyserBot.getJDA().getSelfUser();

        MessageEmbed unbannedEmbed = new EmbedBuilder()
                .setTitle("Unbanned user")
                .addField("User", user.getAsMention(), false)
                .addField("Staff member", mod.getAsMention(), false)
                .addField("Reason", reason, false)
                .setTimestamp(Instant.now())
                .setColor(BotColors.SUCCESS.getColor())
                .build();

        BotServers.executeFromAll((guild) -> {
            LogHelper.log(guild, unbannedEmbed);
            guild.unban(user).queue();
        });

        return unbannedEmbed;
    }
}
