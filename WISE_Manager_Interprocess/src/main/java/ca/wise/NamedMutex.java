package ca.wise;

import java.io.Closeable;
import java.util.Locale;

import ca.wise.posix.PosixNamedSync;
import ca.wise.windows.WindowsNamedSync;

public class NamedMutex implements Closeable {
    
    private INamedMutexWrapper mMutex;

    public NamedMutex(String name) {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (os.contains("mac") || os.contains("darwin"))
            mMutex = null;
        else if (os.contains("win"))
            mMutex = new WindowsNamedSync();
        else if (os.contains("nux"))
            mMutex = new PosixNamedSync();
        else
            throw new InterprocessRuntimeException("Unsupported platform");
        
        mMutex.openOrCreate(name);
    }

    @Override
    public void close() {
        mMutex.close();
    }
    
    public void lock() throws InterprocessException {
        mMutex.lock();
    }
    
    public void unlock() {
        mMutex.unlock();
    }
}
