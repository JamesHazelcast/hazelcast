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

package com.hazelcast.internal.namespace.list;

import com.hazelcast.collection.IList;
import com.hazelcast.config.Config;
import com.hazelcast.config.ListConfig;
import com.hazelcast.internal.namespace.UCDTest;
import org.junit.Before;

import java.io.IOException;

public abstract class ListUCDTest extends UCDTest {
    protected ListConfig listConfig;
    protected IList<Object> list;

    @Override
    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        listConfig = new ListConfig(objectName);
        listConfig.setNamespace(getNamespaceName());

        super.setUp();

        list = instance.getList(objectName);
    }

    @Override
    protected void mutateConfig(Config config) {
        config.addListConfig(listConfig);
    }
}
