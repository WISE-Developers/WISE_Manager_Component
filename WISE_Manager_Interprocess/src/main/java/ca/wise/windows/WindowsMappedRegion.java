package ca.wise.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;

import ca.wise.IMappedRegionWrapper;
import ca.wise.InterprocessRuntimeException;
import ca.wise.MappedRegion;
import ca.wise.windows.NtDllEx.SECTION_BASIC_INFORMATION;
import ca.wise.windows.WindowsSharedMemory.MappingHandle;

public class WindowsMappedRegion implements IMappedRegionWrapper {
    
    private Pointer mBase;
    private long mSize;
    private HANDLE mFileOrMappingHandle;

    public WindowsMappedRegion(WindowsSharedMemory mapping, int mode) {
        MappingHandle mHandle = mapping.getMappingHandle();
        HANDLE nativeMappingHandle = null;
        
        int protection = 0;
        int mapAccess = 0;
        switch (mode) {
        case MappedRegion.READ_ONLY:
        case MappedRegion.READ_PRIVATE:
            protection |= Kernel32.PAGE_READONLY;
            mapAccess |= FileAPI.FILE_MAP_READ;
            break;
        case MappedRegion.READ_WRITE:
            protection |= Kernel32.PAGE_READWRITE;
            mapAccess |= FileAPI.FILE_MAP_WRITE;
            break;
        case MappedRegion.COPY_ON_WRITE:
            protection |= Kernel32.PAGE_WRITECOPY;
            mapAccess |= FileAPI.FILE_MAP_COPY;
            break;
        }
        
        HANDLE handleToClose = Kernel32.INVALID_HANDLE_VALUE;
        if (!mHandle.isShm) {
            nativeMappingHandle = Kernel32.INSTANCE.CreateFileMapping(mHandle.handle, null, protection, 0, 0, null);
            if (nativeMappingHandle == null) {
                int error = Kernel32.INSTANCE.GetLastError();
                throw new InterprocessRuntimeException("Failed to create file mapping: " + error);
            }
            handleToClose = nativeMappingHandle;
        }
        else {
            nativeMappingHandle = mHandle.handle;
        }
        
        try {
            IntByReference result = new IntByReference(0);
            SECTION_BASIC_INFORMATION.ByReference sbc = new SECTION_BASIC_INFORMATION.ByReference();
            NtDllEx.INSTANCE.NtQuerySection(nativeMappingHandle, NtDllEx.SECTION_BASIC_INFORMATION, sbc, sbc.size(), result);
            long mappingSize = sbc.sectionSize;
            long size = sizeFromMappingSize(mappingSize, 0, 0);
            
            Pointer base = FileAPI.INSTANCE.MapViewOfFileEx(nativeMappingHandle, mapAccess, 0, 0, (int)size, null);
            
            if (base == null) {
                int err = Kernel32.INSTANCE.GetLastError();
                throw new InterprocessRuntimeException("Failed to map view of file: " + err);
            }
            
            mBase = base;
            mSize = size;
        }
        finally {
            if (handleToClose != null && handleToClose != Kernel32.INVALID_HANDLE_VALUE) {
                Kernel32.INSTANCE.CloseHandle(handleToClose);
            }
        }
        
        if (!Kernel32.INSTANCE.DuplicateHandle(Kernel32.INSTANCE.GetCurrentProcess(), mHandle.handle,
                Kernel32.INSTANCE.GetCurrentProcess(), new HANDLEByReference(mFileOrMappingHandle),
                0, false, 2)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new InterprocessRuntimeException("Failed to duplicate handle: " + err);
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
            destroySyncsInRange(mBase, mSize);
            Kernel32.INSTANCE.UnmapViewOfFile(mBase);
            mBase = null;
        }
        if (mFileOrMappingHandle != Kernel32.INVALID_HANDLE_VALUE) {
            Kernel32.INSTANCE.CloseHandle(mFileOrMappingHandle);
            mFileOrMappingHandle = Kernel32.INVALID_HANDLE_VALUE;
        }
    }
    
    private void destroySyncsInRange(Pointer addr, long size) {
        //do nothing for now, see if this is ever created
    }
    
    private long sizeFromMappingSize(long mappingSize, int offset, int pageOffset) {
        return (long)(mappingSize - offset);
    }
}
