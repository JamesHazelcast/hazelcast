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

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.NamespaceAwareClassLoaderIntegrationTest;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.test.HazelcastParametrizedRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.apache.commons.text.WordUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

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

    @Parameters(name = "Connection Style: {0}, Config Style: {1}")
    public static Collection<Object[]> parameters() {
        // Cartesian join
        return Arrays.stream(ConnectionStyle.values())
                .flatMap(driver -> Arrays.stream(ConfigStyle.values()).map(configStyle -> new Object[] {driver, configStyle}))
                .collect(Collectors.toList());
    }

    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        testHazelcastFactory = new TestHazelcastFactory();

        Config config = smallInstanceConfig();

        Path classRoot = Paths.get("src/test/class");
        mapResourceClassLoader = NamespaceAwareClassLoaderIntegrationTest.generateMapResourceClassLoaderForDirectory(classRoot);
        namespaceConfig = new NamespaceConfig(getNamespaceName());

        config.getNamespacesConfig().setEnabled(true);

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

    // TODO Should this be a Collection?
    protected abstract String[] getUserDefinedClassNames();

    protected abstract void mutateConfig(Config config);

    private void registerClass(Config config) throws ClassNotFoundException {
        for (String clazz : getUserDefinedClassNames()) {
            namespaceConfig.addClass(mapResourceClassLoader.loadClass(clazz));
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
