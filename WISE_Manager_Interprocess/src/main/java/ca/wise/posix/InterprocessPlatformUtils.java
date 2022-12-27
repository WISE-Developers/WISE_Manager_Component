package ca.wise.posix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.platform.linux.ErrNo;

import ca.wise.posix.NumaAPI.bitmask;

public class InterprocessPlatformUtils {

    public static String addLeadingSlash(String name) {
        if (!name.startsWith(File.separator))
            name = File.separator + name;
        return name;
    }
    
    public static String createSharedDirCleaningOldAndGetFilepath(String name) {
    	String retval = "/tmp/boost_interprocess";
    	SemaphoreAPI.INSTANCE.mkdir(retval, 0777);
    	SemaphoreAPI.INSTANCE.chmod(retval, 0777);
    	retval += File.separator;
    	retval += name;
    	
    	return retval;
    }
    
    public static List<ca.wise.InterprocessUtils.ThreadGroup> getThreadGroups() {
        List<ca.wise.InterprocessUtils.ThreadGroup> retval = new ArrayList<>();
        
        if (NumaAPI.INSTANCE.numa_available() >= 0) {
            int highestNodeNumber = NumaAPI.INSTANCE.numa_max_node();
            for (int i = 0; i <= highestNodeNumber; i++) {
                bitmask mask = NumaAPI.INSTANCE.numa_allocate_cpumask();
                if (NumaAPI.INSTANCE.numa_node_to_cpus(i, mask) == 0) {
                    ca.wise.InterprocessUtils.ThreadGroup tg = new ca.wise.InterprocessUtils.ThreadGroup();
                    tg.numaIndex = i;
                    tg.numThreads = NumaAPI.INSTANCE.numa_bitmask_weight(mask);
                    retval.add(tg);
                }
            }
        }
        else {
            ca.wise.InterprocessUtils.ThreadGroup tg = new ca.wise.InterprocessUtils.ThreadGroup();
            tg.numaIndex = 0;
            tg.numThreads = SemaphoreAPI.INSTANCE.get_nprocs();
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
        try {
            SemaphoreAPI.INSTANCE.kill(id, 0);
        }
        catch (LastErrorException e) {
            if (e.getErrorCode() == ErrNo.ESRCH)
                return false;
        }
        
        return true;
    }
}
