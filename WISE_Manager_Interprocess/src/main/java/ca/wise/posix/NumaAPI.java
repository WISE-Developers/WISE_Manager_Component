package ca.wise.posix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public interface NumaAPI extends Library {

    public static final NumaAPI INSTANCE = Native.load("numa", NumaAPI.class);
    
    int numa_available();
    
    int numa_max_node();
    
    bitmask numa_allocate_cpumask();
    
    int numa_node_to_cpus(int node, bitmask mask);
    
    int numa_bitmask_weight(bitmask mask);
    
    @FieldOrder({ "size", "maskp" })
    public static class bitmask extends Structure {
        public long size;
        public Pointer maskp;
    }
}
