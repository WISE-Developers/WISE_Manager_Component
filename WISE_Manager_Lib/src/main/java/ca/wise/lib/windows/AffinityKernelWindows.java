package ca.wise.lib.windows;

import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.Kernel32;

public interface AffinityKernelWindows extends Kernel32 {
	
	public static AffinityKernelWindows load() {
		return (AffinityKernelWindows)Native.load("Kernel32", AffinityKernelWindows.class);
	}
	
	public boolean SetProcessAffinityMask(final long pid, long dwProcessAffinityMask);
	
	public boolean GetProcessAffinityMask(final long pid, PointerType lpProcessAffinityMask, PointerType lpSystemAffinityMask);
}
