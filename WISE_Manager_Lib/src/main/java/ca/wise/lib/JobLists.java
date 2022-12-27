package ca.wise.lib;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.utils.IOUtils;

import com.google.common.base.Strings;
import com.google.protobuf.util.JsonFormat;

import ca.wise.status.proto.jobStatus;
import ca.wise.fgm.FGMHelper;
import ca.wise.lib.Job.JobStage;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.json.JobHistory;
import ca.wise.lib.json.Shutdown;
import ca.wise.lib.mqtt.MqttListener;
import lombok.Getter;

public class JobLists implements Closeable {
	
	private Path jobDirectory;
	private Lock listLock = new ReentrantLock();
	@Getter private List<Job> finishedJobs = new ArrayList<>();
	private List<Job> jobQueue = new ArrayList<>();
	private List<Job> validateQueue = new ArrayList<>();
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
	private Lock queueLock = new ReentrantLock();
	
	private List<IListChangedListener> finishedListeners = new ArrayList<>();
	private List<IListChangedListener> queueListeners = new ArrayList<>();
	private List<IListChangedListener> validateListeners = new ArrayList<>();
	
	/**
	 * Create a new job list.
	 * @param directory The directory that W.I.S.E. jobs get written to.
	 */
	public JobLists(Path directory) {
		jobDirectory = directory;
	}
	
	public void addFinishedListChangeListener(IListChangedListener listener) {
		finishedListeners.add(listener);
	}
	
	public void removeFinishedListChangeListener(IListChangedListener listener) {
		finishedListeners.remove(listener);
	}
	
	public void addQueueChangeListener(IListChangedListener listener) {
		queueListeners.add(listener);
	}
	
	public void removeQueueChangeListener(IListChangedListener listener) {
		queueListeners.remove(listener);
	}
    
    public void addValidateChangeListener(IListChangedListener listener) {
        validateListeners.add(listener);
    }
    
    public void removeValidateChangeListener(IListChangedListener listener) {
        validateListeners.remove(listener);
    }
	
	/**
	 * Non-thread safe get job list method.
	 * @return The list of queued jobs.
	 */
	public List<Job> getJobQueue() {
		return Collections.unmodifiableList(jobQueue);
	}
    
    /**
     * Non-thread safe get validation list method.
     * @return The list of queued validation jobs.
     */
    public List<Job> getValidationQueue() {
        return Collections.unmodifiableList(validateQueue);
    }
	
	/**
	 * Non-thread safe get validate list method.
	 * @return The list of jobs that need validated.
	 */
	public List<Job> getValidateQueue() {
	    return Collections.unmodifiableList(validateQueue);
	}
	
	/**
	 * Move a job up one position in the job queue.
	 * @param index The index of the job to move.
	 * @return True if a job was moved, false otherwise.
	 */
	public boolean moveJobUp(int index) {
		queueLock.lock();
		boolean retval = false;
		try {
			if (index > 0 && index < jobQueue.size()) {
				Collections.swap(jobQueue, index, index - 1);
				retval = true;
			}
		}
		finally {
			queueLock.unlock();
		}
		return retval;
	}
	
	/**
	 * Move a job up one position in the job queue.
	 * @param index The index of the job to move.
	 * @return True if a job was moved, false otherwise.
	 */
	public boolean moveJobDown(int index) {
		queueLock.lock();
		boolean retval = false;
		try {
			if (index >= 0 && index < (jobQueue.size() - 1)) {
				Collections.swap(jobQueue, index, index + 1);
				retval = true;
			}
		}
		finally {
			queueLock.unlock();
		}
		return retval;
	}
	
	/**
	 * Lock the job queue so it will not be modified while it's members are being accessed.
	 * @return The list of queued jobs.
	 */
	public List<Job> lockJobQueue() {
		queueLock.lock();
		return getJobQueue();
	}
	
	/**
	 * Unlock the job queue so that it may be modified again.
	 */
	public void unlockJobQueue() {
		queueLock.unlock();
	}
	
	/**
	 * Get the number of jobs that are currently running.
	 * @return The number of running jobs.
	 */
	public long getRunningCount() {
		try {
			return lockJobQueue().stream().filter(j -> j.status == JobStage.Running).count();
		}
		finally {
			unlockJobQueue();
		}
	}
	
	/**
	 * Get the number of jobs that are queued to run.
	 * @return The number of jobs that have not yet been run.
	 */
	public long getQueuedCount() {
		try {
			return lockJobQueue().stream().filter(j -> j.status == JobStage.Queued).count();
		}
		finally {
			unlockJobQueue();
		}
	}
	
	/**
	 * Mark jobs that are in the job queue (not yet running) so that they
	 * will be restarted after W.I.S.E. Manager is rebooted.
	 */
	public void markQueueForRestart() {
	    try {
	        lockJobQueue().stream()
	            .filter(j -> j.status == JobStage.Queued)
	            .forEach(Job::markForRestart);
	    }
	    finally {
	        unlockJobQueue();
	    }
	}
	
