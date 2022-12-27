package ca.wise;

import java.io.Closeable;
import java.util.Locale;

import ca.wise.SharedBlock.SharedMemoryDetails;
import ca.wise.posix.PosixSharedMemory;
import ca.wise.windows.WindowsSharedMemory;

public class SharedMemory implements Closeable {
    
    protected ISharedMemoryWrapper mMem;

    public SharedMemory(String name) {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (os.contains("mac") || os.contains("darwin"))
            mMem = null;
        else if (os.contains("win"))
            mMem = new WindowsSharedMemory(name);
        else if (os.contains("nux"))
            mMem = new PosixSharedMemory(name);
        else
            throw new InterprocessRuntimeException("Unsupported platform");
    }
    
    public void truncate(int size) {
        mMem.truncate(size);
    }
    
    public boolean clean(SharedMemoryDetails details) {
        return mMem.clean(details);
    }
    
    @Override
    public void close() {
        mMem.close();
    }
}
