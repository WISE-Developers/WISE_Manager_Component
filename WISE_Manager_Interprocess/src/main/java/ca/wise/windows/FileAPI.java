package ca.wise.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.ULONGByReference;
import com.sun.jna.platform.win32.WinDef.USHORT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.GROUP_AFFINITY;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Kernel32 methods defined in fileapi.h.
 */
public interface FileAPI extends StdCallLibrary {

    FileAPI INSTANCE = Native.load("kernel32", FileAPI.class, W32APIOptions.DEFAULT_OPTIONS);
    
    public static final WORD ALL_PROCESSOR_GROUPS = new WORD(0xffff);
    
    /**
     * The function requests an exclusive lock. Otherwise, it requests a shared lock.
     */
    public static final int LOCKFILE_EXCLUSIVE_LOCK = 2;
    
    /**
     * The function returns immediately if it is unable to acquire the requested lock. Otherwise, it waits.
     */
    public static final int LOCKFILE_FAIL_IMMEDIATELY = 1;
    
    /**
     * The starting point is zero or the beginning of the file.
     */
    public static final int FILE_BEGIN = 0;
    
    /**
     * The starting point is the current value of the file pointer.
     */
    public static final int FILE_CURRENT = 1;
    
    /**
     * The starting point is the current end-of-file position.
     */
    public static final int FILE_END = 2;
    
    public static final int SECTION_QUERY = 0x01;
    public static final int SECTION_MAP_WRITE = 0x02;
    public static final int SECTION_MAP_READ = 0x04;
    public static final int SECTION_MAP_EXECUTE = 0x08;
    public static final int SECTION_EXTEND_SIZE = 0x10;
    
    public static final int FILE_MAP_COPY = SECTION_QUERY;
    public static final int FILE_MAP_WRITE = SECTION_MAP_WRITE;
    public static final int FILE_MAP_READ = SECTION_MAP_READ;
    
    /**
     * Locks the specified file for exclusive access by the calling process. This function can operate either synchronously or asynchronously and can request either an exclusive or a shared lock.
     * @param hFile A handle to the file. The handle must have been created with either the GENERIC_READ or GENERIC_WRITE access right. For more information, see <a href="https://docs.microsoft.com/en-us/windows/desktop/FileIO/file-security-and-access-rights">File Security and Access Rights</a>.
     * @param dwFlags This parameter may be one or more of LOCKFILE_EXCLUSIVE_LOCK and LOCKFILE_FAIL_IMMEDIATELY.
     * @param dwReserved Reserved parameter; must be set to zero.
     * @param nNumberOfBytesToLockLow The low-order 32 bits of the length of the byte range to lock.
     * @param nNumberOfBytesToLockHigh The high-order 32 bits of the length of the byte range to lock.
     * @param lpOverlapped A pointer to an {@link WinBase.OVERLAPPED OVERLAPPED} structure that the function uses with the locking request. This structure, which is required, contains the file offset of the beginning of the lock range. You must initialize the hEvent member to a valid handle or zero.
     * @return If the function succeeds, the return value is nonzero (TRUE). If the function fails, the return value is zero (FALSE). To get extended error information, call {@link Kernel32#GetLastError() GetLastError}.
     */
    boolean LockFileEx(HANDLE hFile, int dwFlags, int dwReserved, int nNumberOfBytesToLockLow, int nNumberOfBytesToLockHigh, WinBase.OVERLAPPED lpOverlapped);
    
