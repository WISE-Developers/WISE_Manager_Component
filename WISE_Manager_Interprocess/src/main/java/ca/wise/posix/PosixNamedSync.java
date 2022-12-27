package ca.wise.posix;

import com.sun.jna.LastErrorException;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.linux.ErrNo;
import com.sun.jna.platform.linux.Fcntl;

import ca.wise.INamedMutexWrapper;
import ca.wise.InterprocessException;
import ca.wise.InterprocessRuntimeException;
import ca.wise.posix.SemaphoreAPI.__off_t;

public class PosixNamedSync implements INamedMutexWrapper {
    
    private int mHandle = -1;

    @Override
    public boolean openOrCreate(String name) {
        name = InterprocessPlatformUtils.createSharedDirCleaningOldAndGetFilepath(name);
        SemaphoreAPI.mknodHelper(name, SemaphoreAPI.S_IFREG | 0666, new NativeLong(0));
        
        mHandle = SemaphoreAPI.INSTANCE.open(name, Fcntl.O_RDWR);
        
        if (mHandle == -1)
            throw new InterprocessRuntimeException("Unable to open semaphore");
        
        return true;
    }

    @Override
    public void close() {
        if (mHandle != -1)
            SemaphoreAPI.INSTANCE.close(mHandle);
        mHandle = -1;
    }

    @Override
    public void lock() throws InterprocessException {
        try {
        	SemaphoreAPI.flock flock = new SemaphoreAPI.flock();
        	flock.l_type = SemaphoreAPI.F_WRLCK;
        	flock.l_whence = SemaphoreAPI.SEEK_SET;
        	flock.l_start = new __off_t();
        	flock.l_len = new __off_t();
        	int ret = SemaphoreAPI.INSTANCE.fcntl(mHandle, SemaphoreAPI.F_SETLKW, flock);
            if (ret != 0) {
                throw new InterprocessException("Unable to lock mutex: " + ret);
            }
        }
        catch (LastErrorException e) {
            throw new InterprocessException(e);
        }
    }

    @Override
    public boolean tryLock() throws InterprocessException {
        try {
        	SemaphoreAPI.flock flock = new SemaphoreAPI.flock();
        	flock.l_type = SemaphoreAPI.F_WRLCK;
        	flock.l_whence = SemaphoreAPI.SEEK_SET;
        	flock.l_start = new __off_t();
        	flock.l_len = new __off_t();
        	int ret = SemaphoreAPI.INSTANCE.fcntl(mHandle, SemaphoreAPI.F_SETLK, flock);
        	if (ret != -1)
        		return true;
        }
        catch (LastErrorException e) {
            if (e.getErrorCode() == ErrNo.EAGAIN || e.getErrorCode() == ErrNo.EACCES)
                return false;
            throw new InterprocessException(e);
        }
        
        throw new InterprocessException("Error locking mutex");
    }

    @Override
    public void unlock() {
        try {
        	SemaphoreAPI.flock flock = new SemaphoreAPI.flock();
        	flock.l_type = SemaphoreAPI.F_UNLCK;
        	flock.l_whence = SemaphoreAPI.SEEK_SET;
        	flock.l_start = new __off_t();
        	flock.l_len = new __off_t();
        	SemaphoreAPI.INSTANCE.fcntl(mHandle, SemaphoreAPI.F_SETLK, flock);
        }
        catch (LastErrorException e) {
            throw new InterprocessRuntimeException(e);
        }
    }
}
