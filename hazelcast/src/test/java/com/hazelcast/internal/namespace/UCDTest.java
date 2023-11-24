/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.namespace;

import com.google.common.collect.Lists;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.NamespaceAwareClassLoaderIntegrationTest;
import com.hazelcast.internal.util.ExceptionUtil;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.test.AssertTask;
import com.hazelcast.test.HazelcastParametrizedRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.SlowTest;
import io.netty.util.internal.StringUtil;
import org.apache.commons.text.WordUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

/**
 * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3597">HZ-3597 - Add unit tests for all @NamespacesSupported UDF
 *      interfaces, across all supported data structures</a>
 */
@RunWith(HazelcastParametrizedRunner.class)
@Category(SlowTest.class)
public abstract class UCDTest extends HazelcastTestSupport {
    private static final ILogger LOGGER = Logger.getLogger(UCDTest.class);
    private static MapResourceClassLoader mapResourceClassLoader;

    @Parameter(0)
    public ConnectionStyle connectionStyle;
    @Parameter(1)
    public ConfigStyle configStyle;
    @Parameter(2)
    public ClassRegistrationStyle classRegistrationStyle;
    @Parameter(3)
    public AssertionStyle assertionStyle;

    private TestHazelcastFactory testHazelcastFactory;

    protected HazelcastInstance member;
    protected HazelcastInstance instance;

    private NamespaceConfig namespaceConfig;

    protected String objectName = randomName();

    // TODO NS Should these be moved into their own classes?
    protected enum ConnectionStyle {
        /** Work directly with the underlying {@link HazelcastInstance} */
        EMBEDDED,
        /** Test communication between {@link HazelcastClient} & member */
        CLIENT_TO_MEMBER,
        /** Test communication between members - using a lite member as the entry point */
        MEMBER_TO_MEMBER;

        @Override
        public String toString() {
            return prettyPrintEnumName(name());
        }
    }

    // TODO NS Should these be moved into their own classes?
    protected enum ConfigStyle {
        /** All configuration is set programmatically <strong>before</strong> the instance is started */
        STATIC_PROGRAMMATIC,
        /** Where possible, configuration is changed <strong>after</strong> the instance has started */
        DYNAMIC,
        /** All configuration is defined by a YAML configuration, derived from our programmatic config */
        STATIC_YAML,
        /** All configuration is defined by an XML configuration, derived from our programmatic config */
        STATIC_XML;

        @Override
        public String toString() {
            return prettyPrintEnumName(name());
        }
    }

    // TODO NS Should these be moved into their own classes?
    /**
     * Classes can be registered in multiple ways, parameterized to allow tests to define what they support and then each to be
     * tested the same way
     * <p>
     * called methods are expected to be inherited and extended so must call a method in the enclosing class.
     */
    protected enum ClassRegistrationStyle {
        INSTANCE_IN_CONFIG(instance -> {
            assumeThat("Can only create instances with a dynamic configuration, i.e. where the cluster is already up & running",
                    instance.configStyle, is(ConfigStyle.DYNAMIC));

            try {
                instance.addClassInstanceToConfig();
            } catch (ReflectiveOperationException e) {
                throw ExceptionUtil.sneakyThrow(e);
            }
        }), NAME_IN_CONFIG(UCDTest::addClassNameToConfig), INSTANCE_IN_DATA_STRUCTURE((instance) -> {
            assumeThat("Can only create instances with a dynamic configuration, i.e. where the cluster is already up & running",
                    instance.configStyle, is(ConfigStyle.DYNAMIC));

            try {
                instance.addClassInstanceToDataStructure();
            } catch (ReflectiveOperationException e) {
                throw ExceptionUtil.sneakyThrow(e);
            }
        }), NONE((instance) -> assumeTrue("Skipping as configuration not supported", instance.isNoClassRegistrationAllowed()));

        private final Consumer<UCDTest> action;

        private ClassRegistrationStyle(Consumer<UCDTest> action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return prettyPrintEnumName(name());
        }
    }

    // TODO NS Should these be moved into their own classes?
    protected enum AssertionStyle {
        /** Happy path - assert the functionality works when configured correctly */
        POSITIVE,
        /**
         * Negative path - assert that the functionality doesn't work normally when namespace not configured to ensure scope of
         * test is correct
         */
        NEGATIVE;

        @Override
        public String toString() {
            return prettyPrintEnumName(name());
        }
    }

