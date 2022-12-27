package ca.wise.lib;

import com.sun.jna.ptr.LongByReference;

import ca.hss.platform.OperatingSystem;
import ca.wise.lib.linux.AffinityKernelLinux;
import ca.wise.lib.windows.AffinityKernelWindows;

public final class AffinityKernel {

	private static AffinityKernelWindows _windowsInstance = null;
	private static AffinityKernelLinux _linuxInstance = null;
	
	private long mask;
	private long pid;
	
	private AffinityKernel() {
		mask = 0;
	}
	
	static {
		try {
			if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows)
				_windowsInstance = AffinityKernelWindows.load();
			else if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Linux)
				_linuxInstance = AffinityKernelLinux.load();
			else
				throw new Exception();
		}
		catch (Exception e) {
			WISELogger.getSpecial(WISELogger.LogName.Backend).warn("No available CPU affinity implementation for this platform.");
		}
	}
	
	public static AffinityKernel setProcessorAffinity(final long pid) {
		AffinityKernel kernel = new AffinityKernel();
		kernel.pid = pid;
		return kernel;
	}
	
	public AffinityKernel withProcessor(int index) {
		mask |= (1L << index);
		return this;
	}
	
	public AffinityKernel withProcessors(int...index) {
		for (int i : index) {
			mask |= (1L << i);
		}
		return this;
	}
	
	public AffinityKernel withoutProcessor(int index) {
		mask = ~(1L << index);
		if (mask < 0)
			mask = -mask;
		return this;
	}
	
	public void save() {
		if (mask > 0 || mask == -1) {
			LongByReference process = new LongByReference();
			LongByReference system = new LongByReference();
			if (_windowsInstance != null) {
				try {
					if (_windowsInstance.GetProcessAffinityMask(pid, process, system)) {
						mask = mask & system.getValue();
						_windowsInstance.SetProcessAffinityMask(pid, mask);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (_linuxInstance != null) {
				try {
					if (_linuxInstance.GetProcessAffinityMask(pid, process, system)) {
						mask = mask & system.getValue();
						_linuxInstance.SetProcessAffinityMask(pid, mask);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
