package ca.wise.windows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.Wbemcli.IEnumWbemClassObject;
import com.sun.jna.platform.win32.COM.Wbemcli.IWbemClassObject;
import com.sun.jna.platform.win32.COM.Wbemcli.IWbemLocator;
import com.sun.jna.platform.win32.COM.Wbemcli.IWbemServices;
import com.sun.jna.ptr.IntByReference;

import ca.wise.ISharedMemoryWrapper;

public class WindowsSharedMemory implements ISharedMemoryWrapper {
    
    private HANDLE mHandle = Kernel32.INVALID_HANDLE_VALUE;

    public WindowsSharedMemory(String name) {
        Optional<String> sharedDir = InterprocessPlatformUtils.createSharedDirAndCleanOld();
        
        Ole32.INSTANCE.CoInitialize(null);
        Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null,
                Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
        
        if (sharedDir.isPresent()) {
            Path p = Paths.get(sharedDir.get());
            p = p.resolve(name);
            mHandle = Kernel32.INSTANCE.CreateFile(p.toString(),
                    Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE,
                    Kernel32.FILE_SHARE_READ | Kernel32.FILE_SHARE_WRITE | Kernel32.FILE_SHARE_DELETE,
                    InterprocessPlatformUtils.getUnrestrictedPermissions(),
                    Kernel32.OPEN_ALWAYS,
                    0,
                    null);
        }
    }
    
    @Override
    public void truncate(int size) {
        WinNT.LARGE_INTEGER filesize = new WinNT.LARGE_INTEGER(0);
        if (FileAPI.INSTANCE.GetFileSizeEx(mHandle, filesize)) {
            //pad out the end of the file with 0s to make it bigger
            if (size > filesize.getValue()) {
                WinBase.OVERLAPPED overlapped = new WinBase.OVERLAPPED();
                FileAPI.INSTANCE.SetFilePointer(mHandle, (int)filesize.getValue(), null, FileAPI.FILE_BEGIN);
                byte data[] = new byte[512];
                for (int remaining = size - (int)filesize.getValue(), writeSize = 0; remaining >0; remaining -= writeSize) {
                    writeSize = 512 < remaining ? 512 : remaining;
                    IntByReference written = new IntByReference(0);
                    Kernel32.INSTANCE.WriteFile(mHandle, data, writeSize, written, overlapped);
                    //something happened
                    if (written.getValue() != writeSize)
                        break;
                }
            }
            //chop off the end of the file
            else {
                FileAPI.INSTANCE.SetFilePointer(mHandle, size, null, FileAPI.FILE_BEGIN);
                FileAPI.INSTANCE.SetEndOfFile(mHandle);
            }
        }
    }
    
    @Override
    public void close() {
        if (mHandle != null && mHandle != Kernel32.INVALID_HANDLE_VALUE) {
            Kernel32.INSTANCE.CloseHandle(mHandle);
            mHandle = Kernel32.INVALID_HANDLE_VALUE;
        }
    }
    
    @Override
    public boolean clean(ca.wise.SharedBlock.SharedMemoryDetails details) {
        Ole32.INSTANCE.CoInitialize(null);
        Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null,
                Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
        
        IWbemLocator pLoc = IWbemLocator.create();
        boolean changed = false;
        if (pLoc != null) {
            IWbemServices pSvc = pLoc.ConnectServer("ROOT\\CIMV2", null, null, null, 0, null, null);
            
            for (ca.wise.SharedBlock.CpuDetails cpu : details.cpus) {
                for (int i = cpu.pids.size() - 1; i >= 0; i--) {
                    try {
                        String query = "SELECT * FROM Win32_Process WHERE ProcessId=" + cpu.pids.get(i);
                        IEnumWbemClassObject pEnumerator = pSvc.ExecQuery("WQL", query,
                                Wbemcli.WBEM_FLAG_FORWARD_ONLY | Wbemcli.WBEM_FLAG_RETURN_IMMEDIATELY, null);
                        IWbemClassObject pclsObj[] = pEnumerator.Next(Wbemcli.WBEM_INFINITE, 1);
                        if (pclsObj == null || pclsObj.length == 0) {
                            cpu.pids.remove(i);
                            changed = true;
                        }
                        else {
                            pclsObj[0].Release();
                        }
                        pEnumerator.Release();
                    }
                    catch (Exception e) { }
                }
            }
            pSvc.Release();
            pLoc.Release();
        }
        
        return changed;
    }
    
    MappingHandle getMappingHandle() {
        MappingHandle ret = new MappingHandle();
        ret.handle = mHandle;
        ret.isShm = false;
        return ret;
    }
    
    public static class MappingHandle {
        public HANDLE handle;
        
        public boolean isShm;
    }
}
