package ca.wise.lib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CPUInfo {

    private static ReentrantLock lock = new ReentrantLock();
    
    private static CentralProcessor _processor = null;
    private static Long _groupCount = null;
    private static Map<Integer, Long> _coreCounts = new HashMap<>();
    
    public CPUInfo() { }
    
    private static void initialize() {
        if (_processor == null) {
            SystemInfo info = new SystemInfo();
            _processor = info.getHardware().getProcessor();
        }
    }

    /**
     * Get the number of NUMA nodes in the system.
     */
    public long numaCount() {
        return staticNumaCount();
    }
    
    /**
     * Get the number of NUMA nodes in the system.
     */
    public static long staticNumaCount() {
        lock.lock();
        try {
            initialize();
            if (_groupCount == null) {
                _groupCount = _processor.getLogicalProcessors().stream()
                        .map(x -> x.getNumaNode())
                        .distinct()
                        .count();
            }
            return _groupCount;
        }
        finally {
            lock.unlock();
        }
    }
    
    /**
     * Get the number of processors in the NUMA node.
     * @param numaNode The NUMA node to check.
     */
    public long coreCount(int numaNode) {
        return staticCoreCount(numaNode);
    }
    
    /**
     * Get the number of processors in the NUMA node.
     * @param numaNode The NUMA node to check.
     */
    public static long staticCoreCount(int numaNode) {
        lock.lock();
        try {
            initialize();
            return _coreCounts.computeIfAbsent(numaNode, number -> {
                if (numaNode < 0)
                    return Long.valueOf(_processor.getLogicalProcessorCount());
                else if (numaNode >= staticNumaCount())
                    return 0L;
                return _processor.getLogicalProcessors().stream()
                    .filter(x -> x.getNumaNode() == numaNode)
                    .count();
            });
        }
        finally {
            lock.unlock();
        }
    }
    
    /**
     * Get the total number of processors on this machine.
     * @return
     */
    public long coreCount() {
        return staticCoreCount();
    }
    
    /**
     * Get the total number of processors on this machine.
     * @return
     */
    public static long staticCoreCount() {
        lock.lock();
        try {
            initialize();
            return _coreCounts.computeIfAbsent(-1, number -> Long.valueOf(_processor.getLogicalProcessorCount()));
        }
        finally {
            lock.unlock();
        }
    }
}