	/**
	 * Does the specified job already exist in the job list.
	 * @param jobName The name of the job to check.
	 * @return 1 if the job is queued or running, -1 if the job is finished, and 0 if the job is unknown.
	 */
	public int containsJob(String jobName) {
        queueLock.lock();
	    try {
	        if (jobQueue.stream().anyMatch(x -> x.name.equals(jobName)))
	            return 1;
	    }
	    finally {
	        queueLock.unlock();
	    }
	    
	    listLock.lock();
	    try {
	        return finishedJobs.stream().anyMatch(x -> x.name.equals(jobName)) ? -1 : 0;
	    }
	    finally {
	        listLock.unlock();
	    }
	}
	
	/**
	 * Get the number of slots that are available.
	 * @return The number of additional jobs that can be run.
	 */
	public long getFreeCount() {
		return Settings.getProcesses() - getRunningCount();
	}
	
	/**
	 * Run something that interacts with the job lists while blocking
	 * changes to the lists. The task should only run for as long as
	 * necessary, don't run long running tasks.
	 * @param runner The task to run.
	 */
	public void runOnLists(Runnable runner) {
		listLock.lock();
		try {
			runner.run();
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * A job has completed, move it from the queue to the finished jobs list.
	 * @param job The job that has finished.
	 */
	public void completeJob(Job job) {
		listLock.lock();
		try {
			int index = jobQueue.indexOf(job);
			if (index >= 0) {
				queueLock.lock();
				try {
					jobQueue.remove(index);
				}
				finally {
					queueLock.unlock();
				}
				queuedJobRemoved(job, index);
				finishedJobs.add(0, job);
				finishedJobAdded(job, 0);
			}
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * A validation job has completed, move it from the validation queue to the finished job list.
	 * @param job The validation job that has finished.
	 */
	public void completeValidationJob(Job job) {
	    listLock.lock();
	    try {
	        int index = validateQueue.indexOf(job);
	        if (index >= 0) {
	            validateQueue.remove(index);
	            validationJobRemoved(job, index);
	            finishedJobs.add(0, job);
	            finishedJobAdded(job, 0);
	        }
	    }
	    finally {
	        listLock.unlock();
	    }
	}
	
	/**
	 * Terminate a running job or stop a queued job from running.
	 * @param shutdown Information about the job to terminate.
	 * @param flagRestart Should the job be flagged to be restarted when W.I.S.E. Manager is restarted.
	 */
	@SuppressWarnings("resource")
    public void terminateJob(Shutdown shutdown, MqttListener mqtt, boolean flagRestart) {
		Job completedJob = null;
		
		listLock.lock();
		try {
			Optional<Job> job = jobQueue.stream().filter(j -> j.name.equals(shutdown.jobId)).findFirst();
			if (job.isPresent()) {
				if (job.get().status == JobStage.Queued) {
					job.get().stop();
					completedJob = job.get();
				}
				else if (shutdown.priority == 1) {
					if (mqtt != null)
						mqtt.sendJobShutdown(shutdown);
					int timeout = shutdown.timeout == null || shutdown.timeout <= 0 ? 5 : shutdown.timeout;
					executor.schedule(new JobKiller(shutdown), timeout, TimeUnit.MINUTES);
				}
				else if (shutdown.priority > 1) {
					job.get().terminateJob();
					//create the restart file if the job should be restarted
					if (flagRestart)
					    job.get().markForRestart();
				}
			}
		}
		finally {
			listLock.unlock();
		}
		
		if (completedJob != null)
			completeJob(completedJob);
	}
	
	/**
	 * A new job has been submitted.
	 * @param job The new job.
	 */
	public void newJob(Job job) {
        job.saveConfig();
		listLock.lock();
		try {
			queueLock.lock();
			int index = -1;
			try {
			    //find the location that the job should be placed in the queue
			    for (int i = jobQueue.size() - 1; i >= 0; i--) {
			        //if the job at this index is running or its priority is higher than or equal
			        //to the new job add the new job after it
			        if (jobQueue.get(i).getStatus() != JobStage.Queued ||
			                jobQueue.get(i).getPriority() >= job.getPriority()) {
			            index = i + 1;
			            jobQueue.add(index, job);
			            break;
			        }
			    }
			    //this job has a higher priority than all the other jobs in the queue
			    if (index < 0) {
			        jobQueue.add(0, job);
			        index = 0;
			    }
			}
			finally {
				queueLock.unlock();
			}
			queuedJobAdded(job, index);
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * A new job that is already completed.
	 * @param job The new job that is completed.
	 */
	public void newFinishedJob(Job job) {
        job.saveConfig();
        if (containsJob(job.name) > 0)
            completeJob(job);
        else {
            listLock.lock();
            try {
                finishedJobs.add(0, job);
                finishedJobAdded(job, 0);
            }
            finally {
                listLock.unlock();
            }
        }
	}
    
    /**
     * A new job that needs to be validated.
     * @param job The new validation job.
     */
    public void newValidationJob(Job job) {
        job.saveConfig();
        if (containsJob(job.name) == 0) {
            listLock.lock();
            try {
                int index = -1;

                //find the location that the job should be placed in the queue
                for (int i = validateQueue.size() - 1; i >= 0; i--) {
                    //if the job at this index is running or its priority is higher than or equal
                    //to the new job add the new job after it
                    if (validateQueue.get(i).getStatus() != JobStage.Queued ||
                            validateQueue.get(i).getPriority() >= job.getPriority()) {
                        index = i + 1;
                        validateQueue.add(index, job);
                        break;
                    }
                }
                //this job has a higher priority than all the other jobs in the queue
                if (index < 0) {
                    validateQueue.add(0, job);
                    index = 0;
                }
                validationJobAdded(job, index);
            }
            finally {
                listLock.unlock();
            }
        }
    }
    
	/**
	 * Get a list of job names for completed jobs.
	 * @return A list of job names.
	 */
	public List<String> getCompleteJobList() {
		listLock.lock();
		try {
			return finishedJobs.stream().map(x -> x.name).sorted().collect(Collectors.toList());
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * Get a list of job details for completed jobs.
	 */
	public List<JobHistory> getCompleteJobDetailsList() {
	    listLock.lock();
	    try {
	        return finishedJobs.stream().map(x -> JobHistory.builder()
	                .status(JobHistory.HistoryStatus.Complete)
	                .name(x.getName())
                    .submitTime(x.getSubmitted() == null ? null : x.getSubmitted().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .startTime(x.getStart() == null ? null : x.getStart().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .completeTime(x.getEnd() == null ? null : x.getEnd().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
	                .build())
                .collect(Collectors.toList());
	    }
	    finally {
	        listLock.unlock();
	    }
	}

	/**
	 * Get the job names at a collection of indices.
	 * @param indices The indices of the jobs to get the names for.
	 * @return The job names at the given indices.
	 */
	public List<String> getFinishedJobs(int[] indices) {
		listLock.lock();
		try {
			return IntStream.range(0, finishedJobs.size()).filter(i -> Arrays.stream(indices).anyMatch(j -> i == j)).mapToObj(i -> finishedJobs.get(i).getName()).collect(Collectors.toList());
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * Get a list of job names for queued (not yet running) jobs.
	 * @return A list of job names.
	 */
	public List<String> getQueuedJobs() {
		listLock.lock();
		try {
			return jobQueue.stream().filter(x -> x.status == JobStage.Queued).map(x -> x.name).sorted().collect(Collectors.toList());
		}
		finally {
			listLock.unlock();
		}
	}
    
	/**
	 * Get a list of job details for queued (not yet running) jobs.
	 */
    public List<JobHistory> getQueuedJobDetailsList() {
        listLock.lock();
        try {
            return jobQueue.stream()
                    .filter(x -> x.status == JobStage.Queued)
                    .map(x -> JobHistory.builder()
                        .status(JobHistory.HistoryStatus.Queued)
                        .name(x.getName())
                        .submitTime(x.getSubmitted() == null ? null : x.getSubmitted().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .startTime(x.getStart() == null ? null : x.getStart().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .completeTime(x.getEnd() == null ? null : x.getEnd().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .build())
                    .collect(Collectors.toList());
        }
        finally {
            listLock.unlock();
        }
    }
	
	/**
	 * Get a list of job names for running jobs.
	 * @return A list of job names.
	 */
	public List<String> getRunningJobs() {
		listLock.lock();
		try {
			return jobQueue.stream().filter(x -> x.status == JobStage.Running).map(x -> x.name).sorted().collect(Collectors.toList());
		}
		finally {
			listLock.unlock();
		}
	}
    
    /**
     * Get a list of job details for running jobs.
     */
    public List<JobHistory> getRunningJobDetailsList() {
        listLock.lock();
        try {
            return jobQueue.stream()
                    .filter(x -> x.status == JobStage.Running)
                    .map(x -> JobHistory.builder()
                        .status(JobHistory.HistoryStatus.Running)
                        .name(x.getName())
                        .submitTime(x.getSubmitted() == null ? null : x.getSubmitted().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .startTime(x.getStart() == null ? null : x.getStart().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .completeTime(x.getEnd() == null ? null : x.getEnd().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .build())
                    .collect(Collectors.toList());
        }
        finally {
            listLock.unlock();
        }
    }
	
	/**
	 * Get all jobs in the queue.
	 * @return A list of job names.
	 */
	public List<String> getUnfinishedJobs() {
		listLock.lock();
		try {
			return jobQueue.stream().map(x -> x.name).sorted().collect(Collectors.toList());
		}
		finally {
			listLock.unlock();
		}
	}
	
	/**
	 * Get a list of jobs that have been archived. They may or may not also exist in the finished job list.
	 * @return A list of job names.
	 */
	public List<String> getArchivedJobs() {
		try {
			return Files.list(Paths.get(Settings.getJobDirectory(), "Archives"))
					.map(p -> p.getFileName().toString())
					.filter(f -> f.endsWith("zip") || f.endsWith("tar") || f.endsWith("tar.gz"))
					.sorted()
					.collect(Collectors.toList());
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Unable to list archived jobs", e);
			return new ArrayList<String>();
		}
	}
	
	/**
	 * Delete a folder and all files within it.
	 * @param path The path of the folder to delete.
	 * @throws IOException If a file system permission error occurred.
	 */
	private void deleteRecursive(Path path) throws IOException {
		Files.walk(path)
		    .sorted(Comparator.reverseOrder())
		    .map(Path::toFile)
		    .forEach(File::delete);
	}
	
	/**
	 * Completely delete a finished job.
	 * @param jobName The name of the job to delete.
	 * @return True if the job was deleted, false otherwise. False may indicate an error or that the job simply didn't exist.
	 */
	public boolean deleteFinishedJob(String jobName) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			if (job.isPresent()) {
				Path path = job.get().getXmlPath().getParent();
				if (Files.exists(path) && Files.isDirectory(path)) {
					try {
						job.get().close();
						deleteRecursive(path);
						success = true;
					}
					catch (IOException e) {
						WISELogger.getSpecial(LogName.Backend).error("Unable to delete job folder", e);
						success = false;
					}
				}

				if (success) {
					int index = finishedJobs.indexOf(job.get());
					if (index >= 0) {
						finishedJobs.remove(index);
						finishedJobRemove(job.get(), index);
					}
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	/**
	 * Delete the output files from a previously run job and move it back onto
	 * the job queue so that it will be run again.
	 * @param jobName The name of the job to rerun.
	 * @return True if the job has been moved back to the job queue, false if
	 * there was an issue rerunning the job.
	 */
	public boolean rerunFinishedJob(String jobName, boolean deleteOld) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			if (job.isPresent()) {
				Path path = job.get().getXmlPath().getParent();
				if (Files.exists(path) && Files.isDirectory(path)) {
					try {
						job.get().close();
						if (deleteOld) {
    						Path outputs = path.resolve("Outputs");
    						//delete any previously generated outputs
    						if (Files.exists(outputs)) {
    							if (Files.isRegularFile(outputs)) {
    								Files.delete(outputs);
    							}
    							else {
    								deleteRecursive(outputs);
    							}
    						}
						}
						Path status = path.resolve("status.json");
						if (Files.exists(status))
							Files.delete(status);
						else {
							status = path.resolve("status.xml");
							if (Files.exists(status))
								Files.delete(status);
						}
						success = true;
					}
					catch (IOException e) {
						WISELogger.getSpecial(LogName.Backend).error("Unable to delete job folder", e);
						success = false;
					}
				}

				if (success) {
					int index = finishedJobs.indexOf(job.get());
					if (index >= 0) {
						finishedJobs.remove(index);
						finishedJobRemove(job.get(), index);
						Job newJob = new Job(jobDirectory.toString(), job.get().getName());
						newJob.setRequestedCores(job.get().getRequestedCores());
						newJob.setSubmitted(job.get().getSubmitted());
                        newJob.setPriority(job.get().getPriority());
						newJob(newJob);
					}
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	/**
	 * Create a zip archive of a finished job.
	 * @param jobName The name of the job to archive.
	 * @return True if the job was successfully archived.
	 */
	public boolean zipFinishedJob(String jobName) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			if (job.isPresent()) {
				try {
					boolean wasLocked = job.get().conditionalClose();
					Path toArchive = job.get().getXmlPath().getParent();
					Path path = Paths.get(Settings.getJobDirectory(), "Archives");
					if (!Files.exists(path))
						Files.createDirectories(path);
					path = path.resolve(jobName + ".zip");
					
					try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
						try (ZipOutputStream zos = new ZipOutputStream(fos)) {
							zipFile(toArchive, jobName, zos);
						}
					}
					if (wasLocked)
						job.get().lock();
					success = true;
				}
				catch (IOException e) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to create zip archive.", e);
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	private void zipFile(Path fileToZip, String filename, final ZipOutputStream stream) throws IOException {
		//skip hidden files and directories
		if (Files.isHidden(fileToZip))
			return;
		//recursively zip directories
		if (Files.isDirectory(fileToZip)) {
			List<Path> children = Files.list(fileToZip).collect(Collectors.toList());
			for (Path child : children) {
				zipFile(child, filename + "/" + child.getFileName().toString(), stream);
			}
		}
		else {
			try (FileInputStream fis = new FileInputStream(fileToZip.toFile())) {
				ZipEntry zipEntry = new ZipEntry(filename);
				stream.putNextEntry(zipEntry);
				byte[] bytes = new byte[1024];
				int length;
				while ((length = fis.read(bytes)) >= 0) {
					stream.write(bytes, 0, length);
				}
			}
		}
	}
	
	/**
	 * Unzip an archived job. If the job directory already exists the archive will not be extracted.
	 * @param jobName The name of the job to extract.
	 * @return True if the archive was extracted or the job already exists.
	 */
	public boolean unzipArchive(String jobName) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			//skip existing jobs
			if (job.isPresent())
				success = true;
			else {
				Path outputPath = Paths.get(Settings.getJobDirectory(), jobName);
				//assume the existing directory is already what the user wants
				if (Files.exists(outputPath))
					success = true;
				else {
					Path archive = Paths.get(Settings.getJobDirectory(), "Archives", jobName + ".zip");
					//make sure the archive exists
					if (Files.exists(archive)) {
						try {
							try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive.toFile()))) {
								ZipEntry zipEntry = zis.getNextEntry();
								while (zipEntry != null) {
									unzipFile(zipEntry, outputPath.resolve(zipEntry.getName()), zis);
									zipEntry = zis.getNextEntry();
								}
							}
							success = true;
						}
						catch (IOException e) {
							WISELogger.getSpecial(LogName.Backend).error("Unable to extract zip archive.", e);
						}
						
						if (success) {
							Job j = new Job(Settings.getJobDirectory(), jobName, JobStage.Finished);
                            j.saveConfig();
							finishedJobs.add(j);
							finishedJobAdded(j, finishedJobs.size() - 1);
						}
					}
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	private void unzipFile(ZipEntry entry, Path file, final ZipInputStream stream) throws IOException {
		if (entry.isDirectory()) {
			Files.createDirectories(file);
		}
		else {
			byte[] buffer = new byte[1024];
			try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
				int len;
				while ((len = stream.read(buffer)) >= 0) {
					fos.write(buffer, 0, len);
				}
			}
			BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
			attributes.setTimes(entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
		}
	}

	/**
	 * Create a tar archive of a finished job.
	 * @param jobName The name of the job to archive.
	 * @return True if the job was successfully archived.
	 */
	public boolean tarFinishedJob(String jobName) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			if (job.isPresent()) {
				try {
					boolean wasLocked = job.get().conditionalClose();
					Path toArchive = job.get().getXmlPath().getParent();
					Path path = Paths.get(Settings.getJobDirectory(), "Archives");
					if (!Files.exists(path))
						Files.createDirectories(path);
					path = path.resolve(jobName + ".tar.gz");
					
					try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(path.toFile())))) {
						out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
						out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
						out.setAddPaxHeadersForNonAsciiNames(true);
						tarFile(toArchive, jobName, out);
					}
					if (wasLocked)
						job.get().lock();
					success = true;
				}
				catch (IOException e) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to create tar archive.", e);
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	private void tarFile(Path file, String filename, TarArchiveOutputStream out) throws IOException {
		if (Files.isDirectory(file)) {
			List<Path> children = Files.list(file).collect(Collectors.toList());
			for (Path child : children) {
				tarFile(child, filename + "/" + child.getFileName().toString(), out);
			}
		}
		else {
			out.putArchiveEntry(new TarArchiveEntry(file.toFile(), filename));
			try (FileInputStream in = new FileInputStream(file.toFile())) {
				IOUtils.copy(in, out);
			}
			out.closeArchiveEntry();
		}
	}

	/**
	 * Untar an archived job. If the job directory already exists the archive will not be extracted.
	 * @param jobName The name of the job to extract.
	 * @return True if the archive was extracted or the job already exists.
	 */
	public boolean untarArchive(String jobName) {
		boolean success = false;
		listLock.lock();
		try {
			Optional<Job> job = finishedJobs.stream().filter(x -> x.name.equals(jobName)).findFirst();
			//skip existing jobs
			if (job.isPresent())
				success = true;
			else {
				Path outputPath = Paths.get(Settings.getJobDirectory(), jobName);
				//assume the existing directory is already what the user wants
				if (Files.exists(outputPath))
					success = true;
				else {
					Path archive = Paths.get(Settings.getJobDirectory(), "Archives", jobName + ".zip");
					//make sure the archive exists
					if (Files.exists(archive)) {
						try {
							InputStream stream = new FileInputStream(archive.toFile());
							if (GzipUtils.isCompressedFilename(archive.getFileName().toString()))
								stream = new GzipCompressorInputStream(stream);
							try (TarArchiveInputStream tin = new TarArchiveInputStream(stream)) {
								TarArchiveEntry entry;
								while ((entry = tin.getNextTarEntry()) != null) {
									Path newFile = outputPath.resolve(entry.getName());
									if (entry.isDirectory()) {
										Files.createDirectories(newFile);
									}
									else {
										if (!Files.exists(newFile.getParent()))
											Files.createDirectories(newFile.getParent());
										IOUtils.copy(tin, new FileOutputStream(newFile.toFile()));
									}
								}
							}
							success = true;
						}
						catch (IOException e) {
							WISELogger.getSpecial(LogName.Backend).error("Unable to extract tar archive.", e);
						}
						
						if (success) {
							Job j = new Job(Settings.getJobDirectory(), jobName, JobStage.Finished);
                            j.saveConfig();
							finishedJobs.add(j);
							finishedJobAdded(j, finishedJobs.size() - 1);
						}
					}
				}
			}
		}
		finally {
			listLock.unlock();
		}
		return success;
	}
	
	private LocalDateTime convertDate(XMLGregorianCalendar xc) {
	    GregorianCalendar gc = xc.toGregorianCalendar();
	    ZonedDateTime zdt = gc.toZonedDateTime();
	    return zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	/**
	 * Guess the number of cores that was requested for XML, binary, or JSON jobs.
	 * @param jobFilePath The path to the XML or JSON job file.
	 * @return A guess at the number of cores, uses 2 if nothing can be found.
	 */
	private int guessCoreCount(Path jobFilePath) {
		int cores = 2;
		if (jobFilePath.toString().endsWith(".fgmj") || jobFilePath.toString().endsWith(".fgmb")) {
			cores = FGMHelper.getCores(jobFilePath);
			if (cores < 0)
				cores = 2;
		}
		else if (jobFilePath.toString().endsWith(".xml")) {
			String regex = "cores=\"([0-9]+)\"";
			
			try (Scanner scanner = new Scanner(jobFilePath)) {
				Pattern pattern = Pattern.compile(regex);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (!Strings.isNullOrEmpty(line)) {
						Matcher matcher = pattern.matcher(line);
						if (matcher.find()) {
							String temp = matcher.group(1);
							cores = Integer.parseInt(temp);
							break;
						}
					}
				}
			}
			catch (Exception e) {
				WISELogger.getSpecial(LogName.Backend).warn("Unable to reload core count for " + jobFilePath.getFileName().toString());
			}
		}
		return cores;
	}

	/**
	 * Re-load the completed jobs from the job directory.
	 */
	public CompletableFuture<Long> repopulateLists() {
		return CompletableFuture.supplyAsync(() -> {
			listLock.lock();
			try {
				List<Path> directories = new ArrayList<>();
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(jobDirectory, p -> Files.isDirectory(p))) {
					directoryStream.forEach(directories::add);
				}
				catch (IOException e1) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to parse job directory", e1);
				}
				directories.sort(Comparator.comparing(Path::toString).reversed());
				
				try {
					JAXBContext context = JAXBContext.newInstance(ca.wise.lib.xml.List.class);
					Unmarshaller unmarshaller = context.createUnmarshaller();
					for (Path p : directories) {
						Path statusPath = p.resolve("status.xml");
						Path jStatusPath = p.resolve("status.json");
						
						boolean needsRestarted = Files.exists(p.resolve("restart.act"));
						if (needsRestarted) {
						    try {
						        Files.delete(p.resolve("restart.act"));
						    }
						    catch (Exception e) { }
						}
						
						if (Files.exists(statusPath)) {
							boolean error = false;
							LocalDateTime dt = LocalDateTime.now();
							ca.wise.lib.xml.List lst = null;
							try {
								lst = (ca.wise.lib.xml.List)unmarshaller.unmarshal(statusPath.toFile());
							}
							catch (JAXBException e) {
								WISELogger.getSpecial(LogName.Backend).warn("Unable to read status XML.", e);
								error = true;
							}
							
							if (error) {
								Files.delete(statusPath);
								Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), JobStage.Error);
								if (job.getSubmitted() == null)
								    job.setSubmitted(dt);
								if (!job.hasRequestedCores) {
		                            //try to read the number of cores to use
		                            int cores = 0;
		                            Path json = p.resolve("job.xml");
		                            if (Files.exists(json)) {
		                                cores = guessCoreCount(json);
		                            }
		                            
								    job.setRequestedCores(cores);
								}
                                job.saveConfig();
								finishedJobs.add(job);
								finishedJobAdded(job, finishedJobs.size() - 1);
							}
							else {
								boolean complete = false;
								boolean stopped = false;
								LocalDateTime started = LocalDateTime.now();
								LocalDateTime ended = LocalDateTime.now();
								for (Object v : lst.getStatusOrMessage()) {
									if (v instanceof ca.wise.lib.xml.List.Status) {
										ca.wise.lib.xml.List.Status st = (ca.wise.lib.xml.List.Status)v;
										if (st.getValue().equals("Submitted"))
											dt = convertDate(st.getTime());
										else if (st.getValue().equals("Started"))
											started = convertDate(st.getTime());
										else if (st.getValue().equals("Complete")) {
											ended = convertDate(st.getTime());
											complete = true;
											break;
										}
										//shutdown requested message added from the job
										else if (st.getValue().equals("Shutdown Requested")) {
											ended = convertDate(st.getTime());
											stopped = true;
											break;
										}
										//shutdown requested message added from the manager
										else if (st.getValue().equals("Information")) {
											if (st.getData() != null && st.getData().equals("Stop Requested")) {
												ended = convertDate(st.getTime());
												stopped = true;
												break;
											}
										}
									}
								}
								if (needsRestarted) {
                                    Files.delete(statusPath);
                                    Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString());
                                    if (job.getSubmitted() == null)
                                        job.setSubmitted(dt);
                                    if (!job.hasRequestedCores) {
                                        //try to read the number of cores to use
                                        int cores = 0;
                                        Path json = p.resolve("job.xml");
                                        if (Files.exists(json)) {
                                            cores = guessCoreCount(json);
                                        }
                                        
                                        job.setRequestedCores(cores);
                                    }
                                    newJob(job);
								}
								else if (complete || stopped) {
									JobStage stage;
									if (stopped)
										stage = JobStage.Stopped;
									else
										stage = JobStage.Finished;
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), stage);
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									job.setStart(started);
									job.setEnd(ended);
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    Path json = p.resolve("job.xml");
	                                    if (Files.exists(json)) {
	                                        cores = guessCoreCount(json);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    job.saveConfig();
									finishedJobs.add(job);
									finishedJobAdded(job, finishedJobs.size() - 1);
								}
								else if (Settings.getRestartOld()) {
									Files.delete(statusPath);
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString());
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    Path json = p.resolve("job.xml");
	                                    if (Files.exists(json)) {
	                                        cores = guessCoreCount(json);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    newJob(job);
								}
								else {
									Files.delete(statusPath);
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), JobStage.Error);
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									if (!job.hasRequestedCores) {
                                        //try to read the number of cores to use
                                        int cores = 0;
                                        Path json = p.resolve("job.xml");
                                        if (Files.exists(json)) {
                                            cores = guessCoreCount(json);
                                        }
                                        
									    job.setRequestedCores(cores);
									}
                                    job.saveConfig();
									finishedJobs.add(job);
									finishedJobAdded(job, finishedJobs.size() - 1);
								}
							}
						}
						else if (Files.exists(jStatusPath)) {
							boolean error = false;
							LocalDateTime dt = LocalDateTime.now();
							jobStatus.Builder schema = jobStatus.newBuilder();
							
							try (BufferedReader stream = new BufferedReader(new FileReader(jStatusPath.toString()))) {
								JsonFormat.parser().ignoringUnknownFields().merge(stream, schema);
							}
							catch (IOException e) {
								WISELogger.getSpecial(LogName.Backend).warn("Unable to read status JSON.", e);
								error = true;
							}

							Path json = p.resolve("job.fgmj");
							if (!Files.exists(json))
								json = p.resolve("job.fgmb");
							
							if (error) {
								Files.delete(jStatusPath);
								Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), JobStage.Error);
								if (job.getSubmitted() == null)
								    job.setSubmitted(dt);
								if (!job.hasRequestedCores) {
                                    //try to read the number of cores to use
                                    int cores = 0;
                                    if (Files.exists(json)) {
                                        cores = guessCoreCount(json);
                                    }
                                    
                                    job.setRequestedCores(cores);
								}
                                job.saveConfig();
								finishedJobs.add(job);
								finishedJobAdded(job, finishedJobs.size() - 1);
							}
							else {
								boolean complete = false;
								boolean stopped = false;
								LocalDateTime started = LocalDateTime.now();
								LocalDateTime ended = LocalDateTime.now();
								for (int i = 0; i < schema.getEntriesCount(); i++) {
									jobStatus.statusEntryOrBuilder entry = schema.getEntriesOrBuilder(i);
									if (entry.getEntryCase() == jobStatus.statusEntry.EntryCase.STATUS) {
										if (entry.getStatusOrBuilder().getStatus() == jobStatus.statusType.SUBMITTED)
											dt = Instant.ofEpochSecond(entry.getStatusOrBuilder().getTimeOrBuilder().getSeconds())
													.atZone(ZoneId.systemDefault()).toLocalDateTime();
										else if (entry.getStatusOrBuilder().getStatus() == jobStatus.statusType.STARTED)
											started = Instant.ofEpochSecond(entry.getStatusOrBuilder().getTimeOrBuilder().getSeconds())
													.atZone(ZoneId.systemDefault()).toLocalDateTime();
										else if (entry.getStatusOrBuilder().getStatus() == jobStatus.statusType.COMPLETE) {
											ended = Instant.ofEpochSecond(entry.getStatusOrBuilder().getTimeOrBuilder().getSeconds())
													.atZone(ZoneId.systemDefault()).toLocalDateTime();
											complete = true;
											break;
										}
										//shutdown requested message added from the job
										else if (entry.getStatusOrBuilder().getStatus() == jobStatus.statusType.SHUTDOWN_REQUESTED) {
											ended = Instant.ofEpochSecond(entry.getStatusOrBuilder().getTimeOrBuilder().getSeconds())
													.atZone(ZoneId.systemDefault()).toLocalDateTime();
											stopped = true;
											break;
										}
										//shutdown requested message added from the manager
										else if (entry.getStatusOrBuilder().getStatus() == jobStatus.statusType.INFORMATION) {
											if (entry.getStatusOrBuilder().getData() != null &&
													entry.getStatusOrBuilder().getData().getValue() != null &&
													entry.getStatusOrBuilder().getData().getValue().equals("Stop Requested")) {
												ended = Instant.ofEpochSecond(entry.getStatusOrBuilder().getTimeOrBuilder().getSeconds())
														.atZone(ZoneId.systemDefault()).toLocalDateTime();
												stopped = true;
												break;
											}
										}
									}
								}

                                if (needsRestarted) {
                                    Files.delete(jStatusPath);
                                    Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString());
                                    if (job.getSubmitted() == null)
                                        job.setSubmitted(dt);
                                    if (!job.hasRequestedCores) {
                                        //try to read the number of cores to use
                                        int cores = 0;
                                        if (Files.exists(json)) {
                                            cores = guessCoreCount(json);
                                        }
                                        
                                        job.setRequestedCores(cores);
                                    }
                                    newJob(job);
                                }
                                else if (complete || stopped) {
									JobStage stage;
									if (stopped)
										stage = JobStage.Stopped;
									else
										stage = JobStage.Finished;
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), stage);
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									job.setStart(started);
									job.setEnd(ended);
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    if (Files.exists(json)) {
	                                        cores = guessCoreCount(json);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    job.saveConfig();
									finishedJobs.add(job);
									finishedJobAdded(job, finishedJobs.size() - 1);
								}
								else if (Settings.getRestartOld()) {
									Files.delete(jStatusPath);
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString());
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    if (Files.exists(json)) {
	                                        cores = guessCoreCount(json);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    newJob(job);
								}
								else {
									Files.delete(jStatusPath);
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), JobStage.Error);
									if (job.getSubmitted() == null)
									    job.setSubmitted(dt);
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    if (Files.exists(json)) {
	                                        cores = guessCoreCount(json);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    job.saveConfig();
									finishedJobs.add(job);
									finishedJobAdded(job, finishedJobs.size() - 1);
								}
							}
						}
						else {
							Path jobPath = p.resolve("job.xml");
							boolean exists = Files.exists(jobPath);
							if (!exists) {
								jobPath = p.resolve("job.fgmj");
								exists = Files.exists(jobPath);
								if (!exists) {
									jobPath = p.resolve("job.fgmb");
									exists = Files.exists(jobPath);
								}
							}
							
							if (exists) {
								if (Settings.getRestartOld() || needsRestarted) {
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString());
									if (job.getSubmitted() == null)
									    job.setSubmitted(LocalDateTime.now());
									if (!job.hasRequestedCores) {
	                                    //try to read the number of cores to use
	                                    int cores = 0;
	                                    if (Files.exists(jobPath)) {
	                                        cores = guessCoreCount(jobPath);
	                                    }
	                                    
									    job.setRequestedCores(cores);
									}
                                    newJob(job);
								}
								else {
									Job job = new Job(jobDirectory.toAbsolutePath().toString(), p.getFileName().toString(), JobStage.Unknown);
									if (job.getSubmitted() == null)
									    job.setSubmitted(LocalDateTime.now());
									if (!job.hasRequestedCores) {
                                        //try to read the number of cores to use
                                        int cores = 0;
                                        if (Files.exists(jobPath)) {
                                            cores = guessCoreCount(jobPath);
                                        }
                                        
									    job.setRequestedCores(cores);
									}
							        job.saveConfig();
									finishedJobs.add(job);
									finishedJobAdded(job, finishedJobs.size() - 1);
								}
							}
						}
					}
				}
				catch (Exception e) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to iterate job directory.", e);
				}
			}
			finally {
				listLock.unlock();
			}
			
			return getQueuedCount();
		}, executor);
	}
	
	private void finishedJobRemove(final Job job, final int index) {
		SwingUtilities.invokeLater(() -> {
			finishedListeners.forEach(l -> l.itemRemoved(job, index));
		});
	}
	
	private void finishedJobAdded(final Job job, final int index) {
		SwingUtilities.invokeLater(() -> {
			finishedListeners.forEach(l -> l.itemAdded(job, index));
		});
	}
	
	private void queuedJobRemoved(final Job job, final int index) {
		SwingUtilities.invokeLater(() -> {
			queueListeners.forEach(l -> l.itemRemoved(job, index));
		});
	}
	
	private void queuedJobAdded(final Job job, final int index) {
		SwingUtilities.invokeLater(() -> {
			queueListeners.forEach(l -> l.itemAdded(job, index));
		});
	}
    
    private void validationJobRemoved(final Job job, final int index) {
        SwingUtilities.invokeLater(() -> {
            validateListeners.forEach(l -> l.itemRemoved(job, index));
        });
    }
    
    private void validationJobAdded(final Job job, final int index) {
        SwingUtilities.invokeLater(() -> {
            validateListeners.forEach(l -> l.itemAdded(job, index));
        });
    }
	
	public static interface IListChangedListener {
		
		void itemAdded(Job job, int index);
		void itemRemoved(Job job, int index);
	}

	@Override
	public void close() throws IOException {
		executor.shutdown();
	}
	
	private class JobKiller implements Runnable {
		
		public Shutdown details;
		
		public JobKiller(Shutdown details) {
			this.details = details;
		}
		
		@Override
		public void run() {
			details.priority = 2;
			terminateJob(details, null, false);
		}
	}
}
