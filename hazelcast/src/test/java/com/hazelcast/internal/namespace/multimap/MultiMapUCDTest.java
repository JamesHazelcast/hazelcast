package com.hazelcast.internal.namespace.multimap;

import com.hazelcast.config.Config;
import com.hazelcast.config.MultiMapConfig;
import com.hazelcast.internal.namespace.UCDTest;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

public abstract class MultiMapUCDTest extends UCDTest {

    protected MultiMapConfig mapConfig;
    protected MultiMap<Object, Object> map;

    @Override
    public void setUpInstance() throws ReflectiveOperationException {
        mapConfig = new MultiMapConfig(objectName);
        mapConfig.setNamespace(getNamespaceName());

        super.setUpInstance();

        map = instance.getMultiMap(objectName);
    }

    protected void populate() {
        map.put(1, 1);
    }

    @Override
    protected void mutateConfig(Config config) {
        config.addMultiMapConfig(mapConfig);
    }
}
