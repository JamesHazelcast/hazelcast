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

import org.junit.Ignore;

// TODO NS this is failing because of https://github.com/hazelcast/hazelcast/pull/26040
// Once fixed, UDF will need to be updated with new interface
// OR it can just extend com.hazelcast.map.impl.query.AlwaysTruePagingPredicate
@Ignore
public class IMapPagingPredicateUCDTest extends IMapPredicateUCDTest {
    @Override
    protected String getUserDefinedClassName() {
        return "usercodedeployment.TruePagingPredicate";
    }
}