    @Parameters(name = "Connection: {0}, Config: {1}, Class Registration: {2}, Assertion: {3}")
    public static Iterable<Object[]> parameters() {
        // TODO NS fix this
        ClassRegistrationStyle[] classRegistrationStyleFiltered = Arrays.stream(ClassRegistrationStyle.values()).filter(style ->
        // ObservableListener relies on putting the value in an IMap accessed via HazelcastInstanceAware
        // But HazelcastInstanceAware is only available on listeners registered via config, NOT on those registered on the data
        // structure directly
        // E.G. AbstractCollectionProxyImpl.initialize() iterates through the configs
        style != ClassRegistrationStyle.INSTANCE_IN_DATA_STRUCTURE &&
        // TODO Just wasn't working, was like the listener wasn't firing - to be investigated properly
                style != ClassRegistrationStyle.NAME_IN_CONFIG).toArray(ClassRegistrationStyle[]::new);

        return Lists
                .cartesianProduct(List.of(ConnectionStyle.values()), List.of(ConfigStyle.values()),
                        List.of(classRegistrationStyleFiltered), List.of(AssertionStyle.values()))
                .stream().map(Collection::toArray)::iterator;

         // Hardcode paramaters for debugging purposes
         // return List.<Object[]>of(new Object[] {ConnectionStyle.EMBEDDED, ConfigStyle.DYNAMIC, ClassRegistrationStyle.NAME_IN_CONFIG, AssertionStyle.POSITIVE});
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        Path classRoot = Paths.get("src/test/class");
        mapResourceClassLoader = NamespaceAwareClassLoaderIntegrationTest.generateMapResourceClassLoaderForDirectory(classRoot);
    }

    @Before
    public void setUp() {
        testHazelcastFactory = new TestHazelcastFactory();
    }

    @After
    public void tearDown() {
        testHazelcastFactory.shutdownAll();
    }

    /** Don't annotate children with {@code @Before}, framework controls test execution */
    private void setUpInstance() throws ReflectiveOperationException {
        initialiseConfig();

        Config config = smallInstanceConfigWithoutJetAndMetrics();

        namespaceConfig = new NamespaceConfig(getNamespaceName());

        config.getNamespacesConfig().setEnabled(assertionStyle == AssertionStyle.POSITIVE);

        if (configStyle != ConfigStyle.DYNAMIC) {
            setupConfigs(config);
            config = convertConfig(config);
        }

        member = testHazelcastFactory.newHazelcastInstance(config);

        switch (connectionStyle) {
            case EMBEDDED:
                instance = member;
                break;
            case CLIENT_TO_MEMBER:
                instance = testHazelcastFactory.newHazelcastClient();
                break;
            case MEMBER_TO_MEMBER:
                instance = testHazelcastFactory.newHazelcastInstance(config.setLiteMember(true));
                break;
            default:
                throw new IllegalArgumentException(connectionStyle.toString());
        }

        initialiseDataStructure();

        if (configStyle == ConfigStyle.DYNAMIC) {
            setupConfigs(instance.getConfig());
        }
    }

    private void setupConfigs(Config config) throws ReflectiveOperationException {
        registerClass(config);

        try {
            classRegistrationStyle.action.accept(this);
        } catch (UnsupportedOperationException e) {
            assumeNoException("Skipping as configuration not supported", e);
        }

        registerConfig(config);
    }

    /**
     * A place to construct an instance of {@link MapConfig} or similar
     * <p>
     * Don't annotate children with {@code @Before}, framework controls test execution
     */
    protected abstract void initialiseConfig();

    /**
     * A place to construct an instance of {@link IMap} or similar
     * <p>
     * Don't annotate children with {@code @Before}, framework controls test execution
     */
    protected abstract void initialiseDataStructure();

    /**
     * Where applicable, transforms our programmatic config into an XML/YAML variant and feeds it back into a new {@link Config}
     * instance, thereby validating XML/YAML parsing.
     *
     * @param config the programmatic config to convert
     * @return the newly converted {@link Config}
     */
    private Config convertConfig(Config config) {
        String configString;
        switch (configStyle) {
            case STATIC_XML:
                configString = new ConfigXmlGenerator(true, false).generate(config);
                break;

            case STATIC_YAML:
                // TODO NS - replace with YAML variant once located
                configString = new ConfigXmlGenerator(true, false).generate(config);
                break;

            default:
                // No conversion necessary
                return config;
        }
        return Config.loadFromString(configString);
    }

    /**
     * Executes {@link #test()}, and checking it's result against the expected {@link #assertionStyle}
     * <p>
     * It's possible (and neater) to implement this as a pair of {@link Test}s and use
     * {@code assumeTrue(assertionStyle=AssertionStyle.XYZ)} to switch between execution implementation at runtime, but then you
     * have twice the {@link Before} overhead when half is never used.
     */
    @Test
    public void executeTest() throws Exception {
        try {
            setUpInstance();
            test();
        } catch (Throwable t) {
            switch (assertionStyle) {
                case NEGATIVE:
                    // Do nothing - this is ok
                    return;
                case POSITIVE:
                    throw t;
                default:
                    throw new IllegalArgumentException(assertionStyle.toString());
            }
        }

        assertEquals("Test passed even though namespace was not configured, suggests scope of test is incorrect",
                AssertionStyle.POSITIVE, assertionStyle);
    }

    /** Don't annotate children with {@code @Test}, framework controls test execution */
    public abstract void test() throws Exception;

    /**
     * Classes to be loaded into the environment within the scope of this test
     * <p>
     * Typically main class under test is first
     */
    protected abstract String[] getUserDefinedClassNames();

    /** Allows registration of a config (e.g. a {@link MapConfig} with the {@link HazelcastInstance#getConfig()} */
    protected abstract void registerConfig(Config config);

