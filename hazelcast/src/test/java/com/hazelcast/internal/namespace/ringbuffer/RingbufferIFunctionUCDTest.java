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

import com.hazelcast.core.IFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RingbufferIFunctionUCDTest extends RingbufferUCDTest {
    @Test
    public void test() throws Exception {
        ringBuffer.add(Byte.MIN_VALUE);

        assertEquals(1,
                ringBuffer.readManyAsync(ringBuffer.headSequence(), 0, 1, (IFunction<Object, Boolean>) getClassInstance())
                        .toCompletableFuture().get().size());
    }

    @Override
    protected String getUserDefinedClassName() {
        return "usercodedeployment.AcceptAllIFunction";
    }
}
