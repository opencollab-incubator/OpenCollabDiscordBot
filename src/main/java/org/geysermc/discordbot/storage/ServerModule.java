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

package org.geysermc.discordbot.storage;

import net.dv8tion.jda.api.entities.Guild;
import org.geysermc.discordbot.GeyserBot;

import java.util.*;

public class ServerModule {
    private static final Map<String, ServerModule> REGISTERED_MODULES = new LinkedHashMap<>();

    public static Map<String, ServerModule> getRegisteredModules() {
        return Collections.unmodifiableMap(REGISTERED_MODULES);
    }

    public static ServerModule get(String key) {
        return REGISTERED_MODULES.get(key);
    }

    public static final ServerModule BUTTON_ROLES = register("oc_bot:button_roles", "Button Roles");
    public static final ServerModule EXPLOIT_REPORT = register("oc_bot:exploit_report", "Exploit Reports");
    public static final ServerModule FORUMS = register("oc_bot:forums", "Forums");
    public static final ServerModule GEYSER_DUMPS = register("oc_bot:geyser_dumps", "Geyser Dump Checking");
    public static final ServerModule LEVELS = register("oc_bot:levels", "Leveling System");
    public static final ServerModule PREVIEWS = register("oc_bot:previews", "Previews");
    public static final ServerModule TICKETS = register("oc_bot:tickets", "Tickets");

    private final String key;
    private final String name;

    public ServerModule(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj instanceof ServerModule that) return that.key.equals(this.key);
        return false;
    }

    public boolean isEnabled(Guild guild) {
        if (guild == null) return true; // Enabled in DMs by default
        return getEnabledModules(guild).contains(this);
    }

    public boolean setEnabled(Guild guild, boolean val) {
        String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), "enabled-modules");
        List<String> enabledModules;
        if (id == null) enabledModules = new ArrayList<>();
        else enabledModules = new ArrayList<>(List.of(id.split(",")));

        if (val) {
            if (enabledModules.contains(key)) return false;
            enabledModules.add(key);
        } else {
            if (!enabledModules.contains(key)) return false;
            enabledModules.remove(key);
        }
        GeyserBot.storageManager.setServerPreference(guild.getIdLong(), "enabled-modules", String.join(",", enabledModules));
        return true;
    }

    private static ServerModule register(String key, String name) {
        ServerModule module = new ServerModule(key, name);
        REGISTERED_MODULES.put(key, module);
        return module;
    }

    public static List<ServerModule> getEnabledModules(Guild guild) {
        String id = GeyserBot.storageManager.getServerPreference(guild.getIdLong(), "enabled-modules");
        if (id == null) return List.of();
        return List.of(id.split(",")).stream().map(REGISTERED_MODULES::get)
                .filter(Objects::nonNull).toList();
    }
}
