package com.hazelcast.internal.namespace.multimap;

import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.map.IMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiMapMapListenerUCDTest extends MultiMapUCDTest {
    @Override
    public void test() throws Exception {
        map.put(1, 1);
        IMap<String, Boolean> map = instance.getMap("MultiMapMapListenerUCDTest");

        //TODO: is there any need to test the other methods of the MultiMapEntryListener ?
        assertTrueEventually(() -> {
            Boolean added = map.get("added");
            assertNotNull(added);
            assertTrue(added);
        });
    }

    @Override
    protected void mutateConfig(Config config) {
        EntryListenerConfig listenerConfig = new EntryListenerConfig();
        listenerConfig.setClassName(getUserDefinedClassNames()[0]);

        mapConfig.addEntryListenerConfig(listenerConfig);
        super.mutateConfig(config);
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.MultiMapEntryListener"};
    }
}
