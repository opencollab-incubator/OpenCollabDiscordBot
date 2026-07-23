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

package org.geysermc.discordbot.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;

import java.util.function.Consumer;

public enum BotServers {
    OPEN_COLLAB("819298245640716289", false),
    OPEN_COLLAB_MEMBERS("1503784549354569880", true),
    GEYSER("613163671870242838", false),
    CLOUDBURST("393465748535640064", false),
    BOAR("1504901803236659351", false);

    private final String serverId;
    private final boolean hiddenUnlessMember;

    BotServers(String serverId, boolean hiddenUnlessMember) {
        this.serverId = serverId;
        this.hiddenUnlessMember = hiddenUnlessMember;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isHidden(JDA jda, User user) {
        Guild server = jda.getGuildById(serverId);
        if (server == null) return false;

        return !hiddenUnlessMember || (user != null && server.isMember(user));
    }

    public Guild get(JDA jda) {
        return jda.getGuildById(serverId);
    }

    public static boolean isHidden(Guild guild, User user) {
        if (guild == null) return false;

        for (BotServers server : values()) {
            if (!server.serverId.equals(guild.getId())) continue;

            return server.isHidden(guild.getJDA(), user);
        }

        return false;
    }

    public static void executeFromAll(Consumer<Guild> action) {
        for (BotServers servers : values()) {
            Guild server = GeyserBot.getJDA().getGuildById(servers.getServerId());
            if (server == null) continue;
            action.accept(server);
        }
    }

    public static void executeFromAll(Guild guild, Consumer<Guild> action) {
        for (BotServers servers : values()) {
            if (servers.getServerId().equals(guild.getId())) action.accept(guild);
            else {
                Guild server = guild.getJDA().getGuildById(servers.getServerId());
                if (server == null) continue;
                action.accept(server);
            }
        }
    }

    public static void executeFromAll(Member member, Consumer<Member> action) {
        for (BotServers servers : values()) {
            if (servers.getServerId().equals(member.getUser().getId())) action.accept(member);
            else {
                Guild server = member.getJDA().getGuildById(servers.getServerId());
                if (server == null) continue;
                Member serverMember = server.getMember(member);
                if (serverMember == null) continue;
                action.accept(serverMember);
            }
        }
    }
}
