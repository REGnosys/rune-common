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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.regnosys.rosetta.common.hashing.ReferenceConfig;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.regnosys.rosetta.common.util.ClassPathUtils;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.process.PostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRunner.class);
    private static final String ROSETTA_FUNC_EVAL_METHOD_NAME = "evaluate";

    private final ExecutionDescriptor executionDescriptor;
    private final InstanceLoader instanceLoader;

    private final ClassLoader classLoader;
    private final ObjectMapper objectMapper;

    public FunctionRunner(ExecutionDescriptor executionDescriptor,
                          InstanceLoader instanceLoader,
                          ClassLoader classLoader,
                          ObjectMapper objectMapper) {
        this.executionDescriptor = executionDescriptor;
        this.instanceLoader = instanceLoader;
        this.classLoader = classLoader;
        this.objectMapper = objectMapper;
    }

    public <INPUT, OUTPUT> FunctionRunnerResult<INPUT, OUTPUT> run() throws ClassNotFoundException, IOException, InvocationTargetException, IllegalAccessException {
        LOGGER.info("Executing " + executionDescriptor.getGroup() + ":" + executionDescriptor.getName());

        String inputFile = executionDescriptor.getInputFile();
        String expectedOutputFile = executionDescriptor.getExpectedOutputFile();
        LOGGER.info("Output File:  " + expectedOutputFile);

        if (executionDescriptor.isNativeFunction()) {
            JsonNode jsonNode = objectMapper.readTree(loadURL(inputFile));

            Object actualOutput = postProcess(runNativeFunction(jsonNode, executionDescriptor.getExecutableFunctionClass()));
            String jsonActual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualOutput);

            if (expectedOutputFile == null) {
                return new FunctionRunnerResult(jsonNode, null, actualOutput, jsonActual, null);
            }

            Object expectedOutput;
            String jsonExpected;
            try {
                expectedOutput = objectMapper.readValue(loadURL(expectedOutputFile), getType(actualOutput));
                jsonExpected = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedOutput);
            } catch (Exception e) {
                LOGGER.error("Error getting expected output " + executionDescriptor.getGroup() + ":" + executionDescriptor.getName(), e);
                expectedOutput = null;
                jsonExpected = "";
            }
            return new FunctionRunnerResult(jsonNode, expectedOutput, actualOutput, jsonActual, jsonExpected);
        } else {
            Class<ExecutableFunction<INPUT, OUTPUT>> functionClass = loadExecutableFunctionClass(executionDescriptor.getExecutableFunctionClass());
            ExecutableFunction<INPUT, OUTPUT> instance = instanceLoader.createInstance(functionClass);

            INPUT input = objectMapper.readValue(loadURL(inputFile), instance.getInputType());
            OUTPUT actualOutput = postProcess(instance.execute(input));

            String jsonActual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualOutput);
            if (expectedOutputFile == null) {
                return new FunctionRunnerResult<>(input, null, actualOutput, jsonActual, null);
            }

            try {
                OUTPUT expectedOutput = objectMapper.readValue(loadURL(expectedOutputFile), instance.getOutputType());
                String jsonExpected = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedOutput);
                return new FunctionRunnerResult<>(input, expectedOutput, actualOutput, jsonActual, jsonExpected);
            } catch (IOException e) {
                LOGGER.warn("Unable to deserialise expected json file, proceeding without it.");
                // TODO: load the url into a string and print it here
                return new FunctionRunnerResult<>(input, null, actualOutput, jsonActual, "");
            }
        }
    }

    private Class<?> getType(Object actualOutput) {
        return RosettaModelObject.class.isInstance(actualOutput) ?
                ((RosettaModelObject) actualOutput).getType() :
                actualOutput.getClass();
    }

    private <OUTPUT> OUTPUT postProcess(OUTPUT actualOutput) {
        if (actualOutput instanceof RosettaModelObject) {
            PostProcessor postProcessor = instanceLoader.createInstance(PostProcessor.class);
            RosettaModelObject funcModelOutput = (RosettaModelObject) actualOutput;
            RosettaModelObjectBuilder instance = funcModelOutput.toBuilder();
            instance.prune();
            RosettaModelObjectBuilder postProcessedBuilder = postProcessor.postProcess(funcModelOutput.getType(), instance);
            RosettaModelObject postProcessed = postProcessedBuilder.build();
            return (OUTPUT) postProcessed;
        }
        return actualOutput;
    }

    private <INPUT> INPUT resolveReferences(INPUT input) {
        if (input instanceof List) {
            List<?> builderList = ((List<?>) input).stream().map(this::resolveReferences).collect(Collectors.toList());
            return (INPUT) builderList;
        } else if (input instanceof RosettaModelObject) {
            RosettaModelObjectBuilder builder = ((RosettaModelObject) input).toBuilder();
            ReferenceConfig resolverConfig = instanceLoader.createInstance(ReferenceConfig.class);
            new ReferenceResolverProcessStep(resolverConfig).runProcessStep(builder.getType(), builder);
            return (INPUT) builder.build();
        }
        return input;
    }

    private Object runNativeFunction(JsonNode jsonNode, String functionClassName) throws ClassNotFoundException, IOException, InvocationTargetException, IllegalAccessException {
        Class<?> functionClass = this.classLoader.loadClass(functionClassName);
        if (!checkFunctionIsRosettaFunction(functionClass)) {
            throw new IllegalArgumentException(String.format("Function %s is not defined in Rosetta.", functionClassName));
        }

        Object rosettaFunction = instanceLoader.createInstance(functionClass);

        if (null == rosettaFunction) {
            throw new IllegalArgumentException(String.format("Function %s cannot be created.", functionClassName));
        }

        Optional<Method> executeMethod = getEvaluateMethod(functionClass);

        if (!executeMethod.isPresent()) {
            throw new IllegalArgumentException(String.format("Function %s is not executable.", functionClassName));
        }

        Method method = executeMethod.get();
        Object[] argsList = getMethodArguments(method, jsonNode);
        return method.invoke(rosettaFunction, argsList);
    }

    private Optional<Method> getEvaluateMethod(Class<?> rosettaClass) {

        List<Method> methods = Arrays.stream(rosettaClass.getMethods())
                .filter(x -> Modifier.isPublic(x.getModifiers()))
                .filter(x -> x.getName().equals(ROSETTA_FUNC_EVAL_METHOD_NAME))
                .collect(Collectors.toList());

        if (methods.size() == 1) {
            return Optional.of(methods.get(0));
        }

        if (methods.isEmpty()) {
            return Optional.empty();
        }

        if (methods.stream().allMatch(x -> x.getParameterCount() == 1)) {
            return methods.stream().filter(x -> !x.getParameterTypes()[0].isAssignableFrom(Object.class)).findFirst();
        }

        throw new RuntimeException("Unable to find the evaluate function as multiple implementations found. "
                + methods.stream().map(Method::toString).collect(Collectors.joining(", ")));
    }

    private boolean checkFunctionIsRosettaFunction(Class<?> functionClass) {
        return RosettaFunction.class.isAssignableFrom(functionClass);
    }

    private Object[] getMethodArguments(Method method, JsonNode jsonNode) throws IOException, ClassNotFoundException {
        Type[] parameterTypes = method.getGenericParameterTypes();
        // single arg
        if (!jsonNode.isArray() && parameterTypes.length == 1 && !isList(parameterTypes[0])) {
            Class<?> parameterType = this.classLoader.loadClass(parameterTypes[0].getTypeName());
            return new Object[]{resolveReferences(objectMapper.treeToValue(jsonNode, parameterType))};
        } else {// multi args as array
            JsonNode[] jsonArrayNodes = Iterables.toArray(jsonNode, JsonNode.class);
            Object[] argsList = new Object[parameterTypes.length];

            if (parameterTypes.length == jsonArrayNodes.length) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    JavaType javaType =  objectMapper.getTypeFactory().constructType(parameterTypes[i]);
                    JsonParser jsonParser = objectMapper.treeAsTokens(jsonArrayNodes[i]);
                    argsList[i] = resolveReferences(objectMapper.readValue(jsonParser, javaType));
                }
            } else {
                throw new IllegalArgumentException(String.format("The function %s requires %s arguments, but %s was supplied in the json array.",
                        method.getName(), parameterTypes.length, jsonArrayNodes.length));
            }
            return argsList;
        }
    }

    private boolean isList(Type parameterType) {
        JavaType javaType =  objectMapper.getTypeFactory().constructType(parameterType);
        return javaType.getRawClass().isAssignableFrom(List.class);
    }

    @SuppressWarnings("unchecked")
    private <INPUT, OUTPUT> Class<ExecutableFunction<INPUT, OUTPUT>> loadExecutableFunctionClass(String testClass) throws ClassNotFoundException {
        return (Class<ExecutableFunction<INPUT, OUTPUT>>) this.classLoader.loadClass(testClass);
    }

    protected URL loadURL(String inputFile) throws MalformedURLException {
        Optional<Path> inputPath = ClassPathUtils.loadFromClasspath(inputFile, this.classLoader).findFirst();
        if (!inputPath.isPresent()) {
            throw new IllegalArgumentException("Could not load " + inputFile);
        }
        return inputPath.get().toUri().toURL();
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    public interface InstanceLoader {
        <T> T createInstance(Class<T> executableFunctionClass) throws RuntimeException;
    }

    public class FunctionRunnerResult<INPUT, OUTPUT> {
        private final INPUT input;
        private final OUTPUT expectedOutput;
        private final OUTPUT actualOutput;
        private final String jsonActual;
        private final String jsonExpected;
        private final boolean success;

        public FunctionRunnerResult(INPUT input, OUTPUT expectedOutput, OUTPUT actualOutput, String jsonActual, String jsonExpected) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.jsonActual = jsonActual;
            this.jsonExpected = jsonExpected;
            this.success = jsonActual.equals(jsonExpected);
        }

        public boolean isSuccess() {
            return success;
        }

        public INPUT getInput() {
            return input;
        }

        public OUTPUT getExpectedOutput() {
            return expectedOutput;
        }

        public OUTPUT getActualOutput() {
            return actualOutput;
        }

        public String getJsonActual() {
            return jsonActual;
        }

        public String getJsonExpected() {
            return jsonExpected;
        }
    }
}
