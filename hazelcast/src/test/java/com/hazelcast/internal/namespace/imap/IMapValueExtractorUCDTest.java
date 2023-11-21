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

import com.hazelcast.config.AttributeConfig;
import com.hazelcast.config.Config;
import com.hazelcast.query.Predicates;
import org.junit.Before;

import static org.junit.Assert.assertFalse;

public class IMapValueExtractorUCDTest extends IMapUCDTest {
    /** The name of a field that does not exist in the objects in the {@link #map} */
    private String attributeName;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        attributeName = "dummyAttribute";
    }

    @Override
    public void test() throws Exception {
        populate();

        assertFalse(map.entrySet(Predicates.equal(attributeName, 2)).isEmpty());
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.IncrementingValueExtractor"};
    }

    @Override
    protected void mutateConfig(Config config) {
        mapConfig.addAttributeConfig(new AttributeConfig(attributeName, getUserDefinedClassNames()[0]));

        super.mutateConfig(config);
    }
}
