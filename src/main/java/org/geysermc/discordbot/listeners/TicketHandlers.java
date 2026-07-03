package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
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
import net.dv8tion.jda.api.utils.FileUpload;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.ServerSettings;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.NetworkUtils;
import org.geysermc.discordbot.util.TicketData;
import org.geysermc.discordbot.util.TicketHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TicketHandlers {
    public static class Create extends ListenerAdapter {
        private static final String TICKET_CREATE_ID = "ticket-create";
        private static final String TICKET_CLIENT_SELECT_ID = "ticket-client-select";

        @Override
        public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
            if (!event.getInteraction().getCustomId().equals(TICKET_CREATE_ID)) return;

            TicketData data = getTicketData(event);
            if (data == null) return;
            if (data.clientRole() == null) {
                List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

                event.replyModal(Modal.create(TICKET_CLIENT_SELECT_ID, "Select client")
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
            if (!event.getInteraction().getCustomId().equals(TICKET_CLIENT_SELECT_ID)) return;

            List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

            Role selectedRole = event.getGuild().getRoleById(event.getValue("client-role").getAsStringList().getFirst());

            if (!clientRoles.contains(selectedRole)) {
                event.reply("You did not select a client role.").setEphemeral(true).queue();
            }

            commonHandle(event, new TicketData(selectedRole), event::replyEmbeds);
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

            ChannelAction<TextChannel> action = category.createTextChannel(getChannelNameFromTicketData(ticketData))
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
                TicketHelper.initTicket(channel, ticketData.clientRole().getName());

                TicketHelper.logTicketAction(
                        channel,
                        "Created ticket.\n" +
                                "- User: @" + event.getUser().getName() + " (ID: " + event.getUser().getId() + ")\n" +
                                "- Client: @" + ticketData.clientRole().getName() + " (ID: " + ticketData.clientRole().getId() + ")\n" +
                                "- Allowed Roles: " + (roleNames.isEmpty() ? "None" : String.join(", ", roleNames)) +
                                "- Channel ID: " + channel.getId()
                );

                ActionRow row = ActionRow.of(
                        Button.danger("ticket-close", "Close Ticket")
                );

                List<String> pings = new ArrayList<>();

                for (Role pingedRole : ServerSettings.getTicketPingedRoles(event.getGuild())) {
                    pings.add(pingedRole.getAsMention());
                }

                channel.sendMessage(String.join(" ", pings))
                        .setEmbeds(new EmbedBuilder()
                                .setTitle("Ticket created!")
                                .setDescription("Please describe your issue and be patient while we get back to you!")
                                .setColor(BotColors.SUCCESS.getColor())
                                .build())
                        .setComponents(row)
                        .queue();
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

        public TicketData getTicketData(@NotNull ButtonInteractionEvent event) {
            List<Role> clientRoles = ServerSettings.getClientRoles(event.getMember());

            if (clientRoles.isEmpty()) {
                event.reply("You are not an Open Collaboration client.").setEphemeral(true).queue();
                return null;
            } else if (clientRoles.size() == 1) {
                Role role = clientRoles.getFirst();
                return new TicketData(role, role.getName().toLowerCase().replace(' ', '-'));
            } else {
                return new TicketData(null, null);
            }
        }

        public String getChannelNameFromTicketData(TicketData data) {
            return data.clientId() + "-" + GeyserBot.storageManager.getAndIncrementTicketId(data.clientId());
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
