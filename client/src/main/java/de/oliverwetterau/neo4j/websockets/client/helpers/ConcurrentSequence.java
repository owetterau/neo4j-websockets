package de.oliverwetterau.neo4j.websockets.client.helpers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by oliver on 26.08.15.
 */
public class ConcurrentSequence {
    protected final AtomicInteger value = new AtomicInteger(0);

    public int incrementAndGet(final int maximumValue) {
        for (;;) {
            int current = value.get();
            int next = current + 1;
            if (next >= maximumValue) next = 0;
            if (compareAndSet(current, next))
                return next;
        }
    }

    protected boolean compareAndSet(final int current, final int next) {
        if (value.compareAndSet(current, next)) {
            return true;
        } else {
            LockSupport.parkNanos(1);
            return false;
        }
    }
}