    /**
     * Unlocks a region in the specified file. This function can operate either synchronously or asynchronously.
     * @param hFile A handle to the file. The handle must have been created with either the GENERIC_READ or GENERIC_WRITE access right. For more information, see <a href="https://docs.microsoft.com/en-us/windows/desktop/FileIO/file-security-and-access-rights">File Security and Access Rights</a>.
     * @param dwReserved Reserved parameter; must be zero.
     * @param nNumberOfBytesToUnlockLow The low-order part of the length of the byte range to unlock.
     * @param nNumberOfBytesToUnlockHigh The high-order part of the length of the byte range to unlock.
     * @param lpOverlapped A pointer to an {@link WinBase.OVERLAPPED OVERLAPPED} structure that the function uses with the unlocking request. This structure contains the file offset of the beginning of the unlock range. You must initialize the hEvent member to a valid handle or zero.
     * @return If the function succeeds, the return value is nonzero. If the function fails, the return value is zero or NULL. To get extended error information, call {@link Kernel32#GetLastError() GetLastError}.
     */
    boolean UnlockFileEx(HANDLE hFile, int dwReserved, int nNumberOfBytesToUnlockLow, int nNumberOfBytesToUnlockHigh, WinBase.OVERLAPPED lpOverlapped);
    
    /**
     * Retrieves the size of the specified file.
     * @param hFile A handle to the file. The handle must have been created with the FILE_READ_ATTRIBUTES access right or equivalent, or the caller must have sufficient permission on the directory that contains the file. For more information, see <a href="https://docs.microsoft.com/en-us/windows/desktop/FileIO/file-security-and-access-rights">File Security and Access Rights</a>.
     * @param lpFileSize A pointer to a {@link WinNT.LARGE_INTEGER LARGE_INTEGER} structure that receives the file size, in bytes.
     * @return If the function succeeds, the return value is nonzero. If the function fails, the return value is zero. To get extended error information, call {@link Kernel32#GetLastError() GetLastError}.
     */
    boolean GetFileSizeEx(HANDLE hFile, WinNT.LARGE_INTEGER lpFileSize);
    
    /**
     * Sets the physical file size for the specified file to the current position of the file pointer. The physical file size is also referred to as the end of the file. The SetEndOfFile function can be used to truncate or extend a file. To set the logical end of a file, use the SetFileValidData function.
     * @param hFile A handle to the file to be extended or truncated. The file handle must be created with the GENERIC_WRITE access right. For more information, see <a href="https://docs.microsoft.com/en-us/windows/desktop/FileIO/file-security-and-access-rights">File Security and Access Rights</a>.
     * @return If the function succeeds, the return value is nonzero. If the function fails, the return value is zero (0). To get extended error information, call {@link Kernel32#GetLastError() GetLastError}.
     */
    boolean SetEndOfFile(HANDLE hFile);
    
    /**
     * Moves the file pointer of the specified file.
     * @param hFile A handle to the file. The file handle must be created with the GENERIC_READ or GENERIC_WRITE access right. For more information, see <a href="https://docs.microsoft.com/en-us/windows/desktop/FileIO/file-security-and-access-rights">File Security and Access Rights</a>.
     * @param lDistanceToMove The low order 32-bits of a signed value that specifies the number of bytes to move the file pointer. If lpDistanceToMoveHigh is not NULL, lpDistanceToMoveHigh and lDistanceToMove form a single 64-bit signed value that specifies the distance to move. If lpDistanceToMoveHigh is NULL, lDistanceToMove is a 32-bit signed value. A positive value for lDistanceToMove moves the file pointer forward in the file, and a negative value moves the file pointer back.
     * @param lpDistanceToMoveHigh A pointer to the high order 32-bits of the signed 64-bit distance to move. If you do not need the high order 32-bits, this pointer must be set to NULL. When not NULL, this parameter also receives the high order DWORD of the new value of the file pointer. For more information, see the Remarks section in this topic.
     * @param dwMoveMethod The starting point for the file pointer move. This parameter can be one of FILE_BEGIN, FILE_CURRENT, FILE_END.
     * @return If the function succeeds and lpDistanceToMoveHigh is NULL, the return value is the low-order DWORD of the new file pointer. Note  If the function returns a value other than INVALID_SET_FILE_POINTER, the call to SetFilePointer has succeeded. You do not need to call {@link Kernel32@GetLastError() GetLastError}. If function succeeds and lpDistanceToMoveHigh is not NULL, the return value is the low-order DWORD of the new file pointer and lpDistanceToMoveHigh contains the high order DWORD of the new file pointer. If the function fails, the return value is INVALID_SET_FILE_POINTER. To get extended error information, call {@link Kernel32@GetLastError() GetLastError}. If the function fails, the return value is INVALID_SET_FILE_POINTER. To get extended error information, call {@link Kernel32@GetLastError() GetLastError}. If a new file pointer is a negative value, the function fails, the file pointer is not moved, and the code returned by GetLastError is ERROR_NEGATIVE_SEEK. If lpDistanceToMoveHigh is NULL and the new file position does not fit in a 32-bit value, the function fails and returns INVALID_SET_FILE_POINTER.
     */
    int SetFilePointer(HANDLE hFile, int lDistanceToMove, IntByReference lpDistanceToMoveHigh, int dwMoveMethod);