    /**
     * Allows registration of a class with the configuration of the data structure, based on an instance of the class
     * <p>
     * E.G. {@link EntryListenerConfig#setImplementation(MapListener)}
     * <p>
     * It's likely this behavior is mutually exclusive with {@link #isNoClassRegistrationAllowed()}
     * <p>
     * Implementers should *consider* overriding this behavior.
     *
     * @throws ReflectiveOperationException because implementers will be constructing a class, likely using reflection
     * @throws UnsupportedOperationException if the test does not support this kind of configuration
     *
     * @see ClassRegistrationStyle#INSTANCE_IN_CONFIG
     */
    protected void addClassInstanceToConfig() throws ReflectiveOperationException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Allows registration of a class with the configuration of the data structure, based on the name of the class
     * <p>
     * E.G. {@link ListenerConfig#setClassName(String)}
     * <p>
     * It's likely this behavior is mutually exclusive with {@link #isNoClassRegistrationAllowed()}
     * <p>
     * Implementers should *consider* overriding this behavior.
     *
     * @throws UnsupportedOperationException if the test does not support this kind of configuration
     *
     * @see ClassRegistrationStyle#NAME_IN_CONFIG
     */
    protected void addClassNameToConfig() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Allows registration of a class with the data structure itself, based on an instance of the class
     * <p>
     * E.G. {@link IMap#addEntryListener(MapListener, boolean)}
     * <p>
     * It's likely this behavior is mutually exclusive with {@link #isNoClassRegistrationAllowed()}
     * <p>
     * Implementers should *consider* overriding this behavior.
     *
     * @throws ReflectiveOperationException because implementers will be constructing a class, likely using reflection
     * @throws UnsupportedOperationException if the test does not support this kind of configuration
     *
     * @see ClassRegistrationStyle#INSTANCE_IN_DATA_STRUCTURE
     */
    protected void addClassInstanceToDataStructure() throws ReflectiveOperationException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Describes whether the test can be run without a class being registered with anything else - e.g.
     * {@link IExecutorService#submitToMember(Callable<T>, Member)}
     * <p>
     * It's likely this behavior is mutually exclusive with other {@link ClassRegistrationStyle}s
     * <p>
     * Implementers should *consider* overriding this behavior.
     *
     * @see ClassRegistrationStyle#NONE
     */
    protected boolean isNoClassRegistrationAllowed() {
        return true;
    }

    private void registerClass(Config config) throws ClassNotFoundException {
        for (String className : getUserDefinedClassNames()) {
            Class<?> clazz = mapResourceClassLoader.loadClass(className);

            Class<?> unwanted = IdentifiedDataSerializable.class;
            assertFalse(String.format(
                    "%s should not implement %s, as unless done with care, when deserialized the parent might be deserialized instead",
                    className, unwanted.getSimpleName()), unwanted.isAssignableFrom(clazz));

            namespaceConfig.addClass(clazz);
        }

        config.getNamespacesConfig().addNamespaceConfig(namespaceConfig);
    }

    /** @return an instance of the {@link Class} returned from {@link #getClassObject()} */
    protected <T> T getClassInstance() throws ReflectiveOperationException {
        return (T) getClassObject().getDeclaredConstructor().newInstance();
    }

    /** @return the {@link Class} object generated from the first element of {@link #getUserDefinedClassNames()} */
    private Class<?> getClassObject() throws ReflectiveOperationException {
        return NamespaceAwareClassLoaderIntegrationTest.tryLoadClass(member, getNamespaceName(), getUserDefinedClassNames()[0]);
    }

    protected String getNamespaceName() {
        return "ns1";
    }

    // TODO NS Should this be moved into some test-scoped "EnumUtils" class?
    private static String prettyPrintEnumName(String name) {
        return WordUtils.capitalizeFully(name.replace('_', StringUtil.SPACE));
    }

    /**
     * We expect that listeners implement {@code usercodedeployment.ObservableListener}
     * <p>
     * When a listeners' event is fired, it calls {@code usercodedeployment.ObservableListener.record(Object)} which:
     * <ol>
     * <li>Creates an {@link IMap} with the name of the listener (derived, via reflection, from the caller) - e.g.
     * {@code MyEntryListener}
     * <li>Puts an entry in the map where:
     * <ul>
     * <li>Key = The name of the method in the listener (derived, via reflection, from the caller) - e.g. {@code entryAdded}
     * <li>Value = {@link Object#toString()} of the event - this is because we cannot ensure the event is
     * {@link java.io.Serializable} in it's native form
     * </ul>
     * </ol>
     * <p>
     * To check for this firing, we can {@link #assertTrueEventually(AssertTask)} a corresponding entry exists.
     * <p>
     * If this was documented in the class itself, it would get lost during compilation
     *
     * @param key the name of the method in the listener that should've fired
     */
    protected void assertListenerFired(String key) throws ReflectiveOperationException {
        Map<String, ?> map = instance.getMap(getClassObject().getSimpleName());
        assertTrueEventually(() -> {
            if (LOGGER.isFinestEnabled()) {
                LOGGER.finest("Checking map for values, currently contains %s", map.keySet());
            }

            assertTrue(map.containsKey(key));
        });
    }
}
