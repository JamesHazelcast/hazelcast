/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.test.bounce;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.RoutingMode;
import com.hazelcast.core.HazelcastInstance;

/**
 * Multisocket client test driver factory for bouncing members tests
 */
public class MultiSocketClientDriverFactory extends AbstractClientDriverFactory {

    public MultiSocketClientDriverFactory() {
    }

    public MultiSocketClientDriverFactory(ClientConfig clientConfig) {
        super(clientConfig);
    }

    /**
     * Creates client config for {@link RoutingMode#ALL_MEMBERS} routing client
     */
    @Override
    protected ClientConfig getClientConfig(HazelcastInstance member) {
        ClientConfig config = clientConfig == null ? new ClientConfig() : clientConfig;
        config.getNetworkConfig().setRedoOperation(true);

        return config;
    }
}
