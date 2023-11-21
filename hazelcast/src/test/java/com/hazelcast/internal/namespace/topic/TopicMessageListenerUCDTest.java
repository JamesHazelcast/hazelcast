package com.hazelcast.internal.namespace.topic;

import com.hazelcast.map.IMap;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TopicMessageListenerUCDTest extends TopicUCDTest {

    @Override
    public void test() throws Exception {
        objectName = "TopicMessageListenerUCDTest";
        topic.publish(Byte.MIN_VALUE);
        IMap<String, Boolean> map = instance.getMap("TopicMessageListenerUCDTest");
        assertNotNull(map);
        assertTrueEventually("The 'processed' key should be set to true eventually", () -> {
            Boolean processed = map.get("processed");
            if (processed != null) {
                assertTrue(map.get("processed"));
            }
        });
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[]{"usercodedeployment.TopicMessageListener"};
    }
}
