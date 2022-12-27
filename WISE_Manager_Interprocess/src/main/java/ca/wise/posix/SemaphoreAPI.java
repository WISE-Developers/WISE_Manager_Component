package ca.wise.posix;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public interface SemaphoreAPI extends Library {
    
    public static final int IPC_RMID = 0;
    public static final int IPC_SET = 1;
    public static final int IPC_STAT = 2;
    public static final int IPC_INFO = 3;
    
    public static final int S_IFREG = 100000;
    
    /* Get record locking info.  */
    public static final int F_GETLK = 5;
    /* Set record locking info (non-blocking).  */
    public static final int F_SETLK = 6;
    /* Set record locking info (blocking).  */
    public static final int F_SETLKW = 7;
    
    public static final int F_RDLCK = 0;
    public static final int F_WRLCK = 1;
    public static final int F_UNLCK = 2;
    
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    public static final int _STAT_VER = 1;

    public static final SemaphoreAPI INSTANCE = Native.load("c", SemaphoreAPI.class);
    
    Pointer sem_open(String name, int flags, int mode, int value) throws LastErrorException;
    
    Pointer sem_open(String name, int flags) throws LastErrorException;
    
    int sem_post(Pointer sem) throws LastErrorException;
    
    int sem_close(Pointer sem);
    
    int sem_wait(Pointer sem) throws LastErrorException;
    
    int sem_trywait(Pointer sem) throws LastErrorException;
    
    int fchmod(int fildes, int mode);
    
    int posix_fallocate(int fd, int offset, int len);
    
    int shmctl(int shmid, int cmd, shmid_ds buf);
    
    Pointer shmat(int shmid, Pointer shmaddr, int shmflg);
    
    int fstat(int fd, stat buf);
    
    int __fxstat(int ver, int fd, stat buf);
    
    public static int fxstatHelper(int fd, stat buf) {
    	try {
    		return INSTANCE.fstat(fd, buf);
    	}
    	catch (UnsatisfiedLinkError e) {
    		return INSTANCE.__fxstat(_STAT_VER, fd, buf);
    	}
    }
    
    int shmdt(Pointer shmaddr);
    
    int get_nprocs();
    
    int kill(int pid, int sig) throws LastErrorException;
    
    int mkdir(String path, int mode);
    
    int chmod(String path, int mode);
    
    int mknod(String path, int mode, NativeLong dev);
    
    int __xmknod(String path, int mode, NativeLong dev);
    
    int open(String file, int oflag, Object... varargs);
    
    int close(int fd);
    
    int fcntl(int fd, int cmd, Object... varargs) throws LastErrorException;
    
    public static int mknodHelper(String path, int mode, NativeLong dev) {
    	try {
    		return INSTANCE.mknod(path, mode, dev);
    	}
    	catch (UnsatisfiedLinkError e) {
    		return INSTANCE.__xmknod(path, mode, dev);
    	}
    }
    
    @FieldOrder({ "tv_sec", "tv_nsec" })
    public static class timespec extends Structure {
    	public NativeLong tv_sec;
    	
    	public NativeLong tv_nsec;
    }
    
    @FieldOrder({ "st_dev", "st_ino", "st_nlink", "st_mode", "st_uid", "st_gid", "__pad0", "st_rdev", "st_size", "st_blksize", "st_blocks", "st_atim", "st_mtim", "st_ctim", "__unused" })
    public static class stat extends Structure {
        public NativeLong st_dev;
        
        public NativeLong st_ino;
        
        public NativeLong st_nlink;
        
        public int st_mode;
        
        public int st_uid;
        
        public int st_gid;
        
        public int __pad0;
        
        public NativeLong st_rdev;
        
        public NativeLong st_size;
        
        public NativeLong st_blksize;
        
        public NativeLong st_blocks;
        
        public timespec st_atim;
        
        public timespec st_mtim;
        
        public timespec st_ctim;
        
        public NativeLong[] __unused = new NativeLong[3];
    }
    
    @FieldOrder({ "__key", "uid", "gid", "cuid", "cgid", "mode", "__seq" })
    public static class ipc_perm  extends Structure {
        public int __key;
        
        public int uid;
        
        public int gid;
        
        public int cuid;
        
        public int cgid;
        
        public short mode;
        
        public short __seq;
    }

    @FieldOrder({ "shm_perm", "shm_segsz", "shm_atime", "shm_dtime", "shm_ctime", "shm_cpid", "shm_lpid", "shm_nattch" })
    public static class shmid_ds extends Structure {
        public ipc_perm shm_perm;
        
        public long shm_segsz;
        
        public long shm_atime;
        
        public long shm_dtime;
        
        public long shm_ctime;
        
        public int shm_cpid;
        
        public int shm_lpid;
        
        public long shm_nattch;
    }
    
	public static class __pid_t extends PointerType {
		public __pid_t(Pointer address) {
			super(address);
		}
		public __pid_t() {
			super();
		}
	};
	
	public static class __off_t extends PointerType {
		public __off_t(Pointer address) {
			super(address);
		}
		public __off_t() {
			super();
		}
	};

    @FieldOrder({ "l_type", "l_whence", "l_start", "l_len", "l_pid" })
    public class flock extends Structure {
    	/** Type of lock: F_RDLCK, F_WRLCK, or F_UNLCK. */
    	public short l_type;
    	
    	/** Where `l_start' is relative to (like `lseek'). */
    	public short l_whence;
    	
    	/**
    	 * Offset where the lock begins.<br>
    	 * C type : __off_t
    	 */
    	public __off_t l_start;
    	
    	/**
    	 * Size of the locked area; zero means until EOF.<br>
    	 * C type : __off_t
    	 */
    	public __off_t l_len;
    	
    	/**
    	 * Process holding the lock.<br>
    	 * C type : __pid_t
    	 */
    	public __pid_t l_pid;
    }
}
