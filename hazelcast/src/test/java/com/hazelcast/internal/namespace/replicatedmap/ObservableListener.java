package com.hazelcast.internal.namespace.replicatedmap;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import java.lang.StackWalker.StackFrame;

public abstract class ObservableListener implements HazelcastInstanceAware {
    private volatile HazelcastInstance hazelcastInstance;

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    protected void record(Object value) {
        if (hazelcastInstance != null) {
            StackFrame caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(frames -> frames.skip(1).findFirst()).orElseThrow();

            hazelcastInstance.getMap(caller.getDeclaringClass().getSimpleName()).put(caller.getMethodName(), value.toString());
        }
    }
}
