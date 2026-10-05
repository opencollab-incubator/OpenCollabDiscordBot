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

import net.dv8tion.jda.api.entities.User;

public abstract class Base extends HtmlPage {
    protected final User user;

    public Base(User user) {
        this.user = user;
    }

    @Override
    public void appendHead(Element head) {
        head.addInnerElement(Element.title("Open Collab Bot"));

        head.addInnerElement(Element.of("script")
                .addField("src", "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"));

        head.addInnerElement(Element.of("link")
                .addField("rel", "stylesheet")
                .addField("href", "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.3.0/css/all.min.css")
                .addField("integrity", "sha512-ApSLB1Pd3/bZN8fWB/RG9YhN/7bd9Hkf3AGaE2mPfebjrxagjuBtx2GcgdqIlJkUzwylBo61r9Xa9NmgBI0swA==")
                .addField("crossorigin", "anonymous")
                .addField("referrerpolicy", "no-referrer"));

        head.addInnerElement(Element.script("""
                  function formatCountdown(el) {
                    const targetSeconds = Number(el.dataset.time);
                    const nowSeconds = Date.now() / 1000;
                    let absoluteDiff = targetSeconds - nowSeconds;
                
                    let diff = Math.floor(absoluteDiff);
                
                    if (absoluteDiff <= 0) {
                      if (el.dataset.type === "relative") {
                          diff = Math.floor(-absoluteDiff);
                
                          const days = Math.floor(diff / 86400);
                          diff -= days * 86400;
                          const hours = Math.floor(diff / 3600);
                          diff -= hours * 3600;
                          const minutes = Math.floor(diff / 60);
                          diff -= minutes * 60;
                          const seconds = diff;
                
                          return {
                            expired: false,
                            text: `${days}d, ${hours}h, ${minutes}m, ${seconds + 1}s ago`
                          };
                      }
                      return { expired: true, text: "Time's up!" };
                    }
                
                    const days = Math.floor(diff / 86400);
                    diff -= days * 86400;
                    const hours = Math.floor(diff / 3600);
                    diff -= hours * 3600;
                    const minutes = Math.floor(diff / 60);
                    diff -= minutes * 60;
                    const seconds = diff;
                
                    return {
                      expired: false,
                      text: `In ${days}d, ${hours}h, ${minutes}m, ${seconds}s`
                    };
                  }
                
                  function formatDate(targetSeconds) {
                    return new Date(targetSeconds * 1000).toLocaleString();
                  }
                
                  function tick(el) {
                    const result = formatCountdown(el);
                    el.textContent = result.text;
                    el.classList.toggle('expired', result.expired);
                    return result.expired;
                  }
                
                  document.addEventListener("DOMContentLoaded", e => {
                    const oneshot = Array.from(document.querySelectorAll('.time-display'));
                
                    oneshot.forEach(el => el.textContent = formatDate(Number(el.dataset.time)));
                
                    const elements = Array.from(document.querySelectorAll('.countdown'));
                
                    elements.forEach(tick);
                
                    const intervalId = setInterval(() => {
                        let allExpired = true;
                        elements.forEach(el => {
                            const expired = tick(el);
                            if (!expired) allExpired = false;
                        });
                        if (allExpired) clearInterval(intervalId);
                    }, 1000);
                  });
                """));
    }

    @Override
    public void appendBody(Element body) {
        Element divContainer = Element.of("div")
                .addClass("container mx-auto");

        addContent(divContainer);

        Element mainContainer = Element.of("div")
                .addClass("flex flex-col h-screen")
                .addInnerElement(
                        Element.of("header")
                                .addClass("bg-gray-200 dark:bg-gray-900 text-black dark:text-white p-4 " +
                                        "shadow-sm border-b border-gray-300 dark:border-gray-800")
                                .addInnerElement(
                                        Element.of("div")
                                                .addClass("container mx-auto flex justify-between items-center")
                                                .addInnerElement(Element.a("/")
                                                        .addClass("flex items-center gap-2 text-2xl font-bold hover:text-blue-500 " +
                                                                "dark:hover:text-blue-400 transition")
                                                        .addInnerElement("Open Collab Bot"))
                                                .addInnerElement(
                                                        Element.of("nav")
                                                                .addClass("flex gap-1 items-center")
                                                                .addInnerElement(Element.a("/")
                                                                        .addClass("px-3 py-1.5 rounded-md hover:bg-white/10 transition")
                                                                        .addInnerElement("Home"))
                                                                .addInnerElement(Element.a("/appeals")
                                                                        .addClass("px-3 py-1.5 rounded-md hover:bg-white/10 transition")
                                                                        .addInnerElement("Appeals"))
                                                                .addInnerElement(Header.ELEMENT)
                                                )
                                )
                )
                .addInnerElement(
                        Element.of("main")
                                .addClass("flex-1 bg-gray-100 dark:bg-gray-800 text-black dark:text-white p-8 overflow-y-auto")
                                .addInnerElement(divContainer)
                );

        this.addInnerElement(mainContainer);
    }

    public abstract void addContent(Element container);
}
