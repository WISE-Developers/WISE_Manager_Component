package ca.wise.lib;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import ca.hss.ui.StreamReaderThread;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.status.IJobStatus;
import ca.wise.lib.status.Status;
import lombok.Getter;
import lombok.Setter;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class Job implements Closeable {
	
	private static final ThreadPoolExecutor threadPool;

	private ProcessWatcher process = null;
	private PerformanceTracker perf = null;
	@Getter private double usage = 0;
	
	@Getter private final Path xmlPath;
	private FileChannel xmlLockStream;
	private FileLock xmlLock;
	private String directory;
	
	@Getter protected String name;
	@Getter @Setter protected LocalDateTime start;
	@Getter @Setter protected LocalDateTime submitted;
	@Getter @Setter protected LocalDateTime end;
	@Getter protected int requestedCores;
	@Getter protected boolean hasRequestedCores = false;
	@Getter @Setter protected JobStage status;
	@Getter @Setter protected int priority;
	/**
	 * Which CPU cores the job is running on.
	 */
	@Getter @Setter CPUUsage cpuUsage = CPUUsage.ZERO;
	
	public void setRequestedCores(int cores) {
	    requestedCores = cores;
	    hasRequestedCores = true;
	}
	
	private List<IPropertyChangedListener> eventListeners = new ArrayList<>();
	private List<IJobCompleteListener> completeListeners = new ArrayList<>();
	
	static {
		SystemInfo si = new SystemInfo();
		CentralProcessor p = si.getHardware().getProcessor();
		threadPool = new ThreadPoolExecutor(p.getLogicalProcessorCount() * 3, 1024, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	/**
	 * Create a new W.I.S.E. job.
	 * @param jobDirectory The directory that all jobs are stored in.
	 * @param jobName The name of the job (will be used as the folder name within {@code jobDirectory}).
	 */
	public Job(String jobDirectory, String jobName) {
		this(jobDirectory, jobName, JobStage.Queued);
	}
	
	/**
	 * Create a new W.I.S.E. job.
	 * @param jobDirectory The directory that all jobs are stored in.
	 * @param jobName The name of the job (will be used as the folder name within {@code jobDirectory}).
	 * @param finished Has the job already finished executing.
	 * @param stopped Was the job manually stopped.
	 */
	public Job(String jobDirectory, String jobName, JobStage stage) {
		Path temp = Paths.get(jobDirectory, jobName, "job.xml");
		if (!Files.exists(temp)) {
			temp = Paths.get(jobDirectory, jobName, "job.fgmj");
			if (!Files.exists(temp)) {
				temp = Paths.get(jobDirectory, jobName, "job.fgmb");
			}
		}
		xmlPath = temp;
		directory = jobDirectory;
		name = jobName;
		status = stage;
		if (stage == JobStage.Queued) {
			IJobStatus st = JobStatus.getUpdater(Paths.get(jobDirectory, jobName).toString());
			st.updateStatus(Status.Submitted);
			lockXml();
		}
		loadConfig();
	}
	
	/**
	 * Save the configuration of the job to a file in the job directory.
	 */
	public void saveConfig() {
	    Path configPath = Paths.get(directory, name, ".config");
	    //only save the configuration once
	    if (!Files.exists(configPath)) {
	        Properties props = new Properties();
	        props.setProperty("name", name);
	        props.setProperty("priority", String.valueOf(priority));
	        props.setProperty("submitted", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(submitted));
	        props.setProperty("cores", String.valueOf(requestedCores));
	        
	        try (OutputStream output = new FileOutputStream(configPath.toFile())) {
	            props.store(output, null);
	        }
	        catch (IOException e) { }
	    }
	}
	
	/**
	 * Try to parse an integer from a string, returning a default value if not possible.
	 * @param value The string value to try to parse.
	 * @param def The default value to return if the string couldn't be parsed.
	 * @return The parsed integer value if possible, otherwise the default.
	 */
	private static int tryParseInteger(String value, int def) {
	    try {
	        return Integer.parseInt(value);
	    }
	    catch (NumberFormatException e) {
	        return def;
	    }
	}
	
	/**
	 * Load the configuration file, if one exists.
	 */
	private void loadConfig() {
        Path configPath = Paths.get(directory, name, ".config");
        //if the config file exists
        if (Files.exists(configPath)) {
            Properties props = new Properties();
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                props.load(input);
            }
            catch (IOException e) { }
            
            if (props.containsKey("name")) {
                String name = props.getProperty("name");
                //make sure the config file is for this job
                if (this.name.equals(name)) {
                    if (props.containsKey("priority")) {
                        priority = tryParseInteger(props.getProperty("priority"), priority);
                    }
                    if (props.containsKey("cores")) {
                        requestedCores = tryParseInteger(props.getProperty("cores"), requestedCores);
                        hasRequestedCores = true;
                    }
                    if (props.containsKey("submitted")) {
                        try {
                            submitted = LocalDateTime.parse(props.getProperty("submitted"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        catch (DateTimeParseException e) { }
                    }
                }
            }
        }
	}
	
	/**
	 * Get the location of the jobs specification file (either xml or fgmj).
	 */
	public Path getSpecificationFile() {
		return xmlPath;
	}
	
	public void addPropertyChangedEventListener(IPropertyChangedListener listener) {
		eventListeners.add(listener);
	}
	
	public void removePropertyChangedEventListener(IPropertyChangedListener listener) {
		eventListeners.remove(listener);
	}
	
	public void addJobCompleteListener(IJobCompleteListener listener) {
		completeListeners.add(listener);
	}
	
	public void removeJobCompleteListener(IJobCompleteListener listener) {
		completeListeners.remove(listener);
	}
	
	public void clearJobCompleteListeners() {
	    completeListeners.clear();
	}
	
	private void notifyPropertyChanged(String name, Object value) {
		eventListeners.forEach(l -> l.onPropertyChanged(this, name, value));
	}
	
	/**
	 * Mark the job to be restarted with W.I.S.E. Manager is restarted.
	 */
	public void markForRestart() {
	    Path dir = Paths.get(directory, name);
	    try {
            Files.createDirectories(dir);
            Files.createFile(dir.resolve("restart.act"));
        }
	    catch (IOException e) { }
	}
	
	protected void jobDone(Process p) {
		try {
			IJobStatus status = JobStatus.getUpdater(Paths.get(directory, name).toString());
			status.forceReload();
			IJobStatus.JobCompletionStatus current = status.getJobStatus();
			
			if (current == IJobStatus.JobCompletionStatus.NONE) {
				status.updateStatus(Status.Failed);
				this.status = JobStage.Error;
			}
			else if (current == IJobStatus.JobCompletionStatus.FAILED)
				this.status = JobStage.Error;
			else
				this.status = JobStage.Finished;
			end = LocalDateTime.now();
			usage = 0.0;
			process = null;
			notifyPropertyChanged("end", end);
			notifyPropertyChanged("status", status);
			notifyPropertyChanged("usage", usage);
			completeListeners.forEach(l -> l.onJobComplete(this));
		}
		catch (Exception e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Unable to read completed job data.", e);
			try {
				Files.write(Paths.get(directory, "stderr.txt"), ("Error ending process\r\n\t" + e.getMessage()).getBytes(), StandardOpenOption.APPEND);
			}
			catch (IOException e1) { }
		}
	}
	
	/**
	 * A validation job has completed running. Check to see if the job was valid or not
	 * and tell the listeners that the validation has completed.
	 * @param p The running process.
	 */
	protected void validationJobDone(Process p) {
	    try {
            if (Files.exists(Paths.get(directory, name, "validation.json")))
                this.status = JobStage.ValidationComplete;
            else
                this.status = JobStage.ValidationFailed;

            end = LocalDateTime.now();
            usage = 0.0;
            process = null;
            notifyPropertyChanged("end", end);
            notifyPropertyChanged("status", status);
            notifyPropertyChanged("usage", usage);
            completeListeners.forEach(l -> l.onJobComplete(this));
            completeListeners.clear();
	    }
	    catch (Exception e) {
            WISELogger.getSpecial(LogName.Backend).fatal("Unable to read completed validation data.", e);
            try {
                Files.write(Paths.get(directory, "stderr.txt"), ("Error ending process\r\n\t" + e.getMessage()).getBytes(), StandardOpenOption.APPEND);
            }
            catch (IOException e1) { }
	    }
	}
	
	public void stop() {
		JobStatus.getUpdater(Paths.get(directory, name).toString()).updateStatus(Status.Information, "Stop Requested");
		releaseXmlLockNoThrow();
		status = JobStage.Stopped;
	}
	
	/**
	 * Attempt to update the process CPU usage.
	 */
	public void updateUsage() {
		if (perf != null) {
			OSProcess process = perf.os.getProcess(perf.process);
			if (process != null) {
				long time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L;
				long running = process.getKernelTime() + process.getUserTime();
				usage = 100d * (running - perf.running) / (((double)(time - perf.time)) * perf.cpuCount);
				if (usage < 0)
					usage = 0;
				perf.time = time;
				perf.running = running;
				notifyPropertyChanged("usage", usage);
			}
		}
	}
	
	/**
	 * Start the job running in W.I.S.E..
	 */
	public void startJob() {
		try {
			releaseXmlLock();
			status = JobStage.Running;
			notifyPropertyChanged("status", status);
			IJobStatus st = JobStatus.getUpdater(Paths.get(directory, name).toString());
			st.updateStatus(Status.Started);
			Process p;
			List<String> parameters;
			//the verison of W.I.S.E. supports the -m parameters
			if (!Strings.isNullOrEmpty(WISESupport.getMqttId()) && WISESupport.isSupportsManagerId()) {
			    parameters = new ArrayList<>();
			    parameters.add(Settings.getWiseExe());
                parameters.add(xmlPath.toAbsolutePath().toString());
                parameters.add("-m");
                parameters.add(WISESupport.getMqttId());
				//we define which cores to run on
				if (cpuUsage.getNumaNode() >= 0 && cpuUsage.getNumaNode() < CPUInfo.staticNumaCount()) {
				    int coresToUse = cpuUsage.getRunningCores().bitCount();
				    //this job has been set to run on no cores or the user has requested fewer cores than the max allowed
				    if (requestedCores > 0 && (coresToUse == 0 || requestedCores < coresToUse))
				        coresToUse = requestedCores;
			        //they have asked for too many cores or they have asked us to decide how many cores to use
			        if ((cpuUsage.getMaxCores() > 0 && coresToUse >= cpuUsage.getMaxCores()) || coresToUse == 0)
			            coresToUse = cpuUsage.getMaxCores();
                    int offset = cpuUsage.getRunningCores().getLowestSetBit();
                    //this job has been set to run on no cores
                    if (offset < 0) {
    			        for (int i = 0; i < cpuUsage.getNumaNode(); i++) {
    			            offset += CPUInfo.staticCoreCount(i);
    			        }
                    }
			        parameters.add("-r");
			        parameters.add(String.valueOf(coresToUse));
			        if (offset > 0) {
			            parameters.add("-f");
			            parameters.add(String.valueOf(offset));
			        }
				}
				//print runtime stats when W.I.S.E. closes
				parameters.add("-t");
			}
			else
				parameters = Arrays.asList(Settings.getWiseExe(), xmlPath.toAbsolutePath().toString());
			p = new ProcessBuilder(parameters)
					.directory(Paths.get(Settings.getWiseExe()).getParent().toFile())
					.start();
			process = new ProcessWatcher(this::jobDone, p);
			threadPool.execute(process);
			//send stdout and stderr to /dev/nul so that output doesn't block the application
			threadPool.execute(new StreamReaderThread(p.getInputStream()));
			threadPool.execute(new StreamReaderThread(p.getErrorStream()));
			start = LocalDateTime.now();
			notifyPropertyChanged("start", start);
			SystemInfo system = new SystemInfo();
			OperatingSystem os = system.getOperatingSystem();
			CentralProcessor processor = system.getHardware().getProcessor();
			int cpuCount = processor.getLogicalProcessorCount();
			Long pid = ca.hss.platform.OperatingSystem.getOperatingSystem().getPid(p);
			if (pid != null) {
				if (pid < 0) {
					WinNT.HANDLE handle = new WinNT.HANDLE();
					handle.setPointer(Pointer.createConstant(-pid));
					pid = (long)Kernel32.INSTANCE.GetProcessId(handle);
				}
				
				if (pid > 0) {
					perf = new PerformanceTracker();
					perf.cpuCount = cpuCount;
					perf.os = os;
					perf.time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L;
					perf.running = 0;
					perf.process = pid.intValue();
					
					if (Settings.getLockCPU())
						AffinityKernel.setProcessorAffinity(pid.longValue())
							.withoutProcessor(0)
							.save();
				}
			}
		}
		catch (Exception e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Error starting job.", e);
		}
	}
	
	/**
	 * Validate the job in W.I.S.E..
	 */
	public boolean validate() {
        try {
            if (WISESupport.isSupportsValidation()) {
                status = JobStage.Validating;
                List<String> parameters = new ArrayList<>();
                parameters.add(Settings.getWiseExe());
                parameters.add(xmlPath.toAbsolutePath().toString());
                parameters.add("--validate");
                if (!Strings.isNullOrEmpty(WISESupport.getMqttId()) && WISESupport.isSupportsManagerId()) {
                    parameters.add("-m");
                    parameters.add(WISESupport.getMqttId());
                }
                Process p = new ProcessBuilder(parameters)
                        .directory(Paths.get(Settings.getWiseExe()).getParent().toFile())
                        .start();
                process = new ProcessWatcher(this::validationJobDone, p);
                threadPool.execute(process);
                //send stdout and stderr to /dev/nul so that output doesn't block the application
                threadPool.execute(new StreamReaderThread(p.getInputStream()));
                threadPool.execute(new StreamReaderThread(p.getErrorStream()));
                start = LocalDateTime.now();
                notifyPropertyChanged("start", start);
                return true;
            }
        }
        catch (Exception e) {
            WISELogger.getSpecial(LogName.Backend).fatal("Error validating job.", e);
        }
        status = JobStage.ValidationFailed;
        return false;
	}
	
	/**
	 * If the job is running, terminate its process immediately.
	 */
	public void terminateJob() {
		if (process != null && process.toExecute != null) {
			process.toExecute.destroyForcibly();
		}
	}
	
	/**
	 * Lock a file to stop other applications from modifying it.
	 * Only works on Windows.
	 */
	private void lockXml() {
		if (GlobalConfiguration.lockXmlFiles && xmlLockStream == null && xmlLock == null) {
			try {
				xmlLockStream = FileChannel.open(xmlPath, StandardOpenOption.WRITE, StandardOpenOption.READ);
				xmlLock = xmlLockStream.lock();
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Unable to lock XML file.", e);
			}
		}
	}
	
	private void releaseXmlLockNoThrow() {
		try {
			releaseXmlLock();
		}
		catch (IOException e) { }
	}
	
	private void releaseXmlLock() throws IOException {
		if (xmlLock != null) {
			xmlLock.release();
			xmlLock = null;
		}
		if (xmlLockStream != null) {
			xmlLockStream.close();
			xmlLockStream = null;
		}
	}
	
	public void lock() throws IOException {
		lockXml();
	}
	
	/**
	 * Unlock the job file, if it's locked.
	 * @return True if the file was locked, false if it wasn't.
	 */
	public boolean conditionalClose() throws IOException {
		if (xmlLock != null && xmlLockStream != null) {
			releaseXmlLock();
			return true;
		}
		return false;
	}

	@Override
	public void close() throws IOException {
		releaseXmlLock();
	}
	
	public static enum JobStage {
		Queued,
		Running,
		Error,
		Finished,
		Stopped,
		Unknown,
		Validating,
		ValidationComplete,
		ValidationFailed;
	    
	    @Override
	    public String toString() {
	        switch (this) {
	        case Queued:
	            return "Queued";
	        case Running:
	            return "Running";
	        case Error:
	            return "Error";
	        case Finished:
	            return "Finished";
	        case Stopped:
	            return "Stopped";
	        case Validating:
	            return "Validating";
	        case ValidationComplete:
	            return "Validation Complete";
	        case ValidationFailed:
	            return "Validation Failed";
            case Unknown:
            default:
                return "Unknown";
	        }
	    }
	}
	
	@FunctionalInterface
	public static interface IPropertyChangedListener {
		
		void onPropertyChanged(Job job, String name, Object value);
	}
	
	@FunctionalInterface
	public static interface IJobCompleteListener {
		
		void onJobComplete(Job job);
	}
	
	private static class PerformanceTracker {
		
		public int cpuCount;
		public int process;
		public OperatingSystem os;
		public long time;
		public long running;
	}
}
