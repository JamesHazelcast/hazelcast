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

import com.hazelcast.config.Config;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.NamespaceAwareClassLoaderIntegrationTest;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3597">HZ-3597 - Add unit tests for all @NamespacesSupported UDF
 *      interfaces, across all supported data structures</a>
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public abstract class UCDTest extends HazelcastTestSupport {
    protected NamespaceConfig namespaceConfig;
    protected HazelcastInstance instance;

    @Before
    public void setUp() throws Exception {
        Config config = new Config();

        Path classRoot = Paths.get("src/test/class");
        MapResourceClassLoader mapResourceClassLoader =
                NamespaceAwareClassLoaderIntegrationTest.generateMapResourceClassLoaderForDirectory(classRoot);
        namespaceConfig = new NamespaceConfig("ns1").addClass(mapResourceClassLoader.loadClass(classNameToLoad()));

        config.getNamespacesConfig().setEnabled(true).addNamespaceConfig(namespaceConfig);

        instance = createHazelcastInstance(config);
    }

    protected abstract String classNameToLoad();

    protected Class<?> tryLoadClass() throws ClassNotFoundException {
        return NamespaceAwareClassLoaderIntegrationTest.tryLoadClass(instance, namespaceConfig.getName(), classNameToLoad());
    }

    protected Object getClassInstance() throws ReflectiveOperationException {
        return tryLoadClass().getDeclaredConstructor().newInstance();
    }
}
