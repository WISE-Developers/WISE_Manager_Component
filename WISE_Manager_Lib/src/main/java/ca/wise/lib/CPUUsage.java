package ca.wise.lib;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import lombok.Getter;

/**
 * Stores details on which CPU group and which physical cores
 * within the group a running job is using.
 */
@Immutable
public class CPUUsage {
    
    /**
     * A CPU usage instance that is not using any cores.
     */
    public static final CPUUsage ZERO = new CPUUsage(-1, BigInteger.ZERO, 0);
    
    /**
     * Which NUMA node the job is restricted to. Will be -1 if it isn't restricted.
     */
    @Getter protected final int numaNode;
    
    /**
     * The cores of either the system if {@link #numaNode} is -1 or the NUMA node
     * if {@link #numaNode} is >0 that the job is running on.
     */
    @Getter protected final BigInteger runningCores;
    
    /**
     * The maximum number of cores that the job can run on.
     */
    @Getter protected final int maxCores;
    
    public CPUUsage(int numaNode, BigInteger runningCores, int maxCores) {
        this.numaNode = numaNode;
        this.runningCores = runningCores;
        this.maxCores = maxCores;
    }
    
    /**
     * Create a list of blocks that jobs can be run on.
     * @param numaLock Should jobs only run on a single NUMA node.
     * @param jobCount The number of simultaneous jobs that should run, either on each NUMA node or in total.
     */
    public static List<CPUUsage> computeAvailableBlocks(boolean numaLock, int jobCount, int skipCount) {
        return computeAvailableBlocks(numaLock, jobCount, skipCount, new CPUInfo());
    }
    
    static List<CPUUsage> computeAvailableBlocks(boolean numaLock, int jobCount, int skipCount, CPUInfo info) {
        List<CPUUsage> retval = new ArrayList<>();
        //make sure the number of jobs is valid, default to 1
        if (jobCount < 1)
            jobCount = 1;
        
        //run all jobs across all CPUs
        long blocks = 1;
        //unless NUMA locking is enabled then lock them to individual nodes
        if (numaLock)
            blocks = info.numaCount();
        int offset = 0;
        for (int block = 0; block < blocks; block++) {
            long totalSize;
            //if NUMA locking is enabled get the number of cores in the current node
            if (numaLock)
                totalSize = info.coreCount(block);
            //otherwise use the total number of CPUs
            else
                totalSize = info.coreCount();
            long blockSize;
            //if jobs will span all NUMA nodes and there is only one node then leave two CPUs without a job
            if (info.numaCount() == 1 || (info.numaCount() > 1 && !numaLock)) {
                if (skipCount == 1)
                    blockSize = totalSize - 1;
                else if (skipCount < 1)
                    blockSize = totalSize - 2;
                else
                    blockSize = totalSize;
            }
            //otherwise leave one CPU without a job
            else {
                if (skipCount < 1)
                    blockSize = totalSize - 1;
                else
                    blockSize = totalSize;
            }
            long tempSize = blockSize - skipCount;
            if (tempSize < 2 && blockSize >= 2)
                blockSize = 2;
            else if (tempSize >= 2)
                blockSize = tempSize;
            //make sure there are some CPUs to use
            if (blockSize < 1) {
                if (totalSize <= 1)
                    blockSize = 1;
                else
                    blockSize = totalSize - 1;
            }
            //the number of CPUs that will be available to each job
            long cpuPerJob = blockSize / jobCount;
            if (cpuPerJob < 1)
                cpuPerJob = 1;
            //the number of jobs that will run on a full number of CPUs, the rest will run -1
            long fullSize = jobCount - (blockSize % jobCount);
            //the offset into the total CPU set that the current job would be allowed to run on
            for (int i = 0; i < jobCount; i++) {
                //the index into the sets of CPUs that this job would run on
                //the index can wrap if more jobs per node have been requested than there are CPUs
                long index = i % blockSize;
                long cpus;
                if (jobCount <= blockSize && index >= fullSize)
                    cpus = cpuPerJob + 1;
                else
                    cpus = cpuPerJob;
                
                BigInteger mask = BigInteger.ZERO;
                //the offset starts with the offset of the current block
                int myOffset = offset;
                //add up all sets within this block before this one, can't use
                //a running total because if the user asks for too many processes
                //it will wrap
                for (int j = 0; j < index; j++) {
                    if (jobCount <= blockSize && j >= fullSize)
                        myOffset += cpuPerJob + 1;
                    else
                        myOffset += cpuPerJob;
                }
                //set the bits that will be the CPUs that the job would be allowed to run on
                for (int j = 0; j < cpus; j++) {
                    mask = mask.setBit(myOffset + j);
                }
                //create the CPU usage spec
                CPUUsage usage = new CPUUsage(numaLock ? block : -1, mask, (int)cpus);
                retval.add(usage);
            }
            
            offset += totalSize;
        }
        
        return retval;
    }
}
