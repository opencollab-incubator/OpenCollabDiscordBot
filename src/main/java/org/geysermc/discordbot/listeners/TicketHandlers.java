package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.ServerSettings;
import org.geysermc.discordbot.util.*;
import org.geysermc.discordbot.util.ticket.TicketData;
import org.geysermc.discordbot.util.ticket.TicketType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TicketHandlers {
    public static class Create extends ListenerAdapter {

        @Override
        public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
            TicketType type = TicketType.getTicketType(event.getInteraction().getCustomId());
            if (type == null) return;

            TicketData data = getTicketData(event, type);
            if (data == null) return;
            if (data.clientRole() == null) {
                List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

                event.replyModal(Modal.create(type.buttonId() + "-client-select", "Select client")
                                .addComponents(Label.of(
                                        "Client",
                                        StringSelectMenu.create("client-role")
                                                .setMaxValues(1).setMinValues(1)
                                                .addOptions(clientRoles.stream()
                                                        .map(role -> SelectOption.of(role.getName(), role.getId()))
                                                        .toList())
                                                .setRequired(true)
                                                .build()
                                ))
                        .build()).queue();
            } else {
                commonHandle(event, data, event::replyEmbeds);
            }
        }

        @Override
        public void onModalInteraction(@NotNull ModalInteractionEvent event) {
            TicketType type = TicketType.getTicketType(event.getInteraction().getCustomId());
            if (type == null) return;

            List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

            Role selectedRole = event.getGuild().getRoleById(event.getValue("client-role").getAsStringList().getFirst());

            if (!clientRoles.contains(selectedRole)) {
                event.reply("You did not select a client role.").setEphemeral(true).queue();
            }

            commonHandle(event, new TicketData(selectedRole, type), event::replyEmbeds);
        }

        private void commonHandle(GenericInteractionCreateEvent event, TicketData ticketData, Function<MessageEmbed, ReplyCallbackAction> methodCall) {
            Category category = getBestCategory(event.getGuild());
            if (category == null) {
                methodCall.apply(new EmbedBuilder()
                        .setTitle("Uh oh!")
                        .setDescription("No category could be found to create a ticket channel in! Please report this!")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(true).queue();

                return;
            }

            ChannelAction<TextChannel> action = category.createTextChannel(ticketData.getChannelName())
                    .addPermissionOverride(
                            event.getGuild().getPublicRole(), null, Collections.singleton(Permission.VIEW_CHANNEL)
                    )
                    .addPermissionOverride(
                            ticketData.clientRole(), Collections.singleton(Permission.VIEW_CHANNEL), null
                    );

            List<String> roleNames = new ArrayList<>();

            for (Role role : ServerSettings.getTicketAllowedRoles(event.getGuild())) {
                action = action.addPermissionOverride(role, Collections.singleton(Permission.VIEW_CHANNEL), null);
                roleNames.add(role.getName() + "(" + role.getId() + ")");
            }

            action.queue(channel -> {
                try {
                    TicketHelper.initTicket(channel, ticketData);

                    TicketHelper.logTicketAction(
                            channel,
                            "Created ticket.\n" +
                                    "- User: @" + event.getUser().getName() + " (ID: " + event.getUser().getId() + ")\n" +
                                    "- Client: @" + ticketData.clientRole().getName() + " (ID: " + ticketData.clientRole().getId() + ")\n" +
                                    "- Allowed Roles: " + (roleNames.isEmpty() ? "None" : String.join(", ", roleNames)) + "\n" +
                                    "- Channel ID: " + channel.getId() + "\n" +
                                    "- Type: " + ticketData.type().name()

                    );

                    ActionRow row = ActionRow.of(
                            Button.danger("ticket-close", "Close Ticket")
                    );

                    StringBuilder builder = new StringBuilder();

                    if (ticketData.type().options().pingRoles()) {
                        List<String> pings = new ArrayList<>();

                        for (Role pingedRole : ServerSettings.getTicketPingedRoles(event.getGuild())) {
                            pings.add(pingedRole.getAsMention());
                        }

                        builder.append(String.join(" ", pings));
                    }

                    channel.sendMessage(builder.toString())
                            .setEmbeds(new EmbedBuilder()
                                    .setTitle("Ticket created!")
                                    .setDescription(ticketData.type().message())
                                    .setColor(BotColors.SUCCESS.getColor())
                                    .build())
                            .setComponents(row)
                            .queue(message -> {
                                if (!DateUtils.isWeekday()) {
                                    message.replyEmbeds(new EmbedBuilder()
                                            .setTitle("Warning!")
                                            .setDescription("You've made a ticket on the weekend, there is no guarantee we will get back to you in a timely manner.")
                                            .setColor(BotColors.WARNING.getColor())
                                            .build()).queue();
                                } else if (!DateUtils.isWorkingHours()) {
                                    message.replyEmbeds(new EmbedBuilder()
                                            .setTitle("Warning!")
                                            .setDescription("You've made a ticket outside of UK working hours, there is no guarantee we will get back to you in a timely manner.")
                                            .setColor(BotColors.WARNING.getColor())
                                            .build()).queue();
                                } else if (DateUtils.isHoliday()) {
                                    message.replyEmbeds(new EmbedBuilder()
                                            .setTitle("Warning!")
                                            .setDescription("You've made a ticket on a UK holiday, there is no guarantee we will get back to you in a timely manner.")
                                            .setColor(BotColors.WARNING.getColor())
                                            .build()).queue();
                                }
                            });
                } catch (Exception e) {
                    StringWriter writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    e.printStackTrace(printWriter);
                    channel.sendMessage("Something went wrong! " + writer.toString()).queue();
                }
            });

            methodCall.apply(new EmbedBuilder()
                    .setTitle("Ticket created!")
                    .setColor(BotColors.SUCCESS.getColor())
                    .build()).setEphemeral(true).queue();
        }

        public Category getBestCategory(@NotNull Guild guild) {
            List<Category> categories = ServerSettings.getTicketCategories(guild);

            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                if (category.getChannels().size() == 50) continue;

                if (i == categories.size() - 1 && category.getChannels().size() >= 40) {
                    TextChannel channel = ServerSettings.getLogChannel(guild);
                    List<Role> roles = ServerSettings.getTicketPingedRoles(guild);
                    channel.sendMessage(roles.stream().map(Role::getAsMention).collect(Collectors.joining(", ")) + " Ticket category is getting full!").queue();
                }

                return category;
            }

            return null;
        }

        public TicketData getTicketData(@NotNull ButtonInteractionEvent event, TicketType type) {
            List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

            if (clientRoles.isEmpty()) {
                event.reply("You are not an Open Collaboration client.").setEphemeral(true).queue();
                return null;
            } else if (clientRoles.size() == 1) {
                Role role = clientRoles.getFirst();
                return new TicketData(role, role.getName().toLowerCase().replace(' ', '-'), type);
            } else {
                return new TicketData(null, null, type);
            }
        }
    }

    public static class Logs extends ListenerAdapter {
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            commonMessageHandle(event.getMessage(), event.getMember(), event.getChannel(), "sent");
        }

        @Override
        public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
            commonMessageHandle(event.getMessage(), event.getMember(), event.getChannel(), "edited");
        }

        private void commonMessageHandle(Message message, Member member, MessageChannel c, String action) {
            if (!ServerSettings.isTicketArchiving(member.getGuild())) return;
            if (!(c instanceof TextChannel channel)) return;
            if (!TicketHelper.isLoggedTicket(channel)) return;

            User user = member.getUser();

            StringBuilder logLine = new StringBuilder();
            logLine.append("@").append(user.getName())
                    .append(" (ID: ")
                    .append(user.getId())
                    .append(") ")
                    .append(action)
                    .append(" a message (ID: ")
                    .append(message.getId())
                    .append(")");

            if (!message.getContentRaw().isEmpty()) {
                logLine.append(" with content \"")
                        .append(message.getContentRaw())
                        .append("\"");
            }

            if (!message.getAttachments().isEmpty()) {
                logLine.append(" with attachments [");
                List<String> fileNames = new ArrayList<>();
                for (Message.Attachment attachment : message.getAttachments()) {
                    String fileName = attachment.getId() + "_" + attachment.getFileName();
                    fileNames.add(fileName);
                    try {
                        NetworkUtils.downloadTo(
                                URI.create(attachment.getUrl()).toURL(),
                                TicketHelper.getTicketDirectory(channel).resolve(fileName)
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                logLine.append(String.join(", ", fileNames))
                        .append("]");
            }

            TicketHelper.logTicketAction(channel, logLine.toString());
        }

        @Override
        public void onMessageDelete(@NotNull MessageDeleteEvent event) {
            if (!ServerSettings.isTicketArchiving(event.getGuild())) return;
            if (!(event.getChannel() instanceof TextChannel channel)) return;
            if (!TicketHelper.isLoggedTicket(channel)) return;

            TicketHelper.logTicketAction(channel, "Message (ID: %s) was deleted.".formatted(event.getMessageId()));
        }
    }

    public static class Action extends ListenerAdapter {
        @Override
        public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
            if (event.getInteraction().getCustomId().equals("ticket-close")) handleTicketClose(event);
        }

        public void handleTicketClose(@NotNull ButtonInteractionEvent event) {
            if (!(event.getChannel() instanceof TextChannel channel)) return;

            // Ignore exceptions, channel is probably gone
            event.reply("Deleting...").setEphemeral(true).queue(interactionHook -> {}, throwable -> {});

            TicketHelper.closeTicket(channel, event.getMember());
        }
    }
}
