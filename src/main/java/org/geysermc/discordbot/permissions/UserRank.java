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

import java.awt.*;
import java.util.EnumSet;

public enum UserRank {
    DONATOR("Donator", "bolt-lightning", Color.decode("#f785ea")),
    NOTABLE("Notable", "certificate", Color.decode("#f0441d")),
    MODERATOR("Moderator", "hammer", Color.decode("#36ca99")),
    OPEN_COLLAB_STAFF("Open Collaboration Staff", "box-open", Color.decode("#f8d040"));

    private final String name;
    private final String icon;
    private final Color color;
    private final EnumSet<UserPermissions> permissions;

    UserRank(String name, String icon, Color color, UserPermissions... permissions) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.permissions = permissions.length == 0 ? EnumSet.noneOf(UserPermissions.class) : EnumSet.of(permissions[0], permissions);
    }

    public String getIcon() {
        return this.icon;
    }

    public Color getColor() {
        return this.color;
    }

    public EnumSet<UserPermissions> getPermissions() {
        return this.permissions;
    }

    @Override
    public String toString() {
        return name;
    }
}
