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
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.NamespaceAwareClassLoaderIntegrationTest;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
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
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3597">HZ-3597 - Add unit tests for all @NamespacesSupported UDF
 *      interfaces, across all supported data structures</a>
 */
@RunWith(HazelcastParametrizedRunner.class)
@Category(SlowTest.class)
public abstract class UCDTest extends HazelcastTestSupport {
    private static MapResourceClassLoader mapResourceClassLoader;

    @Parameter(0)
    public ConnectionStyle connectionStyle;
    @Parameter(1)
    public ConfigStyle configStyle;
    @Parameter(2)
    public AssertionStyle assertionStyle;

    private TestHazelcastFactory testHazelcastFactory;

    protected HazelcastInstance member;
    protected HazelcastInstance instance;

    private NamespaceConfig namespaceConfig;

    protected String objectName = randomName();

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

    protected enum ConfigStyle {
        /** All configuration is done <strong>before</strong> the instance is started */
        STATIC,
        /** Where possible, configuration is changed <strong>after</strong> the instance has started */
        DYNAMIC;

        @Override
        public String toString() {
            return prettyPrintEnumName(name());
        }
    }

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

    @Parameters(name = "Connection Style: {0}, Config Style: {1}, Assertion Style: {2}")
    public static Iterable<Object[]> parameters() {
        return Lists.cartesianProduct(List.of(ConnectionStyle.values()), List.of(ConfigStyle.values()),
                List.of(AssertionStyle.values())).stream().map(Collection::toArray)::iterator;
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
    public void setUpInstance() throws ReflectiveOperationException {
        Config config = smallInstanceConfig();

        namespaceConfig = new NamespaceConfig(getNamespaceName());

        config.getNamespacesConfig().setEnabled(assertionStyle == AssertionStyle.POSITIVE);

        if (configStyle == ConfigStyle.STATIC) {
            mutateConfig(config);
            registerClass(config);
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

        if (configStyle == ConfigStyle.DYNAMIC) {
            mutateConfig(instance.getConfig());
            registerClass(instance.getConfig());
        }
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

    // TODO NS Should this be a Collection?
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

    private static String prettyPrintEnumName(String name) {
        return WordUtils.capitalizeFully(name.replace('_', StringUtil.SPACE));
    }
}
