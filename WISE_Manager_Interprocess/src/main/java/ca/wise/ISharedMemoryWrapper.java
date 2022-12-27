package ca.wise;

import ca.wise.SharedBlock.SharedMemoryDetails;

public interface ISharedMemoryWrapper {

    void truncate(int size);
    
    void close();
    
    boolean clean(SharedMemoryDetails details);
}
