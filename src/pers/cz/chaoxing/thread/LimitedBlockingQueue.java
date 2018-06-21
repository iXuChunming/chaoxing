package pers.cz.chaoxing.thread;

import java.util.concurrent.LinkedBlockingQueue;

public class LimitedBlockingQueue<T> extends LinkedBlockingQueue<T> {
    public LimitedBlockingQueue(int maxSize) {
        super(maxSize);
    }

    @Override
    public boolean offer(T t) {
// turn offer() and add() into a blocking calls (unless interrupted)
        try {
            put(t);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
