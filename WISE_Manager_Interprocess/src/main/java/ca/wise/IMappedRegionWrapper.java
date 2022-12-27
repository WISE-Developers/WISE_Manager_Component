package ca.wise;

import com.sun.jna.Pointer;

public interface IMappedRegionWrapper {

    int getSize();
    
    Pointer getAddress();
    
    void close();
}
