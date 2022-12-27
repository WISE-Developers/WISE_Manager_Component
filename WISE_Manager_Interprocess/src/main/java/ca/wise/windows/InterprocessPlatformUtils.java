package ca.wise.windows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.ULONGByReference;
import com.sun.jna.platform.win32.WinDef.USHORT;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.GROUP_AFFINITY;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg;

import ca.wise.InterprocessUtils;

public class InterprocessPlatformUtils {
    
    private static final String sharedDirectory = "boost_interprocess";

    /**
     * Get an ID using the last boot time.
     * @return An ID that should be unique for the current boot session.
     */
    public static Optional<String> getLastBootupTime() {
        Integer bootId = null;
        Integer animationTime = null;
        try {
            bootId = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management\\PrefetchParameters", "BootId");
        }
        catch (Exception e) { }
        try {
            animationTime = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Power", "HybridBootAnimationTime");
        }
        catch (Exception e) { }
        
        if (bootId != null || animationTime != null) {
            String retval = "";
            
            if (bootId != null)
                retval += StringUtils.rightPad(InterprocessUtils.dwordToString(bootId), 8, '0');
            if (animationTime != null) {
                if (retval.length() > 0)
                    retval += "_";
                retval += StringUtils.rightPad(InterprocessUtils.dwordToString(animationTime), 8, '0');
            }
            
            return Optional.of(retval);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the shared directory for interprocess files. The directory will be specific to the current boot session.
     * @return The shared directory.
     */
    public static Optional<String> getSharedDirectory() {
        try {
            String root = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Common AppData");
            if (!Strings.isNullOrEmpty(root)) {
                Optional<String> bootId = getLastBootupTime();
                if (bootId.isPresent() && !Strings.isNullOrEmpty(bootId.get())) {
                    root += File.separator;
                    root += sharedDirectory;
                    root += File.separator;
                    root += bootId.get();
                    return Optional.of(root);
                }
            }
        }
        catch (Exception e) { }
        
        return Optional.empty();
    }
    
    private static void deleteSubdirectories(Path toDelete, String keep) {
        for (File file : toDelete.toFile().listFiles()) {
            if (file.isDirectory() && !file.getName().equalsIgnoreCase(keep)) {
                try {
                    FileUtils.deleteDirectory(file);
                }
                catch (Exception e) { }
            }
        }
    }
    
    /**
     * Get the path to the interprocess shared directory and create all directories in the path if they don't exist.
     * @return The shared directory or nothing if the directory couldn't be created for any reason.
     */
    public static Optional<String> createSharedDirAndCleanOld() {
        try {
            Optional<String> dir = getSharedDirectory();
            if (dir.isPresent()) {
                Path path = Paths.get(dir.get());
                Files.createDirectories(path);
                
                deleteSubdirectories(path.getParent(), path.getFileName().toString());
                
                return dir;
            }
        }
        catch (Exception e) { }
        
        return Optional.empty();
    }
    
    /**
     * Get file permissions that are unrestricted/allow access to all users.
     */
    public static WinBase.SECURITY_ATTRIBUTES getUnrestrictedPermissions() {
        WinNT.SECURITY_DESCRIPTOR sd = new WinNT.SECURITY_DESCRIPTOR(6 * 1024);
        Advapi32.INSTANCE.InitializeSecurityDescriptor(sd, WinNT.SECURITY_DESCRIPTOR_REVISION);
        Advapi32.INSTANCE.SetSecurityDescriptorDacl(sd, true, null, false);
        WinBase.SECURITY_ATTRIBUTES sa = new WinBase.SECURITY_ATTRIBUTES();
        sa.lpSecurityDescriptor = sd.getPointer();
        sa.bInheritHandle = false;
        return sa;
    }
    
    public static List<ca.wise.InterprocessUtils.ThreadGroup> getThreadGroups() {
        List<ca.wise.InterprocessUtils.ThreadGroup> retval = new ArrayList<>();
        ULONGByReference highestNodeNumber = new ULONGByReference();
        if (FileAPI.INSTANCE.GetNumaHighestNodeNumber(highestNodeNumber) != 0) {
            //loop over all possible but not necessarily present nodes
            for (int i = 0; i <= highestNodeNumber.getValue().intValue(); i++) {
                GROUP_AFFINITY affinity = new GROUP_AFFINITY();
                //get the details of the node, if it exists
                if (FileAPI.INSTANCE.GetNumaNodeProcessorMaskEx(new USHORT(i), affinity) != 0) {
                    ca.wise.InterprocessUtils.ThreadGroup tg = new ca.wise.InterprocessUtils.ThreadGroup();
                    tg.numaIndex = i;
                    tg.numThreads = Long.bitCount(affinity.mask.longValue());
                    retval.add(tg);
                }
            }
        }
        else {
            ca.wise.InterprocessUtils.ThreadGroup tg = new ca.wise.InterprocessUtils.ThreadGroup();
            tg.numaIndex = 0;
            tg.numThreads = FileAPI.INSTANCE.GetActiveProcessorCount(FileAPI.ALL_PROCESSOR_GROUPS).intValue();
            retval.add(tg);
        }
        return retval;
    }
    
    /**
     * Is a process with the given PID currently running.
     * @param id The PID of the process to check.
     * @return True if the process is currently running.
     */
    public static boolean isProcessRunning(int id) {
        HANDLE handle = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ, false, id);
        if (handle == null || handle == Kernel32.INVALID_HANDLE_VALUE)
            return false;
        else {
            Kernel32.INSTANCE.CloseHandle(handle);
            return true;
        }
    }
}
