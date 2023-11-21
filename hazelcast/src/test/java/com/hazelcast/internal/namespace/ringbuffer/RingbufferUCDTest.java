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

package com.hazelcast.internal.namespace.ringbuffer;

import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.internal.namespace.UCDTest;
import com.hazelcast.ringbuffer.Ringbuffer;

import java.io.IOException;

public abstract class RingbufferUCDTest extends UCDTest {
    protected RingbufferConfig ringBufferConfig;
    protected Ringbuffer<Object> ringBuffer;

    @Override
    public void setUpInstance() throws IOException, ReflectiveOperationException {
        ringBufferConfig = new RingbufferConfig(objectName);
        ringBufferConfig.setNamespace(getNamespaceName());

        super.setUpInstance();

        ringBuffer = instance.getRingbuffer(objectName);
    }

    @Override
    protected void mutateConfig(Config config) {
        config.addRingBufferConfig(ringBufferConfig);
    }
}
