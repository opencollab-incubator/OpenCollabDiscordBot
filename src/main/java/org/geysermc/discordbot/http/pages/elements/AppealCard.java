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

package org.geysermc.discordbot.http.pages.elements;

import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.html.HtmlRenderable;
import org.geysermc.discordbot.storage.Appeal;

public class AppealCard extends Element {
    public AppealCard(Appeal appeal, User resolvedUser, boolean addActionButtons) {
        super("div");

        Element elements = Element.div("flex min-w-0 flex-col justify-center")
                .addInnerElement(
                        Element.of("h3", "mt-0 mb-2 text-lg font-semibold")
                                .addInnerElement(new FAIcon(appeal.state().icon()))
                                .addInnerElement(" ")
                                .addInnerElement(addActionButtons ? "%s's Appeal".formatted(resolvedUser.getEffectiveName()) : appeal.state().title())
                )
                .addInnerElement(
                        Element.of("p", "text-sm leading-relaxed text-gray-700 dark:text-gray-300")
                                .addInnerElement(Element.of("p")
                                        .addInnerElement("Submitted On ")
                                        .addInnerElement(Element.staticTimeDisplay(appeal.creationTime())))
                                .addInnerElement(
                                        appeal.actionTime() == null ?
                                                HtmlRenderable.EMPTY :
                                                Element.of("p")
                                                .addInnerElement("Action Taken On ")
                                                .addInnerElement(Element.staticTimeDisplay(appeal.actionTime()))
                                )
                                .addSafeInnerElement("Message: " + appeal.message())
                );


        String bgClasses = switch(appeal.state()) {
            case PENDING -> "bg-gray-200 dark:bg-gray-900";
            case APPROVED -> "bg-green-200 dark:bg-green-900 border-2 border-green-300 dark:border-green-800";
            case DENIED -> "bg-red-200 dark:bg-red-900 border-2 border-red-300 dark:border-red-800";
        };


        this.addClass("flex grow w-full flex-col overflow-hidden rounded-xl shadow-sm mb-2");
        this.addClass(bgClasses);
        this.addInnerElement(
                Element.of("div")
                        .addClass("flex items-stretch gap-4 p-4")
                        .addInnerElement(elements)
        );

        if (addActionButtons && appeal.state().equals(Appeal.State.PENDING)) {
            Element buttonContainer = Element.div("flex gap-2 px-4 pb-4");

            String handler = """
                    let code, message;
                    if (data.status) code = data.status;
                    else code = data.code;
                    if (data.error) message = data.error;
                    else message = data.message;
                    
                    const messageElement = document.getElementById('message-element');
                    
                    messageElement.classList.remove('bg-red-200');
                    messageElement.classList.remove('dark:bg-red-900');
                    messageElement.classList.remove('border-red-300');
                    messageElement.classList.remove('dark:border-red-800');
                    
                    messageElement.classList.remove('bg-orange-200');
                    messageElement.classList.remove('dark:bg-orange-900');
                    messageElement.classList.remove('border-orange-300');
                    messageElement.classList.remove('dark:border-orange-800');
                    
                    messageElement.classList.remove('bg-yellow-200');
                    messageElement.classList.remove('dark:bg-yellow-900');
                    messageElement.classList.remove('border-yellow-300');
                    messageElement.classList.remove('dark:border-yellow-800');
                    
                    messageElement.classList.remove('bg-green-200');
                    messageElement.classList.remove('dark:bg-green-900');
                    messageElement.classList.remove('border-green-300');
                    messageElement.classList.remove('dark:border-green-800');
                    
                    messageElement.classList.remove('bg-blue-200');
                    messageElement.classList.remove('dark:bg-blue-900');
                    messageElement.classList.remove('border-blue-300');
                    messageElement.classList.remove('dark:border-blue-800');
                    
                    if (code === 403) {
                        messageElement.classList.add('bg-red-200');
                        messageElement.classList.add('dark:bg-red-900');
                        messageElement.classList.add('border-red-300');
                        messageElement.classList.add('dark:border-red-800');
                    } else if (code === 400) {
                        messageElement.classList.add('bg-orange-200');
                        messageElement.classList.add('dark:bg-orange-900');
                        messageElement.classList.add('border-orange-300');
                        messageElement.classList.add('dark:border-orange-800');
                    } else if (code === 409) {
                        messageElement.classList.add('bg-yellow-200');
                        messageElement.classList.add('dark:bg-yellow-900');
                        messageElement.classList.add('border-yellow-300');
                        messageElement.classList.add('dark:border-yellow-800');
                    } else if (code === 200) {
                        messageElement.classList.add('bg-green-200');
                        messageElement.classList.add('dark:bg-green-900');
                        messageElement.classList.add('border-green-300');
                        messageElement.classList.add('dark:border-green-800');
                    } else {
                        messageElement.classList.add('bg-blue-200');
                        messageElement.classList.add('dark:bg-blue-900');
                        messageElement.classList.add('border-blue-300');
                        messageElement.classList.add('dark:border-blue-800');
                    }
                    messageElement.innerHTML = message;
                    messageElement.classList.remove('opacity-0');
                    if (messageElement.dataset.scheduleId) clearTimeout(messageElement.dataset.scheduleId);
                    messageElement.dataset.scheduleId = setTimeout(() => {
                        messageElement.classList.add('opacity-0');
                        messageElement.innerHTML = '';
                        messageElement.dataset.scheduleId = undefined;
                    }, 5000);
                    """;

            buttonContainer.addInnerElement(new PostRequestButton("Approve", "/api/appeals/approve/" + appeal.id(), handler, "green"));
            buttonContainer.addInnerElement(new PostRequestButton("Deny", "/api/appeals/deny/" + appeal.id(), handler, "red"));

            this.addInnerElement(buttonContainer);
        }
    }
}
