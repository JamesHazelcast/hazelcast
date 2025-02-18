/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.index;

import com.hazelcast.jet.sql.impl.JetSqlSerializerHook;
import com.hazelcast.sql.impl.exec.scan.index.IndexFilterValue;
import com.hazelcast.sql.impl.exec.scan.index.IndexRangeFilter;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class IndexRangeFilterTest extends IndexFilterTestSupport {
    @Test
    public void testContent() {
        IndexFilterValue from = intValue(1, true);
        IndexFilterValue to = intValue(2, true);

        IndexRangeFilter filter = new IndexRangeFilter(from, true, to, true);

        assertSame(from, filter.getFrom());
        assertTrue(filter.isFromInclusive());
        assertSame(to, filter.getTo());
        assertTrue(filter.isToInclusive());
    }

    @Test
    public void testEquals() {
        IndexRangeFilter filter = new IndexRangeFilter(intValue(1, true), true, intValue(2, true), true);

        checkEquals(filter, new IndexRangeFilter(intValue(1, true), true, intValue(2, true), true), true);

        checkEquals(filter, new IndexRangeFilter(intValue(2, true), true, intValue(2, true), true), false);
        checkEquals(filter, new IndexRangeFilter(null, false, intValue(2, true), true), false);

        checkEquals(filter, new IndexRangeFilter(intValue(1, true), false, intValue(2, true), true), false);

        checkEquals(filter, new IndexRangeFilter(intValue(1, true), true, intValue(3, true), true), false);
        checkEquals(filter, new IndexRangeFilter(intValue(1, true), true, null, false), false);

        checkEquals(filter, new IndexRangeFilter(intValue(1, true), true, intValue(2, true), false), false);
    }

    @Test
    public void testSerialization() {
        IndexRangeFilter original = new IndexRangeFilter(intValue(1, true), true, intValue(2, true), true);
        IndexRangeFilter restored = serializeAndCheck(original, JetSqlSerializerHook.INDEX_FILTER_RANGE);

        checkEquals(original, restored, true);
    }
}
