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

package org.geysermc.discordbot.http.html;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.util.HtmlUtils;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Element implements HtmlRenderable {
    private final Map<String, String> fields = new LinkedHashMap<>();
    private final List<Object> innerHtml = new ArrayList<>();
    private final List<String> classes = new ArrayList<>();
    private final String elementType;

    protected Element(String elementType) {
        this.elementType = elementType;
    }

    public Element addField(String key, String value) {
        fields.put(key, value);
        return this;
    }

    public Element addInnerElement(String element) {
        innerHtml.add(element);
        return this;
    }

    public Element addSafeInnerElement(String element) {
        innerHtml.add(HtmlUtils.htmlEscape(element));
        return this;
    }

    public Element addInnerElement(HtmlRenderable element) {
        innerHtml.add(element);
        return this;
    }

    public Element addClass(String clazz) {
        classes.addAll(List.of(clazz.split(" ")));
        return this;
    }

    public Element removeClass(String clazz) {
        classes.removeAll(List.of(clazz.split(" ")));
        return this;
    }

    public List<String> getClasses() {
        return classes;
    }

    public Element setColor(Color color) {
        return setColor(color, false);
    }

    public Element setColor(Color color, boolean hoverToDefault) {
        if (color != null) {
            this.addClass("text-[#" + Integer.toHexString(color.getRGB()).substring(2) + "]");
            if (hoverToDefault) this.addClass("hover:text-inherit transition");
        }
        return this;
    }

    @Override
    public String render(HttpServletRequest request, OAuth2User principal) {
        Map<String, String> resultFields = new LinkedHashMap<>(fields);
        if (!classes.isEmpty()) resultFields.put("class", String.join(" ", classes));

        if (innerHtml.isEmpty() && elementType.equals("img")) {
            return "<%s %s />".formatted(
                    elementType, resultFields.entrySet().stream().map(entry ->
                            entry.getKey() + "=\"" + entry.getValue() + "\"").collect(Collectors.joining(" "))
            );
        }

        return "<%s %s>%s</%s>".formatted(
                elementType, resultFields.entrySet().stream().map(entry ->
                        entry.getKey() + "=\"" + entry.getValue() + "\"").collect(Collectors.joining(" ")),
                innerHtml.stream().map(obj -> {
                    if (obj instanceof HtmlRenderable renderable) {
                        return renderable.render(request, principal);
                    }
                    return obj.toString();
                }).collect(Collectors.joining()),
                elementType
        );
    }

    public static Element of(String elementType) {
        return new Element(elementType);
    }

    public static Element of(String elementType, String clazz) {
        return of(elementType)
                .addClass(clazz);
    }

    public static Element img(String src, String alt, String clazz) {
        return of("img")
                .addField("src", src)
                .addField("alt", alt)
                .addClass(clazz);
    }

    public static Element div(String clazz) {
        return of("div")
                .addClass(clazz);
    }
}
