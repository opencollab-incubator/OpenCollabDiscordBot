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

package org.geysermc.discordbot.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {
    private static final List<LocalDate> HOLIDAYS = new ArrayList<>();

    static {
        HOLIDAYS.add(LocalDate.of(0, Month.JANUARY, 1));
        HOLIDAYS.add(LocalDate.of(0, Month.MARCH, 15));
        HOLIDAYS.add(LocalDate.of(0, Month.APRIL, 3));
        HOLIDAYS.add(LocalDate.of(0, Month.APRIL, 5));
        HOLIDAYS.add(LocalDate.of(0, Month.APRIL, 6));
        HOLIDAYS.add(LocalDate.of(0, Month.MAY, 4));
        HOLIDAYS.add(LocalDate.of(0, Month.MAY, 25));
        HOLIDAYS.add(LocalDate.of(0, Month.JUNE, 21));
        HOLIDAYS.add(LocalDate.of(0, Month.AUGUST, 31));
        HOLIDAYS.add(LocalDate.of(0, Month.DECEMBER, 24));
        HOLIDAYS.add(LocalDate.of(0, Month.DECEMBER, 25));
        HOLIDAYS.add(LocalDate.of(0, Month.DECEMBER, 26));
        HOLIDAYS.add(LocalDate.of(0, Month.DECEMBER, 31));
    }

    public static boolean isWeekday() {
        return LocalDateTime.now().getDayOfWeek().getValue() < 6;
    }

    public static boolean isWorkingHours() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() >= 9 && now.getHour() < 17;
    }

    public static boolean isHoliday() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = now.toLocalDate();

        for (LocalDate holiday : HOLIDAYS) {
            if (currentDate.getMonthValue() == holiday.getMonthValue() && currentDate.getDayOfMonth() == holiday.getDayOfMonth()) return true;
        }

        return false;
    }
}
