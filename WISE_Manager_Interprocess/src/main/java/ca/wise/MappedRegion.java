package ca.wise;

import java.io.Closeable;

import com.sun.jna.Pointer;

import ca.wise.posix.PosixMappedRegion;
import ca.wise.posix.PosixSharedMemory;
import ca.wise.windows.WindowsMappedRegion;
import ca.wise.windows.WindowsSharedMemory;

public class MappedRegion implements Closeable {

    public static final int READ_ONLY = 1;
    public static final int READ_WRITE = 2;
    public static final int COPY_ON_WRITE = 3;
    public static final int READ_PRIVATE = 4;
    
    private IMappedRegionWrapper mRegion;
    
    public MappedRegion(SharedMemory mem, int mode) {
        if (mem.mMem instanceof WindowsSharedMemory)
            mRegion = new WindowsMappedRegion((WindowsSharedMemory)mem.mMem, mode);
        else if (mem.mMem instanceof PosixSharedMemory)
            mRegion = new PosixMappedRegion((PosixSharedMemory)mem.mMem, mode);
    }
    
    public int getSize() {
        return mRegion.getSize();
    }
    
    public Pointer getAddress() {
        return mRegion.getAddress();
    }

    @Override
    public void close() {
        mRegion.close();
    }
}
