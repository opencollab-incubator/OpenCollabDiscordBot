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

import java.util.ArrayList;
import java.util.List;

public enum UserPermissions {
    VIEW_ALL_APPEALS,
    MANAGE_ALL_APPEALS,
    CONFIGURE_ALL_SERVERS;

    public static int getRawValue(UserPermissions... permissions) {
        int perms = 0;
        for (UserPermissions permission : permissions) {
            perms += Math.pow(2, permission.ordinal());
        }
        return perms;
    }

    public static UserPermissions[] fromRawValue(int perms) {
        List<UserPermissions> permissions = new ArrayList<>();
        int i = 0;
        while (perms > 0) {
            if ((perms & 1) == 1) permissions.add(UserPermissions.values()[i++]);
            perms >>= 1;
        }
        return permissions.toArray(new UserPermissions[0]);
    }
}
