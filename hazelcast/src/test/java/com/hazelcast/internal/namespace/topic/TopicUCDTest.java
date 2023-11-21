package com.hazelcast.internal.namespace.topic;

import com.hazelcast.config.Config;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.internal.namespace.UCDTest;
import com.hazelcast.topic.ITopic;

import java.io.IOException;

public abstract class TopicUCDTest extends UCDTest {

    protected ReliableTopicConfig reliableTopicConfig;
    protected ITopic<Object> topic;

    @Override
    public void setUpInstance() throws IOException, ClassNotFoundException {
        reliableTopicConfig = new ReliableTopicConfig(objectName);
        reliableTopicConfig.setNamespace(getNamespaceName());
        super.setUpInstance();

        topic = instance.getReliableTopic(objectName);
    }

    @Override
    protected void mutateConfig(Config config) {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setClassName(getUserDefinedClassNames()[0]);

        reliableTopicConfig.addMessageListenerConfig(listenerConfig);
        config.addReliableTopicConfig(reliableTopicConfig);
    }
}
