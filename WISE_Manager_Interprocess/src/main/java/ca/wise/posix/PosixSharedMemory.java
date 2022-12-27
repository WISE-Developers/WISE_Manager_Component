package ca.wise.posix;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.ErrNo;
import com.sun.jna.platform.linux.Fcntl;
import com.sun.jna.platform.linux.LibRT;
import com.sun.jna.platform.unix.LibC;
import com.sun.jna.platform.unix.LibCUtil;

import ca.wise.ISharedMemoryWrapper;
import ca.wise.InterprocessRuntimeException;

public class PosixSharedMemory implements ISharedMemoryWrapper {

    private int mHandle;
    
    public PosixSharedMemory(String name) {
        name = InterprocessPlatformUtils.addLeadingSlash(name);
        
        int oflag = Fcntl.O_RDWR;
        while (true) {
            mHandle = LibRT.INSTANCE.shm_open(name, oflag | Fcntl.O_CREAT | Fcntl.O_EXCL, 0666);
            if (mHandle >= 0) {
                SemaphoreAPI.INSTANCE.fchmod(mHandle, 0666);
            }
            else if (Native.getLastError() == ErrNo.EEXIST) {
                mHandle = LibRT.INSTANCE.shm_open(name, oflag, 0666);
                if (mHandle < 0 && Native.getLastError() == ErrNo.ENOENT)
                    continue;
            }
            
            break;
        }
        
        if (mHandle < 0) {
            throw new InterprocessRuntimeException("Failed to open shared memory");
        }
    }

    @Override
    public void truncate(int size) {
        int ret = SemaphoreAPI.INSTANCE.posix_fallocate(mHandle, 0, size);
        
        if (ret != 0 && ret != ErrNo.EOPNOTSUPP) {
            throw new InterprocessRuntimeException("Failed to resize shared memory");
        }
        
        if (LibCUtil.ftruncate(mHandle, size) != 0) {
            throw new InterprocessRuntimeException("Failed to truncate shared memory");
        }
    }

    @Override
    public void close() {
        if (mHandle != 0) {
            LibC.INSTANCE.close(mHandle);
            mHandle = 0;
        }
    }
    
    @Override
    public boolean clean(ca.wise.SharedBlock.SharedMemoryDetails details) {
        boolean changed = false;
        for (ca.wise.SharedBlock.CpuDetails cpu : details.cpus) {
            for (int i = cpu.pids.size() - 1; i >= 0; i--) {
                int status;
                try {
                    status = SemaphoreAPI.INSTANCE.kill(cpu.pids.get(i), 0);
                }
                catch (Exception e) {
                    status = -1;
                }
                
                if (status < 0) {
                    cpu.pids.remove(i);
                    changed = true;
                }
            }
        }
        
        return changed;
    }
    
    MappingHandle getMappingHandle() {
        MappingHandle ret = new MappingHandle();
        ret.handle = mHandle;
        ret.isXsi = false;
        return ret;
    }
    
    public static class MappingHandle {
        public int handle;
        
        public boolean isXsi;
    }
}
