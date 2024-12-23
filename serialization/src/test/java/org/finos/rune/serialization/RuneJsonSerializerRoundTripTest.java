package org.finos.rune.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RuneJsonSerializerRoundTripTest {
    public static final String TEST_TYPE = "rune-serializer-round-trip-test";
    private static DynamicCompiledClassLoader dynamicCompiledClassLoader;
    private RuneJsonSerializer runeJsonSerializer;

    private static CodeGeneratorTestHelper helper;

    @BeforeAll
    static void beforeAll() {
        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
        dynamicCompiledClassLoader = new DynamicCompiledClassLoader();
    }

    @BeforeEach
    void setUp() {
        runeJsonSerializer = new RuneJacksonJsonSerializer();
        ObjectMapper objectMapper = ((RuneJacksonJsonSerializer) runeJsonSerializer).getObjectMapper();
        objectMapper.setTypeFactory(objectMapper.getTypeFactory().withClassLoader(dynamicCompiledClassLoader));
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("testCases")
    public void testSerializationRoundTrip(String group, String testCaseName, Class<? extends RosettaModelObject> rosettaRootType, String jsonString) {
        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(jsonString, rosettaRootType);
        String serializedjsonString = runeJsonSerializer.toJson(deserializedObject);
        assertEquals(jsonString, serializedjsonString, testCaseName + ": Serialization round trip failed");
    }

    public static Stream<Arguments> testCases() {
        return groups(TEST_TYPE).stream()
                .flatMap(groupPath -> {
                            List<Path> rosettas = listFiles(groupPath, ".rosetta");
                            String groupName = groupPath.getFileName().toString();
                    Class<RosettaModelObject> rootDataType = generateCompileAndGetRootDataType(groupName, rosettas, helper, dynamicCompiledClassLoader);

                            return listFiles(groupPath, ".json").stream()
                                    .map(jsonPath -> Arguments.of(
                                            groupName,
                                            jsonPath.getFileName().toString(),
                                            rootDataType,
                                            readAsString(jsonPath)
                                    ));
                        }
                );
    }

}