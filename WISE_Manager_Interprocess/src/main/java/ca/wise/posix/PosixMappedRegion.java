package ca.wise.posix;

import com.sun.jna.Pointer;
import com.sun.jna.platform.linux.Mman;
import com.sun.jna.platform.unix.LibC;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.LibCUtil;

import ca.wise.IMappedRegionWrapper;
import ca.wise.InterprocessRuntimeException;
import ca.wise.posix.PosixSharedMemory.MappingHandle;
import ca.wise.posix.SemaphoreAPI.shmid_ds;

public class PosixMappedRegion implements IMappedRegionWrapper {
    
    private Pointer mBase;
    private long mSize;
    private boolean mIsXsi;

    public PosixMappedRegion(PosixSharedMemory mapping, int mode) {
        MappingHandle mapHnd = mapping.getMappingHandle();
        
        long size = 0;
        if (mapHnd.isXsi) {
            mIsXsi = true;
            shmid_ds xsi_ds = new shmid_ds();
            int ret = SemaphoreAPI.INSTANCE.shmctl(mapHnd.handle, SemaphoreAPI.IPC_STAT, xsi_ds);
            if (ret == -1) {
                throw new InterprocessRuntimeException("Failed to load XSI details");
            }
            if (size == 0) {
                size = xsi_ds.shm_segsz;
            }
            else if (size != xsi_ds.shm_segsz) {
                throw new InterprocessRuntimeException("Invalid mapped region size");
            }
            
            Pointer base = SemaphoreAPI.INSTANCE.shmat(mapHnd.handle, null, 0);
            if (base == null || base.equals(Mman.MAP_FAILED)) {
                throw new InterprocessRuntimeException("Unable to map shared memory");
            }
            
            mBase = base;
            mSize = size;
        }
        else {
            mIsXsi = false;
            if (size == 0) {
                SemaphoreAPI.stat st = new SemaphoreAPI.stat();
                if (SemaphoreAPI.fxstatHelper(mapHnd.handle, st) != 0) {
                    throw new InterprocessRuntimeException("Unable to stat shared memory");
                }
                size = st.st_size.longValue();
            }
            
            int prot = Mman.PROT_WRITE | Mman.PROT_READ;
            int flags = Mman.MAP_SHARED;
            
            Pointer base = LibCUtil.mmap(null, size, prot, flags, mapHnd.handle, 0);
            if (base == null || base.equals(Mman.MAP_FAILED)) {
                throw new InterprocessRuntimeException("Unable to mmap shared memory");
            }
            
            mBase = base;
            mSize = size;
        }
    }

    @Override
    public int getSize() {
        return (int)mSize;
    }

    @Override
    public Pointer getAddress() {
        return mBase;
    }

    @Override
    public void close() {
        if (mBase != null) {
            if (mIsXsi) {
                SemaphoreAPI.INSTANCE.shmdt(mBase);
                mBase = null;
            }
            else {
                LibC.INSTANCE.munmap(mBase, new size_t(mSize));
                mBase = null;
            }
        }
    }

}
