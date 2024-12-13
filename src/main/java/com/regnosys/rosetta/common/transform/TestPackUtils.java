package com.regnosys.rosetta.common.transform;

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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestPackUtils {

    public static final Path PROJECTION_PATH = Paths.get(TransformType.PROJECTION.getResourcePath());
    public static final Path PROJECTION_CONFIG_PATH_WITHOUT_ISO20022 = PROJECTION_PATH.resolve("config");
    public static final Path REPORT_CONFIG_PATH = Paths.get(TransformType.REPORT.getResourcePath()).resolve("config");
    public static final Path INGEST_CONFIG_PATH = Paths.get(TransformType.TRANSLATE.getResourcePath()).resolve("config");

    public static TestPackModel createTestPack(String testPackName, TransformType transformType, String formattedFunctionName, List<TestPackModel.SampleModel> sampleModels) {
        return new TestPackModel(createTestPackId(transformType, formattedFunctionName, testPackName), createPipelineId(transformType, formattedFunctionName), testPackName, sampleModels);
    }

    private static String createTestPackId(TransformType transformType, String formattedFunctionName, String testPackName) {
        return String.format("test-pack-%s-%s-%s", transformType.name().toLowerCase(), formattedFunctionName, testPackName.replace(" ", "-").toLowerCase());
    }

    private static String createPipelineId(TransformType transformType, String formattedFunctionName) {
        return String.format("pipeline-%s-%s", transformType.name().toLowerCase(), formattedFunctionName);
    }

    public static PipelineModel createPipeline(TransformType transformType,
                                               String functionQualifiedName,
                                               String displayName,
                                               String formattedFunctionName,
                                               String inputType,
                                               String outputType,
                                               String upstreamPipelineId,
                                               PipelineModel.Serialisation inputSerialisation,
                                               PipelineModel.Serialisation outputSerialisation) {
        String pipelineId = createPipelineId(transformType, formattedFunctionName);
        PipelineModel.Transform transform = new PipelineModel.Transform(transformType, functionQualifiedName, inputType, outputType);
        return new PipelineModel(pipelineId, displayName, transform, upstreamPipelineId, inputSerialisation, outputSerialisation);
    }

    public static List<PipelineModel> getPipelineModels(Path resourcePath, ClassLoader classLoader, ObjectMapper jsonObjectMapper) {
        List<URL> pipelineFiles = findPaths(resourcePath, classLoader, "pipeline-.*\\.json");
        return pipelineFiles.stream()
                .map(url -> readFile(url, jsonObjectMapper, PipelineModel.class))
                .collect(Collectors.toList());
    }

    public static PipelineModel getPipelineModel(List<PipelineModel> pipelineModels, String functionName) {
        return pipelineModels.stream()
                .filter(p -> p.getTransform().getFunction().equals(functionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No PipelineModel found with function name %s", functionName)));
    }

    public static List<TestPackModel> getTestPackModels(Path resourcePath, ClassLoader classLoader, ObjectMapper jsonObjectMapper) {
        List<URL> testPackUrls = findPaths(resourcePath, classLoader, "test-pack-.*\\.json");
        return testPackUrls.stream()
                .map(url -> readFile(url, jsonObjectMapper, TestPackModel.class))
                .collect(Collectors.toList());
    }

    public static List<TestPackModel> getTestPackModels(List<TestPackModel> testPackModels, String pipelineId) {
        return testPackModels.stream()
                .filter(testPackModel -> testPackModel.getPipelineId() != null)
                .filter(testPackModel -> testPackModel.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
    }

    public static Optional<ObjectMapper> getObjectMapper(PipelineModel.Serialisation serialisation) {
        if (serialisation != null && serialisation.getFormat() == PipelineModel.Serialisation.Format.XML) {
            URL xmlConfigPath = Objects.requireNonNull(Resources.getResource(serialisation.getConfigPath()));
            try (InputStream inputStream = xmlConfigPath.openStream()) {
                return Optional.of(RosettaObjectMapperCreator.forXML(inputStream).create());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    public static Optional<ObjectWriter> getObjectWriter(PipelineModel.Serialisation serialisation) {
        return getObjectMapper(serialisation).map(ObjectMapper::writerWithDefaultPrettyPrinter);
    }

    @Deprecated
    public static String getProjectionTestPackName(String reportId) {
        return "test-pack-projection-" + reportId + "-report-to-iso20022.*\\.json";
    }

    public static String getReportTestPackName(String reportId) {
        return "test-pack-report-" + reportId + ".*\\.json";
    }

    public static List<URL> findPaths(Path basePath, ClassLoader classLoader, String fileName) {
        List<URL> expectations = ClassPathUtils
                .findPathsFromClassPath(Collections.singletonList(UrlUtils.toPortableString(basePath)),
                        fileName,
                        Optional.empty(),
                        classLoader)
                .stream()
                .map(UrlUtils::toUrl)
                .collect(Collectors.toList());
        return ImmutableList.copyOf(expectations);
    }

    public static <T> T readFile(URL u, ObjectMapper mapper, Class<T> clazz) {
        try (Reader src = UrlUtils.openURL(u)) {
            return mapper.readValue(src, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read url %s", u), e);
        }
    }
}