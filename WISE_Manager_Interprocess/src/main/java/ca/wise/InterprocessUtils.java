package ca.wise;

import java.util.List;
import java.util.Locale;

public class InterprocessUtils {
    
    private static final char characters[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    private static List<ThreadGroup> numaNodes = null;
    
    public static String dwordToString(int dword) {
        String retval = "";
        for (int i = 0; i < 4; i++) {
            int val = (dword >> (i * 8)) & 0xFF;
            retval += characters[(val & 0xF0) >> 4];
            retval += characters[(val & 0x0F)];
        }
        
        return retval;
    }

    public static String bytesToString(byte[] mem) {
        String retval = "";
        for (int i = 0; i < mem.length; i++) {
            byte val = mem[i];
            retval += characters[(val & 0xF0) >> 4];
            retval += characters[(val & 0x0F)];
        }
        return retval;
    }
    
    private static void initNumaNodes() {
        if (numaNodes == null) {
            String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (os.contains("mac") || os.contains("darwin"))
                throw new InterprocessRuntimeException("Unsupported platform");
            else if (os.contains("win"))
                numaNodes = ca.wise.windows.InterprocessPlatformUtils.getThreadGroups();
            else if (os.contains("nux"))
                numaNodes = ca.wise.posix.InterprocessPlatformUtils.getThreadGroups();
            else
                throw new InterprocessRuntimeException("Unsupported platform");
        }
    }
    
    public static int getNumaCount() {
        initNumaNodes();
        
        return numaNodes.size();
    }
    
    public static int getProcessors() {
        initNumaNodes();
        
        return numaNodes.stream().map(x -> x.numThreads).reduce(0, Integer::sum);
    }
    
    public static int getProcessors(int node) {
        initNumaNodes();
        
        if (node >= numaNodes.size())
            return 0;
        
        return numaNodes.get(node).numThreads;
    }
    
    public static class ThreadGroup {
        public int numaIndex;
        public int numThreads;
    }
}
