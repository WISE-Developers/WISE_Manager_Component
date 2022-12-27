package ca.wise;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sun.jna.Pointer;

public class SharedBlock {

    private NamedMutex mutex;
    private SharedMemory memory;
    
    /**
     * This was one of the last CPUs that was allocated.
     */
    public static final byte CPU_OPTION_LAST_USED = 1;
    /**
     * The CPU is reserved for future use. Used so that Manager can reserve a CPU
     * before it runs W.I.S.E. without worrying about a race condition.
     */
    public static final byte CPU_OPTION_RESERVE = 2;

    public static final byte OPTION_NUMA_LOCK = 1;
    
    private static final byte SHARED_MEMORY_VERSION = 1;
    
    public SharedBlock(NamedMutex mutex, SharedMemory memory) {
        this.mutex = mutex;
        this.memory = memory;
    }
    
    public void useSpecificBlock(List<Integer> indices) throws InterprocessException {
        try (Locker locker = new Locker(mutex)) {
            SharedMemoryDetails details = loadDetails();
            
            for (Integer index : indices) {
                if (index < details.cpus.size()) {
                    details.cpus.get(index).options |= CPU_OPTION_RESERVE;
                }
            }
            
            saveDetails(details);
        }
    }
    
    /**
     * Operate on the shared memory block inside the named mutex lock. If an error
     * occurs while locking the mutex the method will be called anyways. That way
     * if there are platform issues with the shared mutexes the caller can still
     * function. Automatically removes jobs that have closed from the pid lists
     * if they got caught there due to a crash.
     * @param method The method to run inside the mutex lock.
     */
    public void withSafeLock(Consumer<SharedMemoryDetails> method) {
        Locker locker = null;
        try {
            locker = new Locker(mutex);
        }
        catch (Exception e) { }
        
        SharedMemoryDetails details = null;
        try {
            details = loadDetails();
            if (memory.clean(details))
                saveDetails(details);
        }
        catch (Exception e) { }
        
        try {
            method.accept(details);
        }
        finally {
            if (locker != null)
                locker.close();
        }
    }
    
    public SharedMemoryDetails getCurrentConfig() throws InterprocessException {
        try (Locker locker = new Locker(mutex)) {
            return loadDetails();
        }
    }
    
    /**
     * Update the shared memory block if it is out of sync with the current settings or if it doesn't yet exist.
     * @param jobCount The current configuration for the number of jobs that should be run.
     * @param cpuSkip The current configuration for the number of CPUs to leave idle for the system to run on.
     * @param numaLock The current configuration for whether jobs should be locked to NUMA nodes.
     */
    public void updateConfig(byte jobCount, short cpuSkip, boolean numaLock) {
        try (Locker locker = new Locker(mutex)) {
            boolean needsUpdate = false;
            SharedMemoryDetails details = null;
            try {
                details = loadDetails();
                if (details.jobCount != jobCount ||
                        details.cpuSkip != cpuSkip ||
                        details.options != (numaLock ? OPTION_NUMA_LOCK : 0))
                    needsUpdate = true;
            }
            catch (Exception e2) {
                needsUpdate = true;
            }
            if (needsUpdate) { 
                if (details == null) {
                    details = new SharedMemoryDetails();
                    
                    for (int node = 0; node < InterprocessUtils.getNumaCount(); node++) {
                        for (int cpu = 0 ; cpu < InterprocessUtils.getProcessors(node); cpu++) {
                            CpuDetails c = new CpuDetails();
                            if (numaLock)
                                c.numaNode = (byte)node;
                            else
                                c.numaNode = 0;
                            c.options = 0;
                            details.cpus.add(c);
                        }
                    }
                }
                
                details.jobCount = jobCount;
                details.cpuSkip = cpuSkip;
                details.options = numaLock ? OPTION_NUMA_LOCK : 0;
                
                saveDetails(details);
            }
        }
        catch (InterprocessException e) { }
    }
    
