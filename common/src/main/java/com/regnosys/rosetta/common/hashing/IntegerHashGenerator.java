package com.regnosys.rosetta.common.hashing;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 *   Implements integer hashcode generation for each Rosetta basic type
 */
public class IntegerHashGenerator extends RosettaBasicTypesHashGenerator<Integer> {

    /**
     * If hashcode is being used as a proxy to equality, then it is up to the user to ensure consistent character 
     * encoding.
     */
    @Override
    Integer handle(String string) {
        char[] value = string.toCharArray();

        int h = 0;
        int length = value.length;
        for (int i = 0; i < length; i++) {
            h = 31 * h + value[i];
        }

        return h;
    }

    @Override
    Integer handle(Integer integer) {
        return integer;
    }

    @Override
    Integer handle(LocalDate localDate) {
        int yearValue = localDate.getYear();
        int monthValue = localDate.getMonthValue();
        int dayValue = localDate.getDayOfMonth();
        return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
    }

    @Override
    Integer handle(LocalTime localTime) {
        long nod = localTime.toNanoOfDay();
        return (int) (nod ^ (nod >>> 32));
    }

    @Override
    Integer handle(LocalDateTime localDateTime) {
        return handle(localDateTime.toLocalDate()) ^ handle(localDateTime.toLocalTime());
    }

    @Override
    Integer handle(ZonedDateTime zonedDateTime) {
        return handle(zonedDateTime.toLocalDateTime()) ^ zonedDateTime.getOffset().getTotalSeconds() ^ handle(zonedDateTime.getZone().getId());
    }

    /**
     * The CDM working groups wishes to use the hascode as a proxy to equality, thus we ignore trailing zeros as we 
     * will want 2.0 and 2.00 to produce the same hashcode.
     */
    @Override
    Integer handle(BigDecimal bigDecimal) {
        return handle(bigDecimal.stripTrailingZeros().toPlainString());
    }
    
    @Override
    Integer handle(Boolean bool) {
        return bool ? 1231 : 1237;
    }
    
    @Override
    Integer handle(Enum<?> e) {
        return handle(e.name());
    }

}
