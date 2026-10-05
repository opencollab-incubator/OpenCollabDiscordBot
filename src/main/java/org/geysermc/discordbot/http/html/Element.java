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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Element implements HtmlRenderable {
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Object> innerHtml = new ArrayList<>();
    private final List<String> classes = new ArrayList<>();
    private final String elementType;

    protected Element(String elementType) {
        this.elementType = elementType;
    }

    public Element addField(String key) {
        fields.put(key, null);
        return this;
    }

    public Element addField(String key, String value) {
        fields.put(key, value);
        return this;
    }

    public Element addField(String key, HtmlRenderable value) {
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
        Map<String, Object> resultFields = new LinkedHashMap<>(fields);
        if (!classes.isEmpty()) resultFields.put("class", String.join(" ", classes));

        if (innerHtml.isEmpty() && elementType.equals("img")) {
            return "<%s %s />".formatted(
                    elementType, resultFields.entrySet().stream().map(entry -> {
                        String value;
                        if (entry.getValue() == null) return entry.getKey();
                        if (entry.getValue() instanceof HtmlRenderable renderable) {
                            value = renderable.render(request, principal);
                        } else {
                            value = entry.getValue().toString();
                        }

                        return entry.getKey() + "=\"" + value + "\"";
                    }).collect(Collectors.joining(" "))
            );
        }

        return "<%s %s>%s</%s>".formatted(
                elementType, resultFields.entrySet().stream().map(entry -> {
                    String value;
                    if (entry.getValue() == null) return entry.getKey();
                    if (entry.getValue() instanceof HtmlRenderable renderable) {
                        value = renderable.render(request, principal).replace('"', '\'');
                    } else {
                        value = entry.getValue().toString();
                    }

                    return entry.getKey() + "=\"" + value + "\"";
                }).collect(Collectors.joining(" ")),
                innerHtml.isEmpty() ? "" : "\n" + innerHtml.stream().map(obj -> {
                    if (obj instanceof HtmlRenderable renderable) {
                        return renderable.render(request, principal);
                    }
                    return obj.toString();
                }).collect(Collectors.joining("\n")) + "\n",
                elementType
        );
    }

    @Override
    public String toString() {
        Map<String, Object> resultFields = new LinkedHashMap<>(fields);
        if (!classes.isEmpty()) resultFields.put("class", String.join(" ", classes));

        if (innerHtml.isEmpty() && elementType.equals("img")) {
            return "<%s %s />".formatted(
                    elementType, resultFields.entrySet().stream().map(entry -> {
                        String value;
                        if (entry.getValue() instanceof HtmlRenderable renderable) {
                            value = renderable.toString();
                        } else {
                            value = entry.getValue().toString();
                        }

                        return entry.getKey() + "=\"" + value + "\"";
                    }).collect(Collectors.joining(" "))
            );
        }

        return "<%s %s>\n%s</%s>".formatted(
                elementType, resultFields.entrySet().stream().map(entry -> {
                    String value;
                    if (entry.getValue() instanceof HtmlRenderable renderable) {
                        value = renderable.toString().replace('"', '\'');
                    } else {
                        value = entry.getValue().toString();
                    }

                    return entry.getKey() + "=\"" + value + "\"";
                }).collect(Collectors.joining(" ")),
                innerHtml.stream().map(obj -> {
                    if (obj instanceof HtmlRenderable renderable) {
                        return renderable.toString();
                    }
                    return obj.toString();
                }).collect(Collectors.joining("\n")),
                elementType
        );
    }

    public static String jsString(String value) {
        return jsString(value, true);
    }

    public static String jsString(String value, boolean doubleQuotes) {
        if (doubleQuotes) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        } else {
            return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
        }
    }

    public static Element of(String elementType) {
        return new Element(elementType);
    }

    public static Element of(String elementType, String clazz) {
        return of(elementType)
                .addClass(clazz);
    }

    public static Element img(String src, String alt) {
        return of("img")
                .addField("src", src)
                .addField("alt", alt);
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

    public static Element br() {
        return of("br");
    }

    public static Element script(String scriptText) {
        return of("script").addInnerElement(scriptText);
    }

    public static Element a(String href) {
        return of("a")
                .addClass("hover:text-blue-200")
                .addField("href", href);
    }

    public static Element p() {
        return of("p", "text-gray-700 dark:text-gray-300");
    }

    public static Element h1() {
        return of("h1", "text-4xl font-bold mb-4");
    }

    public static Element h2() {
        return of("h2", "text-3xl font-bold mb-4");
    }

    public static Element h3() {
        return of("h3", "text-2xl font-bold mb-2");
    }

    public static Element title(String title) {
        return of("title").addSafeInnerElement(title);
    }

    public static Element staticTimeDisplay(OffsetDateTime dateTime) {
        return Element.of("span").addClass("time-display")
                .addField("data-time", String.valueOf(dateTime.toEpochSecond()))
                .addInnerElement("Loading...");
    }

    public static Element staticTimeDisplay(Instant instant) {
        return Element.of("span").addClass("time-display")
                .addField("data-time", String.valueOf(instant.getEpochSecond()))
                .addInnerElement("Loading...");
    }

    public static Element dynamicTimeDisplay(Instant instant) {
        return Element.of("span").addClass("countdown")
                .addField("data-time", String.valueOf(instant.getEpochSecond()))
                .addInnerElement("Loading...");
    }
}
