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

import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.RingbufferStoreConfig;
import com.hazelcast.core.IFunction;
import com.hazelcast.internal.namespace.UCDTest;
import com.hazelcast.ringbuffer.Ringbuffer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RingbufferUCDTest extends UCDTest {
    private RingbufferConfig ringBufferConfig;
    private Ringbuffer<Object> ringBuffer;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();

        ringBufferConfig = instance.getConfig().getRingbufferConfig(randomName());
        ringBufferConfig.setNamespace(namespaceConfig.getName());

        ringBuffer = instance.getRingbuffer(ringBufferConfig.getName());
    }

    @Test
    public void testIFunction() throws Exception {
        String clazz = "usercodedeployment.AcceptAllIFunction";

        registerClass(clazz);

        ringBuffer.add(Byte.MIN_VALUE);

        assertEquals(1,
                ringBuffer.readManyAsync(ringBuffer.headSequence(), 0, 1, (IFunction<Object, Boolean>) getClassInstance(clazz))
                        .toCompletableFuture().get().size());
    }

    @Test
    public void testRingBufferStore() throws Exception {
        String clazz = "usercodedeployment.LargeSequenceRingBufferStore";

        ringBufferConfig.setRingbufferStoreConfig(new RingbufferStoreConfig().setClassName(clazz));

        registerClass(clazz);

        assertEquals(Long.MAX_VALUE, ringBuffer.tailSequence());
    }
}
