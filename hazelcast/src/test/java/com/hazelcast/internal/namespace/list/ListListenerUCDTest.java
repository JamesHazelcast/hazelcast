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

import com.hazelcast.config.ItemListenerConfig;

public abstract class ListListenerUCDTest extends ListUCDTest {
    @Override
    protected void addClassInstanceToConfig() throws ReflectiveOperationException {
        ItemListenerConfig itemListenerConfig = new ItemListenerConfig();
        itemListenerConfig.setImplementation(getClassInstance());
        listConfig.addItemListenerConfig(itemListenerConfig);
    }

    @Override
    protected void addClassNameToConfig() {
        ItemListenerConfig itemListenerConfig = new ItemListenerConfig();
        itemListenerConfig.setClassName(getUserDefinedClassNames()[0]);
        listConfig.addItemListenerConfig(itemListenerConfig);
    }

    @Override
    protected void addClassInstanceToDataStructure() throws ReflectiveOperationException {
        list.addItemListener(getClassInstance(), true);
    }

    @Override
    protected boolean isNoClassRegistrationAllowed() {
        return false;
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.MyItemListener", "usercodedeployment.ObservableListener"};
    }
}
