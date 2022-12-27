package ca.wise.windows;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.Structure.FieldOrder;

public interface NtDllEx extends StdCallLibrary {
    
    public static final int SECTION_BASIC_INFORMATION = 0;
    public static final int SECTION_IMAGE_INFORMATION = 1;

    NtDllEx INSTANCE = Native.load("NtDll", NtDllEx.class, W32APIOptions.DEFAULT_OPTIONS);
    
    int NtQuerySection(HANDLE hFile, int nInformationClass, SECTION_BASIC_INFORMATION.ByReference lpInformation, int nInformationSize, IntByReference lpResultLength);
    
    @FieldOrder({ "baseAddress", "sectionAttributes", "sectionSize" })
    public static class SECTION_BASIC_INFORMATION extends Structure {
        public static class ByReference extends SECTION_BASIC_INFORMATION implements Structure.ByReference {
        }
        
        //this is actually a void*, maybe I should make this a Pointer
        public long baseAddress;
        
        public int sectionAttributes;
        
        public long sectionSize;
    }
}
