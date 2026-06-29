package org.finos.rune.serialization;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import serialization.test.passing.basic.BasicList;
import serialization.test.passing.basic.Root;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuneJsonDeserializerTest {

    private static final String JSON_FILE_PATH = "src/test/resources/rune-deserialization-test/basic/basic-types-list.json";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new RuneJsonObjectMapper();
    }

    @Test
    void shouldDeserializeBasicTypesList() throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));

        Root root = objectMapper.readValue(jsonString, Root.class);

        assertNotNull(root, "Deserialized Root should not be null");

        BasicList basicList = root.getBasicList();
        assertNotNull(basicList, "BasicList should not be null");

        List<? extends Boolean> booleanTypes = basicList.getBooleanTypes();
        assertEquals(3, booleanTypes.size());
        assertEquals(true, booleanTypes.get(0));
        assertEquals(false, booleanTypes.get(1));
        assertEquals(true, booleanTypes.get(2));

        List<? extends BigDecimal> numberTypes = basicList.getNumberTypes();
        assertEquals(3, numberTypes.size());
        assertEquals(new BigDecimal("123.456"), numberTypes.get(0));
        assertEquals(new BigDecimal("789"), numberTypes.get(1));
        assertEquals(new BigDecimal("345.123"), numberTypes.get(2));

        List<? extends BigDecimal> parameterisedNumberTypes = basicList.getParameterisedNumberTypes();
        assertEquals(3, parameterisedNumberTypes.size());
        assertEquals(new BigDecimal("123.99"), parameterisedNumberTypes.get(0));
        assertEquals(new BigDecimal("456"), parameterisedNumberTypes.get(1));
        assertEquals(new BigDecimal("99.12"), parameterisedNumberTypes.get(2));

        List<? extends String> parameterisedStringTypes = basicList.getParameterisedStringTypes();
        assertEquals(3, parameterisedStringTypes.size());
        assertEquals("abcDEF", parameterisedStringTypes.get(0));
        assertEquals("foo", parameterisedStringTypes.get(1));
        assertEquals("foo", parameterisedStringTypes.get(2));

        List<? extends String> stringTypes = basicList.getStringTypes();
        assertEquals(3, stringTypes.size());
        assertEquals("foo", stringTypes.get(0));
        assertEquals("bar", stringTypes.get(1));
        assertEquals("Baz123", stringTypes.get(2));

        List<? extends LocalTime> timeTypes = basicList.getTimeTypes();
        assertEquals(1, timeTypes.size());
        assertEquals(LocalTime.of(12, 0, 0), timeTypes.get(0));
    }
}
