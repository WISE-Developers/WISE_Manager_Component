package ca.wise;

public interface INamedMutexWrapper {

    boolean openOrCreate(String name);
    
    void close();
    
    void lock() throws InterprocessException;
    
    boolean tryLock() throws InterprocessException;
    
    void unlock();
}
