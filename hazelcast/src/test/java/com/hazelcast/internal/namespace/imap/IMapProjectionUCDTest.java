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

package com.hazelcast.internal.namespace.imap;

import com.hazelcast.projection.Projection;
import org.junit.Test;

import java.util.Map.Entry;

import static org.junit.Assert.assertNotNull;

public class IMapProjectionUCDTest extends IMapUCDTest {
    @Test
    public void test() throws Exception {
        populate();

        assertNotNull(map.project((Projection<Entry<?, ?>, ?>) getClassInstance()));
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.IdentityProjection"};
    }
}
