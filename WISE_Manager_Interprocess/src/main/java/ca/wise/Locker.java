package ca.wise;

import java.io.Closeable;

public class Locker implements Closeable {

    private NamedMutex mutex;
    
    public Locker(NamedMutex mutex) throws InterprocessException {
        this.mutex = mutex;
        this.mutex.lock();
    }

    @Override
    public void close() {
        mutex.unlock();
    }
}
