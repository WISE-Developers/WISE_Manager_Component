package ca.wise.windows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import ca.wise.INamedMutexWrapper;
import ca.wise.InterprocessException;
import ca.wise.InterprocessUtils;

public class WindowsNamedSync implements INamedMutexWrapper {
    
    private final int errorSharingViolationTries = 3;
    private final int errorSharingViolationSleepMs = 250;
    
    private HANDLE mFileHnd = Kernel32.INVALID_HANDLE_VALUE;
    private HANDLE mMtxHnd = null;
    
    public WindowsNamedSync() {
        Ole32.INSTANCE.CoInitialize(null);
        Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null,
                Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
    }

    @Override
    public boolean openOrCreate(String name) {
        Optional<String> sharedDir = InterprocessPlatformUtils.createSharedDirAndCleanOld();
        
        boolean success = false;
        if (sharedDir.isPresent()) {
            Path p = Paths.get(sharedDir.get());
            p = p.resolve(name);
            try {
                mFileHnd = Kernel32.INVALID_HANDLE_VALUE;
                //open or create the file
                for (int attempt = 0; attempt < errorSharingViolationTries; attempt++) {
                    mFileHnd = Kernel32.INSTANCE.CreateFile(p.toString(),
                            Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE,
                            Kernel32.FILE_SHARE_READ | Kernel32.FILE_SHARE_WRITE | Kernel32.FILE_SHARE_DELETE,
                            InterprocessPlatformUtils.getUnrestrictedPermissions(),
                            Kernel32.OPEN_ALWAYS,
                            0,
                            null);
                    //break if we retrieved a valid handle
                    if (mFileHnd != null && mFileHnd != Kernel32.INVALID_HANDLE_VALUE)
                        break;
                    //break if someone else is already locking the file
                    else if (Kernel32.INSTANCE.GetLastError() != Kernel32.ERROR_SHARING_VIOLATION)
                        break;
                    //wait for a bit and maybe try again
                    else
                        Thread.sleep(errorSharingViolationSleepMs);
                }
                
                if (mFileHnd != null && mFileHnd != Kernel32.INVALID_HANDLE_VALUE) {
                    WinBase.OVERLAPPED overlapped = new WinBase.OVERLAPPED();
                    //lock the file
                    if (FileAPI.INSTANCE.LockFileEx(mFileHnd,
                            FileAPI.LOCKFILE_EXCLUSIVE_LOCK,
                            0,
                            8, //sizeof(long)
                            0,
                            overlapped)) {
                        try {
                            WinNT.LARGE_INTEGER filesize = new WinNT.LARGE_INTEGER(0);
                            if (FileAPI.INSTANCE.GetFileSizeEx(mFileHnd, filesize)) {
                                IntByReference writtenOrRead = new IntByReference(0);
                                byte[] uniqueIdVal = new byte[8];
                                if (filesize.getValue() != 8) {
                                    FileAPI.INSTANCE.SetEndOfFile(mFileHnd);
                                    long uniqueId = System.nanoTime();
                                    for (int i = 0, j = 0; i < 8; i++, j += 8) {
                                        uniqueIdVal[i] = (byte)((uniqueId >> j) & 0xFF);
                                    }
                                    if (Kernel32.INSTANCE.WriteFile(mFileHnd, uniqueIdVal, 8, writtenOrRead, null) &&
                                            writtenOrRead.getValue() == 8) {
                                        success = true;
                                    }
                                    FileAPI.INSTANCE.GetFileSizeEx(mFileHnd, filesize);
                                    assert(filesize.getValue() == 8);
                                }
                                else {
                                    if (Kernel32.INSTANCE.ReadFile(mFileHnd, uniqueIdVal, 8, writtenOrRead, null) &&
                                            writtenOrRead.getValue() == 8) {
                                        success = true;
                                    }
                                }
                                
                                if (success) {
                                    String uniqueIdName = InterprocessUtils.bytesToString(uniqueIdVal);
                                    String auxStr = "Global\\bipc.mut." + uniqueIdName;
                                    mMtxHnd = Kernel32.INSTANCE.CreateMutex(InterprocessPlatformUtils.getUnrestrictedPermissions(),
                                            false,
                                            auxStr);
                                    success = mMtxHnd != null && mMtxHnd != Kernel32.INVALID_HANDLE_VALUE;
                                }
                            }
                        }
                        finally {
                            FileAPI.INSTANCE.UnlockFileEx(mFileHnd, 0, 8, 0, overlapped);
                        }
                    }
                }
            }
            catch (Exception e) {
            }
            finally {
                if (!success) {
                    if (mFileHnd != Kernel32.INVALID_HANDLE_VALUE ) {
                        Kernel32.INSTANCE.CloseHandle(mFileHnd);
                        mFileHnd = Kernel32.INVALID_HANDLE_VALUE;
                    }
                }
            }
        }
        
        return success;
    }
    
    @Override
    public void close() {
        if (mMtxHnd != null && mMtxHnd != Kernel32.INVALID_HANDLE_VALUE) {
            Kernel32.INSTANCE.CloseHandle(mMtxHnd);
            mMtxHnd = null;
        }
        if (mFileHnd != Kernel32.INVALID_HANDLE_VALUE) {
            Kernel32.INSTANCE.CloseHandle(mFileHnd);
            mFileHnd = Kernel32.INVALID_HANDLE_VALUE;
        }
    }
    
    @Override
    public void lock() throws InterprocessException {
        doWinAPIWait(mMtxHnd, 0xFFFFFFFF);
    }
    
    @Override
    public boolean tryLock() throws InterprocessException {
        return doWinAPIWait(mMtxHnd, 0);
    }
    
    @Override
    public void unlock() {
        Kernel32.INSTANCE.ReleaseMutex(mMtxHnd);
    }
    
    private static boolean doWinAPIWait(HANDLE handle, int milliseconds) throws InterprocessException {
        int ret = Kernel32.INSTANCE.WaitForSingleObject(handle, milliseconds);
        if (ret == Kernel32.WAIT_OBJECT_0)
            return true;
        else if (ret == Kernel32.WAIT_TIMEOUT)
            return false;
        else if (ret == Kernel32.WAIT_ABANDONED) {
            Kernel32.INSTANCE.ReleaseMutex(handle);
            throw new InterprocessException("The owner of the mutex is abandoned");
        }
        else {
            throw new InterprocessException("Error locking mutex: " + Kernel32.INSTANCE.GetLastError());
        }
    }
}
