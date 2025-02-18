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

package com.hazelcast.spring.cache;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.map.IMap;
import com.hazelcast.map.MapInterceptorAdaptor;
import com.hazelcast.spring.CustomSpringExtension;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.Serial;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for {@link HazelcastCache} for timeout.
 *
 * @author Gokhan Oner
 */
@ExtendWith({SpringExtension.class, CustomSpringExtension.class})
@ContextConfiguration(locations = {"no-readtimeout-config.xml"})
class HazelcastCacheNoReadTimeoutTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private IDummyTimeoutBean dummyTimeoutBean;

    private Cache delayNo;

    @BeforeAll
    @AfterAll
    public static void start() {
        Hazelcast.shutdownAll();
    }

    @BeforeEach
    public void setup() {
        this.delayNo = cacheManager.getCache("delayNo");

        //no timeout
        ((IMap<?, ?>) this.delayNo.getNativeCache()).addInterceptor(new DelayIMapGetInterceptor(250));
    }

    @Test
    void testCache_TimeoutConfig() {
        assertEquals(0, ((HazelcastCache) delayNo).getReadTimeout());
    }

    @Test
    void testBean_delayNo() {
        String key = createRandomKey();
        long start = System.nanoTime();
        dummyTimeoutBean.getDelayNo(key);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(time >= 250L);
    }

    private static class DelayIMapGetInterceptor extends MapInterceptorAdaptor {
        @Serial
        private static final long serialVersionUID = 1L;

        private final int delay;

        DelayIMapGetInterceptor(int delay) {
            this.delay = delay;
        }

        @Override
        public Object interceptGet(Object value) {
            HazelcastTestSupport.sleepMillis(delay);
            return super.interceptGet(value);
        }
    }

    public static class DummyTimeoutBean implements IDummyTimeoutBean {

        @Override
        public Object getDelay150(String key) {
            return null;
        }

        @Override
        public Object getDelay50(String key) {
            return null;
        }

        @Override
        public Object getDelayNo(String key) {
            return null;
        }

        @Override
        public String getDelay100(String key) {
            return null;
        }
    }

    private String createRandomKey() {
        return UUID.randomUUID().toString();
    }
}
