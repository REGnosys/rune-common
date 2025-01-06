package com.regnosys.rosetta.common.testing;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class ExecutionDescriptor {

    private String group;
    private String name;
    private String description;
    private String markDownFile;
    private String inputFile;
    private String expectedOutputFile;
    private String executableFunctionClass;
    private boolean nativeFunction;

    public static List<ExecutionDescriptor> loadExecutionDescriptor(ObjectMapper objectMapper, URL url) {
        try {
            return objectMapper.readValue(url, new TypeReference<List<ExecutionDescriptor>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load expectations from " + url.toString(), e);
        }
    }

    public static List<ExecutionDescriptor> loadExecutionDescriptor(ObjectMapper objectMapper, String resourceName, InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, new TypeReference<List<ExecutionDescriptor>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load expectations " + resourceName, e);
        }
    }

    public ExecutionDescriptor() {
    }

    public boolean isNativeFunction() {
        return nativeFunction;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getExpectedOutputFile() {
        return expectedOutputFile;
    }

    public String getExecutableFunctionClass() {
        return executableFunctionClass;
    }

    public String getMarkDownFile() {
        return markDownFile;
    }
}
