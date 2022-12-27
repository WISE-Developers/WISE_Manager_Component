package ca.wise.lib.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import ca.wise.lib.INewJobListener;
import ca.wise.lib.JobLists;
import ca.wise.lib.JobStartDetails;
import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.json.JobRequest;

public class ConnectionHandler implements Runnable {
	
	private Socket socket;
	private INewJobListener listener;
	private JobLists jobs;
	
	public ConnectionHandler(JobLists jobs, Socket socket, INewJobListener listener) {
		this.jobs = jobs;
		this.socket = socket;
		this.listener = listener;
	}

	@Override
	public void run() {
		String jobName = "";
		int cores = -1;
		int priority = 0;
		int validate = JobRequest.VALIDATE_NONE;
		try {
			try (BufferedReader rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				while(true) {
					String line = rdr.readLine();
					if (line == null || line.length() == 0)
						break;
					String[] split = line.split(",");
					if (split.length == 1) {
						if (split[0].equals("ACK")) {
							try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
								writer.write("ACK" + "\r\n");
								writer.flush();
							}
							break;
						}
					}
					else if (split.length == 2) {
						if (split[0].equals("NAME"))
							jobName = split[1];
						else if (split[0].equals("CORES")) {
							try {
								cores = Integer.parseInt(split[1]);
							}
							catch (NumberFormatException e) { }
						}
                        else if (split[0].equals("PRIORITY")) {
                            try {
                                priority = Integer.parseInt(split[1]);
                            }
                            catch (NumberFormatException e) { }
                        }
                        else if (split[0].equals("VALIDATE")) {
                            try {
                                validate = Integer.parseInt(split[1]);
                            }
                            catch (NumberFormatException e) { }
                        }
						else if (split[0].equals("QUERY")) {
							handleQuery(split[1]);
							break;
						}
					}
					else if (split.length == 3) {
						if (split[0].equals("EXECUTE")) {
							handleExecute(split);
							break;
						}
					}
				}
			}
		}
		catch (Exception e) {
			WISELogger.getSpecial(LogName.Backend).error("Error reading from socket.", e);
		}
		finally {
			try {
				socket.close();
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Failed to close the socket connection.", e);
			}
		}
		if (jobName != null && jobName.length() > 0 && cores > 0)
			listener.onNewJob(new JobStartDetails(jobName, cores, priority, validate));
	}
	
	protected void handleQuery(String query) {
		try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
			if (query.equals("COMPLETE")) {
				List<String> list = jobs.getCompleteJobList();
				for (String job : list) {
					writer.write(job + "\r\n");
					writer.flush();
				}
			}
			else if (query.equals("RUNNING")) {
				List<String> list = jobs.getRunningJobs();
				for (String job : list) {
					writer.write(job + "\r\n");
					writer.flush();
				}
			}
			else if (query.equals("QUEUED")) {
				List<String> list = jobs.getQueuedJobs();
				for (String job : list) {
					writer.write(job + "\r\n");
					writer.flush();
				}
			}
			else if (query.equals("ARCHIVED")) {
				List<String> list = jobs.getArchivedJobs();
				for (String job : list) {
					writer.write(job + "\r\n");
					writer.flush();
				}
			}
			writer.write("COMPLETE\r\n");
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).error("Error writing query response to socket.", e);
		}
	}
	
	protected void handleExecute(String[] split) {
		try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
			if (split[1].equals("TAR")) {
				String jobName = split[2];
				if (!jobs.tarFinishedJob(jobName))
					writer.write("NODIR\r\n");
			}
			else if (split[1].equals("UNTAR")) {
				String jobName = split[2];
				if (!jobs.untarArchive(jobName))
					writer.write("NODIR\r\n");
			}
			else if (split[1].equals("ZIP")) {
				String jobName = split[2];
				if (!jobs.zipFinishedJob(jobName))
					writer.write("NODIR\r\n");
			}
			else if (split[1].equals("UNZIP")) {
				String jobName = split[2];
				if (!jobs.unzipArchive(jobName))
					writer.write("NODIR\r\n");
			}
			else if (split[1].equals("DELETE")) {
				String jobName = split[2];
				if (!jobs.deleteFinishedJob(jobName))
					writer.write("NODIR\r\n");
			}
			writer.write("COMPLETE\r\n");
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).error("Error writing execute response to socket.", e);
		}
	}
}
