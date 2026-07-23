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

package org.geysermc.discordbot.permissions;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    public static boolean hasPermission(Member member, MemberRolePermissions permission) {
        if (member.isOwner()) return true;
        if (List.of(GeyserBot.storageManager.getMemberPermissions(member)).contains(permission)) return true;
        for (Role role : member.getUnsortedRoles()) {
            if (hasPermission(role, permission)) return true;
        }
        return false;
    }

    public static boolean hasPermission(Role role, MemberRolePermissions permission) {
        return List.of(GeyserBot.storageManager.getRolePermissions(role)).contains(permission);
    }

    public static boolean hasPermission(User user, UserPermissions permission) {
        return List.of(GeyserBot.storageManager.getUserPermissions(user)).contains(permission);
    }

    public static void grantPermission(Member member, MemberRolePermissions permission) {
        List<MemberRolePermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getMemberPermissions(member)));
        permissions.add(permission);
        GeyserBot.storageManager.setMemberPermissions(member, permissions.toArray(MemberRolePermissions[]::new));
    }

    public static void grantPermission(Role role, MemberRolePermissions permission) {
        List<MemberRolePermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getRolePermissions(role)));
        permissions.add(permission);
        GeyserBot.storageManager.setRolePermissions(role, permissions.toArray(MemberRolePermissions[]::new));
    }

    public static void grantPermission(User user, UserPermissions permission) {
        List<UserPermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getUserPermissions(user)));
        permissions.add(permission);
        GeyserBot.storageManager.setUserPermissions(user, permissions.toArray(UserPermissions[]::new));
    }

    public static void revokePermission(Member member, MemberRolePermissions permission) {
        List<MemberRolePermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getMemberPermissions(member)));
        permissions.remove(permission);
        GeyserBot.storageManager.setMemberPermissions(member, permissions.toArray(MemberRolePermissions[]::new));
    }

    public static void revokePermission(Role role, MemberRolePermissions permission) {
        List<MemberRolePermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getRolePermissions(role)));
        permissions.remove(permission);
        GeyserBot.storageManager.setRolePermissions(role, permissions.toArray(MemberRolePermissions[]::new));
    }

    public static void revokePermission(User user, UserPermissions permission) {
        List<UserPermissions> permissions = new ArrayList<>();
        permissions.addAll(List.of(GeyserBot.storageManager.getUserPermissions(user)));
        permissions.remove(permission);
        GeyserBot.storageManager.setUserPermissions(user, permissions.toArray(UserPermissions[]::new));
    }
}
