package ca.wise.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.hss.platform.OperatingSystem;
import ca.hss.platform.OperatingSystem.Type;

public class Executor {

	/**
	 * Start a process that is detached from the Java instance.
	 * @param commands The command and its arguments.
	 */
	public static void Execute(String...commands) throws IOException {
		List<String> c = new ArrayList<>();
		if (OperatingSystem.getOperatingSystem().getType() == Type.Windows) {
			c.add("cmd");
			c.add("/c");
			c.add("start");
		}
		else {
			c.add("/bin/sh");
			c.add("-c");
		}
		c.addAll(Arrays.asList(commands));
		Runtime.getRuntime().exec(c.toArray(new String[0]));
	}
}
