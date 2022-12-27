package ca.wise.lib;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import ca.wise.lib.status.IJobStatus;
import ca.wise.lib.status.ProtoJobStatus;
import ca.wise.lib.status.XmlJobStatus;

/**
 * Builder for the status file.
 * @author Travis Redpath
 */
public class JobStatus {

	private static Map<String, IJobStatus> _instances = new HashMap<>();
	
	public static IJobStatus getUpdater(String directory) {
		IJobStatus ret = null;
		synchronized (_instances) {
			String key = Paths.get(directory).getFileName().toString();
			ret = _instances.computeIfAbsent(key, k -> {
				if (Files.exists(Paths.get(directory, "job.fgmj")) || Files.exists(Paths.get(directory, "job.fgmb")))
					return new ProtoJobStatus(directory);
				else
					return new XmlJobStatus(directory);
			});
		}
		return ret;
	}
}
