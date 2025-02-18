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

package com.hazelcast.internal.services;

import com.hazelcast.spi.impl.NodeEngine;

import java.util.Properties;

/**
 * An interface that can be implemented by an SPI Service to receive lifecycle calls:
 * <ol>
 * <li>initialization</li>
 * <li>shutdown</li>
 * <li>reset</li>
 * </ol>
 */
public interface ManagedService {

    /**
     * Initializes this service.
     *
     * @param nodeEngine the NodeEngine that this service belongs to
     * @param properties the Properties (can be used to pass settings to the service)
     */
    void init(NodeEngine nodeEngine, Properties properties);

    /**
     * Resets this service back to its initial state. This should represent the same
     * state that the service would be in after a normal startup, including config
     * defined properties being loaded.
     * <p>
     * Some of the uses for the #reset() method include usage in "force start" scenarios
     * with Hot Restart, and during split-brain merging.
     */
    void reset();

    /**
     * Shuts down this service.
     * <p>
     * TODO: what is the purpose of the terminate variable
     *
     * @param terminate {@code true} to shut down this service
     */
    void shutdown(boolean terminate);
}
