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

package usercodedeployment;

import com.hazelcast.map.EntryProcessor;
import usercodedeployment.child.ComplexProcessor;

import java.util.Map;

/**
 * This test class is intentionally in its own package
 * as Hazelcast has special rules for loading classes
 * from the {@code com.hazelcast.*} package.
 */
public class ComplexStringEntryProcessor implements EntryProcessor<String, Integer, Integer> {
    @Override
    public Integer process(Map.Entry<String, Integer> entry) {
        int result = ComplexProcessor.handleComplexProcessing(entry.getValue());
        entry.setValue(result);
        return result;
    }
}
