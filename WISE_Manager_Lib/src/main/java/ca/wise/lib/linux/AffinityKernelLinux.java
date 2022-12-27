package ca.wise.lib.linux;

import java.util.BitSet;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.LongByReference;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public class AffinityKernelLinux {
	
	private LinuxLibrary _internal;
	
	private AffinityKernelLinux() {
		_internal = (LinuxLibrary)Native.load("c", LinuxLibrary.class);
	}
	
	public static AffinityKernelLinux load() {
		return new AffinityKernelLinux();
	}
	
	public boolean GetProcessAffinityMask(final long pid, LongByReference lpProcessAffinityMask, LongByReference lpSystemAffinityMask) {
		final cpu_set_t cpuset = new cpu_set_t();
		try {
			boolean retval = _internal.sched_getaffinity((int)(pid == -1 ? 0 : pid), cpu_set_t.SIZE_OF_CPU_SET_T, cpuset) == 0;
			if (retval) {
				BitSet set = new BitSet(cpu_set_t.__CPU_SETSIZE);
				int i = 0;
				for (NativeLong nl : cpuset.__bits) {
					for (int j = 0; j < Long.SIZE; j++)
						set.set(i++, ((nl.longValue() >>> j) & 1) != 0);
				}
				lpProcessAffinityMask.setValue(set.toLongArray()[0]);
				SystemInfo info = new SystemInfo();
				HardwareAbstractionLayer hal = info.getHardware();
				int coreCount = hal.getProcessor().getLogicalProcessorCount();
				long system = 0;
				for (i = 0; i < coreCount; i++) {
					system |= (1L << i);
				}
				lpSystemAffinityMask.setValue(system);
				
				return retval;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean SetProcessAffinityMask(final long pid, long dwProcessAffinityMask) {
		final cpu_set_t cpuset = new cpu_set_t();
		try {
			cpuset.__bits[0].setValue(dwProcessAffinityMask);
			return _internal.sched_setaffinity((int)(pid == -1 ? 0 : pid), cpu_set_t.SIZE_OF_CPU_SET_T, cpuset) == 0;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private interface LinuxLibrary extends Library {
		
		int sched_getaffinity(final int pid, final int cpusetsize, final cpu_set_t cpuset);
		
		int sched_setaffinity(final int pid, final int cpusetsize, final cpu_set_t cpuset);
	}
}