    /**
     * Maps a view of a file mapping into the address space of a calling
     * process.
     *
     * @param hFileMappingObject
     *            A handle to a file mapping object. The CreateFileMapping and
     *            OpenFileMapping functions return this handle.
     * @param dwDesiredAccess
     *            The type of access to a file mapping object, which determines
     *            the protection of the pages.
     * @param dwFileOffsetHigh
     *            A high-order DWORD of the file offset where the view begins.
     * @param dwFileOffsetLow
     *            A low-order DWORD of the file offset where the view is to
     *            begin.
     * @param dwNumberOfBytesToMap
     *            The number of bytes of a file mapping to map to the view.
     * @param lpBaseAddress A pointer to the memory address in the calling process address space where mapping begins. This must be a multiple of the system's memory allocation granularity, or the function fails. To determine the memory allocation granularity of the system, use the GetSystemInfo function. If there is not enough address space at the specified address, the function fails.
     *            If lpBaseAddress is NULL, the operating system chooses the mapping address. In this scenario, the function is equivalent to the MapViewOfFile function.
     *            While it is possible to specify an address that is safe now (not used by the operating system), there is no guarantee that the address will remain safe over time. Therefore, it is better to let the operating system choose the address. In this case, you would not store pointers in the memory mapped file, you would store offsets from the base of the file mapping so that the mapping can be used at any address.
     * @return If the function succeeds, the return value is the starting
     *         address of the mapped view. If the function fails, the return
     *         value is NULL. To get extended error information, call
     *         GetLastError.
     */
    Pointer MapViewOfFileEx(HANDLE hFileMappingObject, int dwDesiredAccess, int dwFileOffsetHigh, int dwFileOffsetLow, int dwNumberOfBytesToMap, Pointer lpBaseAddress);

    /**
     * Retrieves the node that currently has the highest number.
     * @param HighestNodeNumber The number of the highest node.
     * @return If the function succeeds, the return value is nonzero.
     */
    int GetNumaHighestNodeNumber(ULONGByReference HighestNodeNumber);
    
    /**
     * Retrieves the processor mask for a node regardless of the processor group the node belongs to.
     * @param Node The node number.
     * @param ProcessMask A pointer to a GROUP_AFFINITY structure that receives the processor mask for the specified node. A processor mask is a bit vector in which each bit represents a processor and whether it is in the node.
     * @return If the function succeeds, the return value is nonzero.
     */
    int GetNumaNodeProcessorMaskEx(USHORT Node, GROUP_AFFINITY ProcessMask);
    
    /**
     * Returns the number of active processors in a processor group or in the system.
     * @param GroupNumber The processor group number. If this parameter is ALL_PROCESSOR_GROUPS, the function returns the number of active processors in the system.
     * @return If the function succeeds, the return value is the number of active processors in the specified group.
     */
    DWORD GetActiveProcessorCount(WORD GroupNumber);
}
