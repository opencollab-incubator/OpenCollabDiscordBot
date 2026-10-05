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

package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BoosterListener extends Handler {
    private static final Logger log = LoggerFactory.getLogger(BoosterListener.class);

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {
            for (Member member : guild.getMembers()) {
                List<MemberRank> ranks = List.of(GeyserBot.storageManager.getMemberRanks(member));
                if (ranks.contains(MemberRank.BOOSTER) && !member.isBoosting()) {
                    PermissionManager.revokeRank(member, MemberRank.BOOSTER);
                } else if (!ranks.contains(MemberRank.BOOSTER) && member.isBoosting()) {
                    PermissionManager.grantRank(member, MemberRank.BOOSTER);
                }
            }
        }
    }

    @Override
    public void onGuildMemberUpdateBoostTime(@NonNull GuildMemberUpdateBoostTimeEvent event) {
        if (event.getOldTimeBoosted() == null && event.getNewTimeBoosted() != null) {
            // Is now boosting
            PermissionManager.grantRank(event.getMember(), MemberRank.BOOSTER);
        } else if (event.getOldTimeBoosted() != null && event.getNewTimeBoosted() == null) {
            // No longer boosting
            PermissionManager.revokeRank(event.getMember(), MemberRank.BOOSTER);
        }
    }
}