    /**
     * Mark a set of CPU cores as reserved for a new job.
     * @param details The already loaded configuration details.
     * @param cores A bitmask of the cores that should be marked reserved.
     */
    public void reserveBlock(SharedMemoryDetails details, BigInteger cores) {
        for (int i = 0; i < details.cpus.size(); i++) {
            if (cores.testBit(i))
                details.cpus.get(i).options |= CPU_OPTION_RESERVE;
            else
                details.cpus.get(i).options &= ~CPU_OPTION_RESERVE;
        }
        
        saveDetails(details);
    }
    
    private void saveDetails(SharedMemoryDetails details) {
        BOStream stream = new BOStream();
        stream.putByte(SHARED_MEMORY_VERSION);
        stream.putByte(details.options);
        stream.putShort(details.cpuSkip);
        stream.putByte(details.jobCount);
        stream.putShort((short)details.cpus.size());
        for (CpuDetails c : details.cpus) {
            stream.putByte(c.numaNode);
            stream.putByte(c.options);
            stream.putShort((short)c.pids.size());
            for (Integer pid : c.pids) {
                stream.putInt(pid);
            }
        }
        
        memory.truncate(stream.size());
        
        try (MappedRegion region = new MappedRegion(memory, MappedRegion.READ_WRITE)) {
            Pointer addr = region.getAddress();
            int index = 0;
            for (Byte b : stream.bytes) {
                addr.setByte(index++, b);
            }
        }
    }
    
    private SharedMemoryDetails loadDetails() {
        try (MappedRegion region = new MappedRegion(memory, MappedRegion.READ_ONLY)) {
            SharedMemoryDetails retval = new SharedMemoryDetails();
            BIStream stream = new BIStream(region.getAddress(), region.getSize());
            byte version = stream.getByte();
            if (version != SHARED_MEMORY_VERSION)
                throw new InterprocessRuntimeException("Invalid shared memory version " + version);
            
            retval.options = stream.getByte();
            retval.cpuSkip = stream.getShort();
            retval.jobCount = stream.getByte();
            short cpuCount = stream.getShort();
            for (short i = 0; i < cpuCount; i++) {
                CpuDetails c = new CpuDetails();
                c.numaNode = stream.getByte();
                c.options = stream.getByte();
                short pidCount = stream.getShort();
                for (short j = 0; j < pidCount; j++) {
                    c.pids.add(stream.getInt());
                }
                retval.cpus.add(c);
            }
            
            return retval;
        }
    }
    
    public static class CpuDetails {
        public byte numaNode;
        public byte options;
        public List<Integer> pids = new ArrayList<Integer>();
    }
    
    public static class SharedMemoryDetails {
        public List<CpuDetails> cpus = new ArrayList<CpuDetails>();
        public byte options;
        public short cpuSkip;
        public byte jobCount;
        
        public BigInteger getUsedMask() {
            BigInteger retval = BigInteger.ZERO;
            
            for (int i = 0; i < cpus.size(); i++) {
                if (cpus.get(i).pids.size() > 0)
                    retval = retval.setBit(i);
            }
            
            return retval;
        }
    }
    
    private static class BIStream {
        private Pointer addr;
        private int size;
        private int index = 0;
        
        public BIStream(Pointer base, int length) {
            addr = base;
            size = length;
        }
        
        public byte getByte() {
            if (index >= size)
                return 0;
            return addr.getByte(index++);
        }
        
        public short getShort() {
            if (index >= (size - 1))
                return 0;
            short val = addr.getShort(index);
            index += 2;
            return val;
        }
        
        public int getInt() {
            if (index >= (size - 3))
                return 0;
            int val = addr.getInt(index);
            index += 4;
            return val;
        }
    }
    
    private static class BOStream {
        private List<Byte> bytes = new ArrayList<Byte>();
        
        public void putByte(byte val) {
            bytes.add(val);
        }
        
        public void putShort(short val) {
            bytes.add((byte)(val & 0xFF));
            bytes.add((byte)((val >> 8) & 0xFF));
        }
        
        public void putInt(int val) {
            bytes.add((byte)(val & 0xFF));
            bytes.add((byte)((val >> 8) & 0xFF));
            bytes.add((byte)((val >> 16) & 0xFF));
            bytes.add((byte)((val >> 24) & 0xFF));
        }
        
        public int size() {
            return bytes.size();
        }
    }
}
