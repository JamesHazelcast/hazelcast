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
import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.NamespaceAwareClassLoaderIntegrationTest;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.test.HazelcastParametrizedRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.apache.commons.text.WordUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3597">HZ-3597 - Add unit tests for all @NamespacesSupported UDF
 *      interfaces, across all supported data structures</a>
 */
// TODO Is this a quick test?
@RunWith(HazelcastParametrizedRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public abstract class UCDTest extends HazelcastTestSupport {
    private TestHazelcastFactory testHazelcastFactory;

    @Parameter(0)
    public ConnectionStyle connectionStyle;
    @Parameter(1)
    public ConfigStyle configStyle;
    @Parameter(2)
    public AssertionStyle assertionStyle;

    private HazelcastInstance member;
    protected HazelcastInstance instance;

    private NamespaceConfig namespaceConfig;
    private MapResourceClassLoader mapResourceClassLoader;

    protected String objectName = randomName();

    private enum ConnectionStyle {
        /** Work directly with the underlying {@link HazelcastInstance} */
        MEMBER,
        /** Work via a client proxy, gives additional testing scope */
        CLIENT;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name());
        }
    }

    private enum ConfigStyle {
        /** All configuration is done <strong>before</strong> the instance is started */
        STATIC,
        /** Where possible, configuration is changed <strong>after</strong> the instance has started */
        DYNAMIC;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name());
        }
    }

    private enum AssertionStyle {
        /** Happy path - assert the functionality works when configured correctly */
        POSITIVE,
        /**
         * Negative path - assert that the functionality doesn't work normally when namespace not configured to ensure scope of
         * test is correct
         */
        NEGATIVE;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name());
        }
    }

    @Parameters(name = "Connection Style: {0}, Config Style: {1}, Assertion Style: {2}")
    public static Iterable<Object[]> parameters() {
        return Lists.cartesianProduct(List.of(ConnectionStyle.values()), List.of(ConfigStyle.values()),
                List.of(AssertionStyle.values())).stream().map(Collection::toArray)::iterator;
    }

    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        testHazelcastFactory = new TestHazelcastFactory();

        Config config = smallInstanceConfig();

        Path classRoot = Paths.get("src/test/class");
        mapResourceClassLoader = NamespaceAwareClassLoaderIntegrationTest.generateMapResourceClassLoaderForDirectory(classRoot);
        namespaceConfig = new NamespaceConfig(getNamespaceName());

        config.getNamespacesConfig().setEnabled(assertionStyle == AssertionStyle.POSITIVE);

        if (configStyle == ConfigStyle.STATIC) {
            mutateConfig(config);
            registerClass(config);
        }

        member = testHazelcastFactory.newHazelcastInstance(config);

        switch (connectionStyle) {
            case CLIENT:
                instance = testHazelcastFactory.newHazelcastClient();
                break;
            case MEMBER:
                instance = member;
                break;
            default:
                throw new IllegalArgumentException(connectionStyle.toString());
        }

        if (configStyle == ConfigStyle.DYNAMIC) {
            mutateConfig(instance.getConfig());
            registerClass(instance.getConfig());
        }
    }

    @After
    public void tearDown() {
        testHazelcastFactory.shutdownAll();
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

    @Test
    public void testChildMethodsNotTestable() throws ReflectiveOperationException {
        Class<Test> unwantedAnnotation = Test.class;
        assertNull(
                String.format("%s framework handles test execution, don't annotate implementations' methods with %s",
                        UCDTest.class.getSimpleName(), unwantedAnnotation.getSimpleName()),
                getClass().getDeclaredMethod("test").getAnnotation(unwantedAnnotation));
    }

    public abstract void test() throws Exception;

    // TODO Should this be a Collection?
    protected abstract String[] getUserDefinedClassNames();

    protected abstract void mutateConfig(Config config);

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

    protected Object getClassInstance() throws ReflectiveOperationException {
        return NamespaceAwareClassLoaderIntegrationTest.tryLoadClass(member, getNamespaceName(), getUserDefinedClassNames()[0])
                .getDeclaredConstructor().newInstance();
    }

    protected String getNamespaceName() {
        return "ns1";
    }
}
