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

package com.hazelcast.internal.namespace.ischeduledexecutor;

import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.internal.namespace.iexecutor.IExecutorUCDTest;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;

public abstract class IScheduledExecutorUCDTest extends IExecutorUCDTest {
    protected ExecutorConfig executorConfig;
    protected IScheduledExecutorService executor;

    @Override
    public void setUpInstance() throws ReflectiveOperationException {
        executorConfig = new ExecutorConfig(objectName);
        executorConfig.setNamespace(getNamespaceName());

        super.setUpInstance();

        executor = instance.getScheduledExecutorService(objectName);
    }
}
