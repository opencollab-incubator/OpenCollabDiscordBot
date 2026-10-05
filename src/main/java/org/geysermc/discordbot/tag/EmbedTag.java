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

package org.geysermc.discordbot.tag;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.geysermc.discordbot.util.BotColors;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmbedTag extends Tag {
    private final String title, description, color, titleUrl, image;

    public EmbedTag(List<Button> buttons, String title, String description, String color, String titleUrl, String image) {
        super(buttons);
        this.title = title;
        this.description = description;
        this.color = color;
        this.titleUrl = titleUrl;
        this.image = image;
    }

    @Override
    public void append(MessageCreateBuilder builder) {
        Color rawColor;
        try {
            rawColor = Color.decode(this.color);
        } catch (NumberFormatException e) {
            try {
                rawColor = BotColors.valueOf(this.color.toUpperCase()).getColor();
            } catch (IllegalArgumentException ex) {
                rawColor = null;
            }
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        if (title != null && !title.isEmpty()) {
            components.add(TextDisplay.of("# " + title));
            components.add(Separator.createDivider(Separator.Spacing.LARGE));
        }

        components.add(TextDisplay.of(description));

        if (image != null && !image.isEmpty()) {
            components.add(MediaGallery.of(MediaGalleryItem.fromUrl(image)));
        }

        // TODO
//        if (!buttons.isEmpty()) {
//            components.add(Separator.createDivider(Separator.Spacing.SMALL));
//            components.add(ActionRow.of(buttons));
//        }

        builder.useComponentsV2()
                .setComponents(Container.of(components).withAccentColor(rawColor));
    }
}
