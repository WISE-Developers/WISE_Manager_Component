package ca.wise.manager.ui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import com.g00fy2.versioncompare.Version;
import com.google.common.base.Strings;

import ca.hss.platform.OperatingSystem;
import ca.hss.tr.Translated;
import ca.hss.ui.StreamReaderThread;
import ca.hss.ui.TextAreaTableModel;
import ca.wise.lib.AffinityKernel;
import ca.wise.lib.CPUUsage;
import ca.wise.lib.GlobalConfiguration;
import ca.wise.lib.Job;
import ca.wise.lib.JobLists;
import ca.wise.lib.JobStartDetails;
import ca.wise.lib.MqttSettings;
import ca.wise.lib.WISELogger;
import ca.wise.lib.WISESupport;
import ca.wise.lib.Settings;
import ca.wise.lib.json.JobRequest;
import ca.wise.lib.json.Shutdown;
import ca.wise.lib.socket.SocketListener;
import ca.wise.manager.ui.QueuedJobsTableModel.MoveStatus;
import ca.wise.NamedMutex;
import ca.wise.SharedMemory;
import ca.wise.SharedBlock;
import ca.wise.lib.Job.JobStage;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.mqtt.MqttListener;
import ca.wise.lib.mqtt.MqttListener.IPauseRequestedListener;

import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;

class MainForm extends JFrame implements Translated {
	private static final long serialVersionUID = 1L;
	
	private Timer timer;
	private Timer processMonitorTimer;
	private OperatingSystemMXBean osBean;
	private ExecutorService executor = Executors.newFixedThreadPool(10);
	private JobLists jobs;
	private boolean pauseQueue = false;
	/**
	 * The currently running CPU blocks.
	 */
	private List<CPUUsage> runningJobs = new ArrayList<>();
	/**
	 * A list of blocks of CPUs that can be used to run jobs.
	 */
	private List<CPUUsage> coresAvailable = Collections.emptyList();
	/**
	 * Is a validation job currently running.
	 */
	private AtomicBoolean validationJobRunning = new AtomicBoolean(false);
	private final Object locker = new Object();
	private SocketListener socketListener;
	private MqttListener mqttListener;
	private MqttMessageTableModel messageTable;
	private JImageLabel lblOpenPrometheus;
	private JImageLabel lblMoveUp;
	private JImageLabel lblMoveDown;
	private JImageLabel btnPause;
	private JImageLabel btnStop;
	private JImageLabel btnBuilderStart;
	private JTable txtBuilderCommandLine;
	private boolean prometheusExists = false;
	private AtomicBoolean jobListPopulated = new AtomicBoolean(false);
	/**
	 * The named mutex and shared memory that regulates communication with W.I.S.E. and Prometheus
	 */
	private SharedBlock systemMemory;
	
	private Process builderSubprocess = null;
	
	private TrayIcon trayIcon;
	private SystemTray tray;
	
	MainForm() {
		initializeUi();
		if (Settings.getStartPaused())
			setQueuePaused(IPauseRequestedListener.PauseState.Paused);
		
		if (Settings.getLockCPU())
			AffinityKernel.setProcessorAffinity(-1)
				.withProcessor(0)
				.save();
		
		GlobalConfiguration.debug = BuildConfig.debug;
		
		Point location = Settings.getWindowLocation();
		if (location != null && MonitorManager.screenContains(location)) {
			this.setLocation(location);
		}
		Dimension size = Settings.getWindowSize();
		if (size != null)
			this.setSize(size);
		
		updateCpuUsageMonitor();
		updateWiseLocation();
		
		if (isSystemTraySupported()) {
			try {
				tray = SystemTray.getSystemTray();
				Image image = ImageIO.read(getClass().getResource("/Prometheus-Logo-40.png"));
				PopupMenu menu = new PopupMenu();
				MenuItem item = new MenuItem(getResources().getString("menu.show"));
				item.addActionListener(e -> {
					setVisible(true);
					setState(NORMAL);
					tray.remove(trayIcon);
				});
				menu.add(item);
				item = new MenuItem(getResources().getString("menu.exit"));
				item.addActionListener(e -> shutdown());
				menu.add(item);
				trayIcon = new TrayIcon(image, "W.I.S.E. Manager", menu);
				trayIcon.addActionListener(a -> {
					setVisible(true);
					setState(NORMAL);
					tray.remove(trayIcon);
				});
				trayIcon.setImageAutoSize(true);
			}
			catch (Exception e) {
				tray = null;
			}
		}
        executor.execute(this::initializeSharedMemory);
		
		SwingUtilities.invokeLater(() -> {
		    updateCoresAvailable();
			repopulateLists();
			startListener();
			startMqttListener();
			if (Settings.getBuilderStartAtStart())
				startWiseBuilder("");
		});
	}
	
	/**
	 * Should the system tray be used. Disabled on Linux because of bugs in Java.
	 * @return True if the system tray can be used, false if it should not.
	 */
	private boolean isSystemTraySupported() {
		return (!BuildConfig.debug) && SystemTray.isSupported() && OperatingSystem.getOperatingSystem().getType() != OperatingSystem.Type.Linux;
	}
	
	/**
	 * Initialize the shared memory block that allows communication with W.I.S.E. and Prometheus.
	 */
	private void initializeSharedMemory() {
        try {
            NamedMutex sharedMutex = new NamedMutex("lock.WISE");
            SharedMemory sharedMemory = new SharedMemory("memory.WISE");
            systemMemory = new SharedBlock(sharedMutex, sharedMemory);
            systemMemory.updateConfig((byte)Settings.getProcesses(), (short)Settings.getSkipProcesses(), Settings.getNumaLock());
        }
        catch (Exception e) {
            WISELogger.getSpecial(LogName.Ui).warn("Unable to initialize shared memory for communication with W.I.S.E./Prometheus", e);
        }
	}
	
	private void updateWiseLocation() {
		wiseLink.setText(Settings.getWiseExe());
		lblOpenPrometheus.setVisible(false);
		prometheusExists = false;
		getWiseVersion()
			.thenAccept(l -> {
				int errorStatus = 0;
				Version wise = null;
				String displayVersion = l;
				if (l != null && l.length() > 0) {
					long lines = l.chars().filter(c -> c == '\n').count() + 1;
					if (lines == 1) {
						l = l.replaceAll("\n", "");
						wise = new Version(l);
                        if (wise.isHigherThan("6.2.5.2"))
                            WISESupport.setSupportsManagerId(true);
                        else
                            WISESupport.setSupportsManagerId(false);
                        if (wise.getMinor() > 2000) {
                            int index = l.indexOf('.');
                            displayVersion = l.substring(index + 1);
                        }
                        wiseLink.setText(wiseLink.getText() + " (" + displayVersion + ")");
                        WISESupport.setSupportsValidation(wise.isHigherThan("6.2.6.0"));
                        //hack the warning message for 6 vs 7
						if (BuildConfig.version.getMajor() == 7 && wise.getMajor() == 6) {
							String temp = "7" + l.substring(1);
							wise = new Version(temp);
						}
						if (BuildConfig.version.isHigherThan(wise))
							errorStatus = 1;
					}
					else
						errorStatus = 2;
				}
				else
					errorStatus = 3;
				
				String message = null;
				if (errorStatus == 1 || errorStatus == 2) {
					message = getResources().getString("wise.version.old", (wise == null ? "Unknown" : displayVersion), BuildConfig.version.getOriginalString());
				}
				else if (errorStatus == 3) {
					message = getResources().getString("wise.version.missing");
				}
				
				lblOpenPrometheus.setVisible(prometheusExists);
				
				if (message != null) {
					if (JOptionPane.showConfirmDialog(this, message,
							getResources().getString("error"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						selectWiseExecutable(null);
					}
				}
			});
	}
	
	private CompletableFuture<String> getWiseVersion() {
		return CompletableFuture.supplyAsync(() -> {
			String version = null;
			String file = Settings.getWiseExe();
			if (file != null && file.length() > 0) {
				try {
					Path p = Paths.get(file);
					if (Files.exists(p)) {
						ProcessBuilder builder = new ProcessBuilder();
						builder.directory(p.getParent().toFile());
						builder.command(file, "-v2");
						Process process = builder.start();
						InputStream str = process.getInputStream();
						version = "";
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(str))) {
							String[] lines = reader.lines().toArray(String[]::new);
							for (String line : lines)
								version += line;
						}
						if (version.length() == 0 || !Character.isDigit(version.charAt(0))) {
							builder = new ProcessBuilder();
							builder.directory(p.getParent().toFile());
							builder.command(file, "--v2");
							process = builder.start();
							str = process.getInputStream();
							version = "";
							try (BufferedReader reader = new BufferedReader(new InputStreamReader(str))) {
								String[] lines = reader.lines().toArray(String[]::new);
								for (String line : lines)
									version += line;
							}
						}
						
						p = p.resolveSibling("Prometheus.exe");
						prometheusExists = Files.exists(p);
					}
				}
				catch (Exception e) {
					WISELogger.getSpecial(LogName.Ui).debug("Failed to get W.I.S.E. version", e);
					version = null;
				}
			}
			return version;
		}, executor);
	}
	
	private void updateCpuUsageMonitor() {
		if (Settings.getCpuUpdateFrequency() <= 0) {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			cpuUsageBar.setValue(0);
		}
		else {
			try {
				osBean = ManagementFactory.getOperatingSystemMXBean();
				Class.forName("com.sun.management.OperatingSystemMXBean");
				timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
					
					@Override
					public void run() {
						double value = ((com.sun.management.OperatingSystemMXBean)osBean).getSystemCpuLoad();
						int usage = (int)(value * 100.0);
						if (usage >= 0 && usage <= 100)
							cpuUsageBar.setValue(usage);
						
						try {
    						jobs.lockJobQueue()
    						    .forEach(j -> j.updateUsage());
						}
						catch (Exception e) {
						    //don't let the runnable crash
						}
						finally {
						    jobs.unlockJobQueue();
						}
					}
				}, 2000, Settings.getCpuUpdateFrequency() * 1000);
			}
			//the OS bean class cannot be found.
			catch (ClassNotFoundException|ClassCastException e) { }
		}
	}
	
	private void selectWiseExecutable(ActionEvent e) {
		String current = Settings.getWiseExe();
		JFileChooser chooser = new JFileChooser();
		if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows) {
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.exe"), "exe"));
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.script.windows"), "bat"));
		}
		else if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Mac)
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.app"), "app"));
		else
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.script.shell"), "sh"));
		chooser.setDialogTitle(getResources().getString("select.wise.exe"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setCurrentDirectory(new File(current));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			Settings.setWiseExe(chooser.getSelectedFile().getAbsolutePath());
			updateWiseLocation();
		}
	}
	
	/**
	 * Update the blocks of CPU cores that are available for jobs to run on.
	 */
	private void updateCoresAvailable() {
        synchronized(locker) {
            coresAvailable = CPUUsage.computeAvailableBlocks(Settings.getNumaLock(), Settings.getProcesses(), Settings.getSkipProcesses());
        }
	}
	
	/**
	 * A job is complete, remove its details from the running cores list.
	 * @param job The job that has completed.
	 */
	private void jobComplete(Job job) {
		synchronized(locker) {
		    runningJobs.remove(job.getCpuUsage());
			jobs.completeJob(job);
		}
		startJobs();
	}
	
	private void validationJobComplete(Job job) {
	    synchronized(locker) {
	        validationJobRunning.set(false);
	        jobs.completeValidationJob(job);

            //if validation was successful
            if (job.getStatus() == JobStage.ValidationComplete) {
                mqttListener.sendValidationReportSuccess(job);
            }
            else {
                mqttListener.sendValidationReportFailed(job);
            }
	    }
	    startValidationJobs();
	}
	
	public void addJob(JobStartDetails details) {
		synchronized(locker) {
			Job j = new Job(Settings.getJobDirectory(), details.jobName);
			j.setRequestedCores(details.cores);
			j.setSubmitted(LocalDateTime.now());
			j.setPriority(details.priority);
			if (details.validationState == JobRequest.VALIDATE_COMPLETE)
			    jobs.newFinishedJob(j);
			else if (details.validationState == JobRequest.VALIDATE_CURRENT)
			    jobs.newValidationJob(j);
			else
                jobs.newJob(j);
		}
		if (details.validationState == JobRequest.VALIDATE_CURRENT)
		    startValidationJobs();
		else if (details.validationState == JobRequest.VALIDATE_NONE)
		    startJobs();
	}
	
	/**
	 * Run over the job queue and try to start more jobs.
	 */
	private void startJobs() {
		synchronized(locker) {
			if (pauseQueue) {
				return;
			}
			if (processMonitorTimer == null) {
			    processMonitorTimer = new Timer();
			    //run the timer every 15 seconds to check if it's possible to start new jobs
			    processMonitorTimer.scheduleAtFixedRate(new TimerTask() {
                    
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(MainForm.this::startJobs);
                    }
                }, 0, 15000);
			}
			//don't allow list changes while looking for jobs to run
			jobs.runOnLists(() -> {
                //find the first job in the queue that is not yet running
			    MutableInt firstIndex = new MutableInt(-1);
                for (int i = 0; i < jobs.getJobQueue().size(); i++) {
                    Job j = jobs.getJobQueue().get(i);
                    if (j.getStatus() == JobStage.Queued) {
                        firstIndex.setValue(i);
                        break;
                    }
                }

                //if there is at least one job that is not yet running
                if (firstIndex.getValue() >= 0) {
                    List<StartupDetails> toStart = new ArrayList<>();
                    if (Settings.isRespectShmem() && systemMemory != null) {
        			    systemMemory.withSafeLock(details -> {
                            //loop over all the available CPU blocks to run on
                            for (CPUUsage available : coresAvailable) {
                                //check to see if all CPUs in this block are currently unused by Manager
                                if (runningJobs.stream()
                                        .noneMatch(x -> x.getRunningCores().and(available.getRunningCores()).bitCount() > 0)) {
                                    //also check that nobody started an external process on these CPUs
                                    if (details == null || available.getRunningCores().and(details.getUsedMask()).bitCount() == 0) {
                                        //we are going to use this block so add it to the used list
                                        runningJobs.add(available);
                                        //reserve the block in shared memory
                                        systemMemory.reserveBlock(details, available.getRunningCores());
                                        //save the details to start the job once we are outside the shared memory lock
                                        toStart.add(new StartupDetails(firstIndex.getValue(), available));
                                        //try the next job
                                        firstIndex.increment();
                                        //no more jobs to try
                                        if (firstIndex.getValue() >= jobs.getJobQueue().size())
                                            break;
                                    }
                                }
                            }
        			    });
                    }
                    else {
                        //loop over all the available CPU blocks to run on
                        for (CPUUsage available : coresAvailable) {
                            //check to see if all CPUs in this block are currently unused by Manager
                            if (runningJobs.stream()
                                    .noneMatch(x -> x.getRunningCores().and(available.getRunningCores()).bitCount() > 0)) {
                                //we are going to use this block so add it to the used list
                                runningJobs.add(available);
                                //save the details to start the job once we are outside the shared memory lock
                                toStart.add(new StartupDetails(firstIndex.getValue(), available));
                                //try the next job
                                firstIndex.increment();
                                //no more jobs to try
                                if (firstIndex.getValue() >= jobs.getJobQueue().size())
                                    break;
                            }
                        }
                    }
    			    
    			    //actually start the jobs we have found
    			    for (StartupDetails details : toStart) {
                        Job j = jobs.getJobQueue().get(details.index);
                        j.setCpuUsage(details.available);
                        j.addJobCompleteListener(this::jobComplete);
                        j.startJob();
    			    }
                }
			});
		}
	}
	
	private static class StartupDetails {
	    public final int index;
	    
	    public final CPUUsage available;
	    
	    public StartupDetails(int index, CPUUsage available) {
	        this.index = index;
	        this.available = available;
	    }
	}
	
	/**
	 * Try to start a new validation job if one is available.
	 */
	private void startValidationJobs() {
        synchronized(locker) {
            if (pauseQueue || validationJobRunning.get()) {
                return;
            }
            //don't allow list changes while looking for jobs to run
            jobs.runOnLists(() -> {
                List<Job> queue = jobs.getValidateQueue();
                while (queue.size() > 0) {
                    Job j = queue.get(0);
                    j.addJobCompleteListener(this::validationJobComplete);
                    validationJobRunning.set(true);
                    if (!j.validate()) {
                        validationJobRunning.set(false);
                        jobs.completeValidationJob(j);
                        j.clearJobCompleteListeners();
                        //tell MQTT listeners that the validation request couldn't be completed
                        if (mqttListener != null)
                            mqttListener.sendValidationReportFailed(j);
                    }
                    //if a validation job started exit the loop
                    else
                        break;
                }
            });
        }
	}
	
	private void repopulateLists() {
		synchronized(locker) {
		    jobListPopulated.set(false);
			jobs = new JobLists(Paths.get(Settings.getJobDirectory()));
			finishedJobs.setModel(new FinishedJobsTableModel(jobs));
			queuedJobs.setModel(new QueuedJobsTableModel(jobs));
			messageTable.setJobs(jobs);
			jobs.repopulateLists()
				.thenAccept(x -> {
					if (x > 0)
						startJobs();
					if (mqttListener != null)
					    mqttListener.repopulateLoadBalanceList();
					jobListPopulated.set(true);
				});
		}
	}
	
	private void startListener() {
		if (socketListener == null) {
			socketListener = new SocketListener(jobs, this::addJob);
			executor.execute(socketListener);
		}
	}
	
	/**
	 * Start the MQTT client.
	 */
	private void startMqttListener() {
		if (mqttListener == null) {
			String host;
			//if use internal broker is set always connect to localhost
			if (MqttSettings.useInternalBroker())
				host = "127.0.0.1";
			else
				host = MqttSettings.getHost();
			if (!Strings.isNullOrEmpty(host)) {
				if (MqttSettings.getPort() != 1883)
					host += ":" + MqttSettings.getPort();
				String username = MqttSettings.useAuthentication() ? MqttSettings.getUser() : "";
				String password = MqttSettings.useAuthentication() ? MqttSettings.getPassword() : "";
				mqttListener = MqttListener.newBuilder(jobs, host, MqttSettings.getTopic())
						.withAuthentication(username, password)
						.withPersistence(true, null)
						.withRpc(Settings.isRpcEnabled(), Settings.getRpcAddress(), Settings.getInternalRpcAddress(), Settings.getRpcPort())
						.withRpcDisplayPorts(Settings.getInternalRpcPort(), Settings.getExternalRpcPort())
						.build();
				mqttListener.addMqttMessageReceivedListener(messageTable::addMessage);
				mqttListener.addShutdownRequestListener(this::shutdown);
				mqttListener.setNewJobListener(this::addJob);
				mqttListener.addMqttConnectionListener(this::mqttConnectionChanged);
				mqttListener.addRestartRequestListener(() -> {
					try {
						WISELogger.getSpecial(LogName.Ui).info(App.getRestartCommand());
						App.unlockInstance();
						Runtime.getRuntime().exec(App.getRestartCommand());
					}
					catch (IOException e) { }
					shutdown();
				});
				mqttListener.addPauseRequestListener(new IPauseRequestedListener() {
                    
                    @Override
                    public void onPauseRequested(IPauseRequestedListener.PauseState pause) {
                        synchronized (locker) {
                            setQueuePaused(pause);
                        }
                    }
                    
                    @Override
                    public boolean isPaused() {
                        synchronized (locker) {
                            return pauseQueue;
                        }
                    }
                });
				mqttListener.addJobQueueRunListener(this::startJobs);
				WISESupport.setMqttId(mqttListener.getMqttId());
				mqttListener.connectAsync();
			}
		}
	}
	
	private void stopMqttListener() {
		messageTable.clearMessages();
		if (mqttListener != null) {
			try {
				mqttListener.close();
				mqttListener = null;
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Ui).error("Failed to disconnect from Mqtt broker.", e);
			}
		}
	}
	
	/**
	 * Does the passed {@link IPauseRequestedListener.PauseState} represent a paused
	 * or unpaused state.
	 * @return {@code true} if the state is paused, {@code false} if it is unpaused.
	 */
	private boolean shouldPause(IPauseRequestedListener.PauseState pause) {
	    return pause != IPauseRequestedListener.PauseState.Unpaused;
	}
	
	/**
	 * Change a boolean paused value to its opposite pause state.
	 * @param paused The current state of the job queue.
	 * @return {@link IPauseRequestedListener.PauseState#Unpaused} if the job queue is already paused,
	 *         {@link IPauseRequestedListener.PauseState#Paused} if it isn't paused.
	 */
	private IPauseRequestedListener.PauseState invertPauseState(boolean paused) {
	    return paused ? IPauseRequestedListener.PauseState.Unpaused : IPauseRequestedListener.PauseState.Paused;
	}
	
	/**
	 * Pause or unpause the job queue.
	 * @param pause Should the job queue be paused.
	 */
	private void setQueuePaused(IPauseRequestedListener.PauseState pause) {
		synchronized(locker) {
		    boolean pauseMe = shouldPause(pause);
			if (pauseQueue != pauseMe || pause == IPauseRequestedListener.PauseState.Offline) {
				pauseQueue = pauseMe;
				//cancel the start job timer if the queue is paused
				if (pauseQueue) {
				    if (processMonitorTimer != null) {
				        processMonitorTimer.cancel();
				        processMonitorTimer = null;
				    }
				}
				SwingUtilities.invokeLater(() -> {
					synchronized(locker) {
						btnPause.setSelected(pauseQueue);
						if (!pauseQueue) {
							btnPause.setIcon(new ImageIcon(getClass().getResource("/PauseHS.png")));
							btnPause.setToolTipText(getResources().getString("menu.pause"));
							startJobs();
							startValidationJobs();
						}
						else {
							btnPause.setIcon(new ImageIcon(getClass().getResource("/PlayHS.png")));
							btnPause.setToolTipText(getResources().getString("menu.play"));
							//disable the button so the user cannot unpause the job queue without restarting manager
							if (pause == IPauseRequestedListener.PauseState.Offline)
							    btnPause.setEnabled(false);
						}
					}
				});
			}
		}
	}
	
	/**
	 * Stop the selected job by either marking it as complete (if not running),
	 * sending a shutdown message to the job, or forcefully stopping the process.
	 */
	private void stopSelectedJob() {
		synchronized(locker) {
			final int index = queuedJobs.getSelectedRow();
			if (index >= 0) {
				QueuedJobsTableModel model = (QueuedJobsTableModel)queuedJobs.getModel();
				if (model.isRunning(index)) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem item = new JMenuItem(getResources().getString("menu.stop.request"), new ImageIcon(getClass().getResource("/Stop_16x.png")));
					item.addActionListener(y -> {
						//request the running process stop using MQTT
						if (JOptionPane.showConfirmDialog(this, getResources().getString("confirm.request"),
								getResources().getString("warning"), JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
							Shutdown shutdown = new Shutdown();
							shutdown.jobId = model.getJobId(index);
							shutdown.priority = 1;
							jobs.terminateJob(shutdown, mqttListener, false);
						}
					});
					menu.add(item);
					item = new JMenuItem(getResources().getString("menu.stop.terminate"), new ImageIcon(getClass().getResource("/TerminateProcess_16x.png")));
					item.addActionListener(y -> {
						//terminate the running process immediately
						if (JOptionPane.showConfirmDialog(this, getResources().getString("confirm.terminate"),
								getResources().getString("warning"), JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
							Shutdown shutdown = new Shutdown();
							shutdown.jobId = model.getJobId(index);
							shutdown.priority = 10;
							jobs.terminateJob(shutdown, mqttListener, false);
						}
					});
					menu.add(item);
					menu.show(btnStop, 0, btnStop.getHeight());
				}
				//job isn't running, just remove it from the queue
				else {
					if (JOptionPane.showConfirmDialog(this, getResources().getString("confirm.stop"),
							getResources().getString("warning"), JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
						Shutdown shutdown = new Shutdown();
						shutdown.jobId = model.getJobId(index);
						shutdown.priority = 1;
						jobs.terminateJob(shutdown, null, false);
					}
				}
			}
		}
	}
	
	private void queuedJobSelectionChanged(ListSelectionEvent event) {
		synchronized (locker) {
			int index = queuedJobs.getSelectedRow();
			if (index < 0) {
				lblMoveUp.setEnabled(false);
				lblMoveDown.setEnabled(false);
				btnStop.setEnabled(false);
			}
			else {
				btnStop.setEnabled(true);
				QueuedJobsTableModel model = (QueuedJobsTableModel)queuedJobs.getModel();
				MoveStatus status = model.getJobMoveStatus(index);
				boolean up = status == MoveStatus.UP || status == MoveStatus.UP_DOWN;
				boolean down = status == MoveStatus.DOWN || status == MoveStatus.UP_DOWN;
				if (up) {
					lblMoveUp.setEnabled(true);
					lblMoveUp.setIcon(new ImageIcon(getClass().getResource("/Upload_16x.png")));
				}
				else {
					lblMoveUp.setEnabled(false);
					lblMoveUp.setIcon(new ImageIcon(getClass().getResource("/Upload_gray_16x.png")));
				}
				if (down) {
					lblMoveDown.setEnabled(true);
					lblMoveDown.setIcon(new ImageIcon(getClass().getResource("/Download_16x.png")));
				}
				else {
					lblMoveDown.setEnabled(false);
					lblMoveDown.setIcon(new ImageIcon(getClass().getResource("/Download_grey_16x.png")));
				}
			}
		}
	}
	
	private void moveJobUp(String e) {
		synchronized (locker) {
			int index = queuedJobs.getSelectedRow();
			QueuedJobsTableModel model = (QueuedJobsTableModel)queuedJobs.getModel();
			if (model.moveJobUp(index))
				queuedJobs.setRowSelectionInterval(index - 1, index - 1);
		}
	}
	
	private void moveJobDown(String e) {
		synchronized (locker) {
			int index = queuedJobs.getSelectedRow();
			QueuedJobsTableModel model = (QueuedJobsTableModel)queuedJobs.getModel();
			if (model.moveJobDown(index))
				queuedJobs.setRowSelectionInterval(index + 1, index + 1);
		}
	}
	
	private void mqttConnectionChanged(MqttListener.ConnectionStatus status) {
		switch (status) {
		case CONNECTED:
			mqttStatusLabel.setText(getResources().getString("mqtt.connected", mqttListener.getMyId()));
			mqttStatusLabel.setOpaque(false);
			if (jobListPopulated.compareAndSet(true, true))
			    mqttListener.repopulateLoadBalanceList();
			break;
		case DISCONNECTED:
			mqttStatusLabel.setText(getResources().getString("mqtt.disconnected"));
			mqttStatusLabel.setOpaque(false);
			break;
		case CONNECTING:
			mqttStatusLabel.setText(getResources().getString("mqtt.connecting"));
			mqttStatusLabel.setOpaque(false);
			break;
		case FAILED:
			mqttStatusLabel.setText(getResources().getString("mqtt.failure"));
			mqttStatusLabel.setOpaque(true);
			mqttStatusLabel.setBackground(Color.RED);
			break;
		}
	}
	
	private void stopListener() {
		if (socketListener != null) {
			socketListener.stop();
			socketListener = null;
		}
	}
	
	/**
	 * Start W.I.S.E. builder as a subprocess.
	 */
	private void startWiseBuilder(String text) {
		if (builderSubprocess == null) {
			if (!Files.exists(Paths.get(Settings.getBuilderLocation()))) {
				JOptionPane.showMessageDialog(this, getResources().getString("error.builder.exist"),
						getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			try {
				TextAreaTableModel model = (TextAreaTableModel)txtBuilderCommandLine.getModel();
				model.clear();

				String output = "";
				try {
					Process temp = Runtime.getRuntime().exec(new String[] { "java", "-jar",
						Settings.getBuilderLocation(), "-v" });
					String line;
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(temp.getInputStream()))) {
						while ((line = reader.readLine()) != null)
							output += line;
					}
				}
				catch (Exception e) { }
				
				int version = 6;
				if (output.startsWith("W.I.S.E. Builder version"))
					version = 7;
				WISELogger.getSpecial(LogName.Ui).debug("Found W.I.S.E. Builder: " + output);
				
				String[] args;
				if (version == 7) {
					String brokerUrl;
					if (MqttSettings.useInternalBroker())
						brokerUrl = "tcp://127.0.0.1";
					else {
						brokerUrl = MqttSettings.getHost();
						if (brokerUrl.indexOf("://") < 0)
							brokerUrl = "tcp://" + brokerUrl;
					}
					brokerUrl += ":" + MqttSettings.getPort();
					args = new String[] { "java", "-jar",
							Settings.getBuilderLocation(), "-o", Settings.getBuilderOutputType(),
							"-m", "-j", Settings.getJobDirectory(), "-l", "32479", "-e", ":memory",
							"-w", Settings.getBuilderLogLevel(), "-u", "-b", brokerUrl };
					if (Settings.getBuilderSingleFile()) {
						args = Arrays.copyOf(args, args.length + 1);
						args[args.length - 1] = "-s";
					}
				}
				else {
					String log = "-warn";
					if (Settings.getBuilderLogLevel().equals("severe"))
						log = "-error";
					else if (Settings.getBuilderLogLevel().equals("info"))
						log = "-info";
					else if (Settings.getBuilderLogLevel().equals("off"))
						log = "-none";
					else if (Settings.getBuilderLogLevel().equals("all"))
						log = "-debug";
					args = new String[] { "java", "-jar",
							Settings.getBuilderLocation(), "-m", Settings.getJobDirectory(), "32479", log, "-memory", ":memory" };
				}
				WISELogger.getSpecial(LogName.Ui).debug("Starting W.I.S.E. Builder: " + String.join(" ", args));
				builderSubprocess = Runtime.getRuntime().exec(args);
				executor.execute(new StreamReaderThread(builderSubprocess.getInputStream(), model));
				executor.execute(new StreamReaderThread(builderSubprocess.getErrorStream(), model));
				btnBuilderStart.setIcon(new ImageIcon(getClass().getResource("/Stop_16x.png")));
			}
			catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, getResources().getString("error.builder.start"),
						getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
			}
		}
		else {
			builderSubprocess.destroyForcibly();
			builderSubprocess = null;
			btnBuilderStart.setIcon(new ImageIcon(getClass().getResource("/PlayHS.png")));
		}
	}
	
	/**
	 * Open the MQTT settings dialog.
	 */
	private void onMqttSettings() {
		MqttDlg dlg = new MqttDlg(this);
		dlg.setVisible(true);
		if (dlg.getResult() == JOptionPane.OK_OPTION) {
			stopMqttListener();
			startMqttListener();
		}
	}
	
	/**
	 * Add an existing job to the job queue.
	 */
	private void onAddExistingJob() {
	    JFileChooser chooser = new JFileChooser(Settings.getJobDirectory());
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
	        File fl = chooser.getSelectedFile();
	        File fl2 = new File(Settings.getJobDirectory());
	        
            boolean success = true;
            //move the job to the job directory before adding it
	        if (fl2.equals(fl.getParentFile())) {
	            if (jobs.containsJob(fl.getName()) != 0) {
	                success = false;
                    JOptionPane.showMessageDialog(this, getResources().getString("menu.file.add.loaded"),
                            getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
	            }
	        }
	        else {
	            Path p = Paths.get(fl2.getAbsolutePath(), fl.getName());
	            if (Files.exists(p)) {
	                success = false;
	                JOptionPane.showMessageDialog(this, getResources().getString("menu.file.add.exists"),
	                        getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
	            }
	            else {
    	            try {
    	                Path jobFile = fl.toPath().resolve("job.fgmx");
    	                //the job isn't an XML file
    	                if (!Files.exists(jobFile)) {
    	                    jobFile = fl.toPath().resolve("job.fgmj");
    	                    //the job isn't a JSON file
    	                    if (!Files.exists(jobFile)) {
    	                        jobFile = fl.toPath().resolve("job.fgmb");
    	                        //the job isn't a binary file
    	                        if (!Files.exists(jobFile)) {
    	                            JOptionPane.showMessageDialog(this, getResources().getString("menu.file.add.invalid"),
    	                                    getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
    	                            success = false;
    	                        }
    	                    }
    	                }
    	                
    	                if (success) {
                            Files.createDirectories(p);
                            FileUtils.copyDirectory(fl, p.toFile());
    	                }
    	            }
    	            catch (Exception e) {
    	                success = false;
                        JOptionPane.showMessageDialog(this, getResources().getString("menu.file.add.move"),
                                getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
    	            }
	            }
	        }
	        
	        if (success) {
	            Job j = new Job(Settings.getJobDirectory(), fl.getName());
	            jobs.newJob(j);
	            startJobs();
	        }
	    }
	}
	
	protected JProgressBar cpuUsageBar;
	protected JLinkLabel wiseLink;
	private JTextField txtMqttFilter;
	private JTable finishedJobs;
	private JTable queuedJobs;
	private JSplitPane splitPane;
	private JLabel mqttStatusLabel;
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		try {
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo.png")));
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo-40.png")));
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo-20.png")));
			setIconImages(icons);
		}
		catch (IOException e1) {
			WISELogger.getSpecial(LogName.Ui).debug("Unable to load icon", e1);
		}
		setMinimumSize(new Dimension(800, 600));
		
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu(getResources().getString("menu.file"));
		menuBar.add(fileMenu);
		
		JMenuItem settingsItem = new JMenuItem(getResources().getString("menu.file.settings"));
		settingsItem.addActionListener(e -> {
			SettingsDlg dlg = new SettingsDlg(this);
			dlg.setVisible(true);
			updateCoresAvailable();
			if (dlg.isJobDirectoryChanged())
				repopulateLists();
			if (dlg.isWiseExeChanged())
				updateWiseLocation();
			if (dlg.isRpcChanged()) {
				stopMqttListener();
				startMqttListener();
			}
			else if (dlg.isProcessCountChanged() || dlg.isSkipProcessCountChanged())
			    startJobs();
		});
		fileMenu.add(settingsItem);
		JMenuItem mqttConfigItem = new JMenuItem(getResources().getString("menu.file.mqtt"));
		mqttConfigItem.addActionListener(e -> onMqttSettings());
		fileMenu.add(mqttConfigItem);
		fileMenu.addSeparator();
		JMenuItem addJobItem = new JMenuItem(getResources().getString("menu.file.add"));
		addJobItem.addActionListener(e -> onAddExistingJob());
		fileMenu.add(addJobItem);
		fileMenu.addSeparator();
		JMenuItem aboutItem = new JMenuItem(getResources().getString("menu.file.about"));
		aboutItem.addActionListener(e -> {
			AboutDlg dlg = new AboutDlg(this);
			dlg.setVisible(true);
		});
		fileMenu.add(aboutItem);
		fileMenu.addSeparator();
		JMenuItem exitItem = new JMenuItem(getResources().getString("menu.file.exit"));
		exitItem.addActionListener(e -> shutdown());
		fileMenu.add(exitItem);
		
		JMenu viewMenu = new JMenu(getResources().getString("menu.view"));
		menuBar.add(viewMenu);
		
		final JMenu cpuUsageMenu = new JMenu(getResources().getString("menu.view.cpu"));
		viewMenu.add(cpuUsageMenu);
		
		int time = Settings.getCpuUpdateFrequency();
		final JCheckBoxMenuItem disableUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.disable"));
		if (time <= 0)
			disableUsageItem.setSelected(true);
		cpuUsageMenu.add(disableUsageItem);
		final JCheckBoxMenuItem oneSecondUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.second"));
		if (time == 1)
			oneSecondUsageItem.setSelected(true);
		cpuUsageMenu.add(oneSecondUsageItem);
		final JCheckBoxMenuItem tenSecondUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.tenseconds"));
		if (time == 10)
			tenSecondUsageItem.setSelected(true);
		cpuUsageMenu.add(tenSecondUsageItem);
		final JCheckBoxMenuItem thirtySecondUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.thirtyseconds"));
		if (time == 30)
			thirtySecondUsageItem.setSelected(true);
		cpuUsageMenu.add(thirtySecondUsageItem);
		final JCheckBoxMenuItem oneMinuteUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.minute"));
		if (time == 60)
			oneMinuteUsageItem.setSelected(true);
		cpuUsageMenu.add(oneMinuteUsageItem);
		final JCheckBoxMenuItem tenMinuteUsageItem = new JCheckBoxMenuItem(getResources().getString("menu.file.cpu.tenminute"));
		if (time == 600)
			tenMinuteUsageItem.setSelected(true);
		cpuUsageMenu.add(tenMinuteUsageItem);

		disableUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(true);
			oneSecondUsageItem.setSelected(false);
			tenSecondUsageItem.setSelected(false);
			thirtySecondUsageItem.setSelected(false);
			oneMinuteUsageItem.setSelected(false);
			tenMinuteUsageItem.setSelected(false);
			Settings.setCpuUpdateFrequency(0);
			updateCpuUsageMonitor();
		});
		oneSecondUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(false);
			oneSecondUsageItem.setSelected(true);
			tenSecondUsageItem.setSelected(false);
			thirtySecondUsageItem.setSelected(false);
			oneMinuteUsageItem.setSelected(false);
			tenMinuteUsageItem.setSelected(false);
			Settings.setCpuUpdateFrequency(1);
			updateCpuUsageMonitor();
		});
		tenSecondUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(false);
			oneSecondUsageItem.setSelected(false);
			tenSecondUsageItem.setSelected(true);
			thirtySecondUsageItem.setSelected(false);
			oneMinuteUsageItem.setSelected(false);
			tenMinuteUsageItem.setSelected(false);
			Settings.setCpuUpdateFrequency(10);
			updateCpuUsageMonitor();
		});
		thirtySecondUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(false);
			oneSecondUsageItem.setSelected(false);
			tenSecondUsageItem.setSelected(false);
			thirtySecondUsageItem.setSelected(true);
			oneMinuteUsageItem.setSelected(false);
			tenMinuteUsageItem.setSelected(false);
			Settings.setCpuUpdateFrequency(30);
			updateCpuUsageMonitor();
		});
		oneMinuteUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(false);
			oneSecondUsageItem.setSelected(false);
			tenSecondUsageItem.setSelected(false);
			thirtySecondUsageItem.setSelected(false);
			oneMinuteUsageItem.setSelected(true);
			tenMinuteUsageItem.setSelected(false);
			Settings.setCpuUpdateFrequency(60);
			updateCpuUsageMonitor();
		});
		tenMinuteUsageItem.addActionListener(f -> {
			disableUsageItem.setSelected(false);
			oneSecondUsageItem.setSelected(false);
			tenSecondUsageItem.setSelected(false);
			thirtySecondUsageItem.setSelected(false);
			oneMinuteUsageItem.setSelected(false);
			tenMinuteUsageItem.setSelected(true);
			Settings.setCpuUpdateFrequency(600);
			updateCpuUsageMonitor();
		});
		
		this.setJMenuBar(menuBar);
		
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setPreferredSize(new Dimension(getWidth(), 16));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		
		JLabel mqttLabel = new JLabel(getResources().getString("mqtt.heading"));
		statusPanel.add(mqttLabel);
		
		mqttStatusLabel = new JLabel(getResources().getString("mqtt.disconnnected"));
		mqttStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
		statusPanel.add(mqttStatusLabel);
		
		JLabel cpuUsageLabel = new JLabel(getResources().getString("status.cpu"));
		statusPanel.add(cpuUsageLabel);
		cpuUsageBar = new JProgressBar(0, 100);
		statusPanel.add(cpuUsageBar);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel exePanel = new JPanel();
		exePanel.setPreferredSize(new Dimension(getWidth(), 16));
		panel.add(exePanel, BorderLayout.SOUTH);
		exePanel.setLayout(new BoxLayout(exePanel, BoxLayout.X_AXIS));
		
		wiseLink = new JLinkLabel();
		wiseLink.addLinkClickedListener(l -> {
			String loc = Settings.getWiseExe();
			boolean error = true;
    		Path p = Paths.get(loc);
			if (Files.exists(p)) {
		    	if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows) {
		    		loc = loc.replace("/", "\\");
		    		try {
			    		Runtime.getRuntime().exec("explorer.exe /select, \"" + loc + "\"");
						error = false;
					}
		    		catch (IOException e1) { }
		    	}
		    	else {
		    		try {
						Desktop.getDesktop().open(new File(p.getParent().toString()));
						error = false;
					}
		    		catch (IOException e1) { }
		    	}
			}
			
			if (error) {
				JOptionPane.showMessageDialog(this, getResources().getString("wise.missing"),
						getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
			}
		});
		exePanel.add(wiseLink);
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JPanel topToolstrip = new JPanel();
		topToolstrip.setBorder(new EmptyBorder(0, 5, 0, 5));
		topToolstrip.setPreferredSize(new Dimension(getWidth(), 24));
		panel_2.add(topToolstrip, BorderLayout.NORTH);
		topToolstrip.setBackground(Color.WHITE);
		topToolstrip.setLayout(new BoxLayout(topToolstrip, BoxLayout.X_AXIS));
		
		btnPause = new JImageLabel(new ImageIcon(getClass().getResource("/PauseHS.png")));
		btnPause.setToolTipText(getResources().getString("menu.pause"));
		btnPause.addClickedListener(e -> {
			synchronized (locker) {
				setQueuePaused(invertPauseState(pauseQueue));
			}
		});
		topToolstrip.add(btnPause);
		
		btnStop = new JImageLabel(new ImageIcon(getClass().getResource("/Stop_16x.png")));
		btnStop.setEnabled(false);
		btnStop.setToolTipText(getResources().getString("menu.stop"));
		btnStop.addClickedListener(e -> {
			stopSelectedJob();
		});
		topToolstrip.add(btnStop);
		
		lblMoveUp = new JImageLabel(new ImageIcon(getClass().getResource("/Upload_gray_16x.png")));
		lblMoveUp.setToolTipText(getResources().getString("menu.up"));
		lblMoveUp.setEnabled(false);
		lblMoveUp.addClickedListener(this::moveJobUp);
		topToolstrip.add(lblMoveUp);
		
		lblMoveDown = new JImageLabel(new ImageIcon(getClass().getResource("/Download_grey_16x.png")));
		lblMoveDown.setToolTipText(getResources().getString("menu.down"));
		lblMoveDown.setEnabled(false);
		lblMoveDown.addClickedListener(this::moveJobDown);
		topToolstrip.add(lblMoveDown);
		
		//split the toolstrip into a left and right side
		topToolstrip.add(Box.createHorizontalGlue());
		
		lblOpenPrometheus = new JImageLabel(new ImageIcon(getClass().getResource("/Prometheus-Logo-16.png")));
		lblOpenPrometheus.setToolTipText(getResources().getString("menu.job.prometheus"));
		lblOpenPrometheus.addClickedListener(x -> {
			final int index = finishedJobs.getSelectedRow();
			if (index < 0) {
				Path p = Paths.get(Settings.getWiseExe());
				p = p.resolveSibling("Prometheus.exe");
				try {
					//Executor.Execute(p.toString());
					new ProcessBuilder(p.toString())
							.directory(p.getParent().toFile())
							.inheritIO()
							.start();
				}
				catch (IOException e1) {
					JOptionPane.showMessageDialog(MainForm.this, getResources().getString("error.exec"),
							getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				Job job = jobs.getFinishedJobs().get(index);
				if (job != null) {
					Path j = job.getSpecificationFile();
					Path p = Paths.get(Settings.getWiseExe());
					p = p.resolveSibling("Prometheus.exe");
					try {
						//Executor.Execute(p.toString(), j.toString());
						new ProcessBuilder(p.toString(), j.toString())
								.directory(p.getParent().toFile())
								.inheritIO()
								.start();
					}
					catch (IOException e1) {
						JOptionPane.showMessageDialog(MainForm.this, getResources().getString("error.exec"),
								getResources().getString("error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		topToolstrip.add(lblOpenPrometheus);
		
		final JImageLabel btnRestartJob = new JImageLabel(new ImageIcon(getClass().getResource("/RestoreFromRecycleBin_16x.png")));
		btnRestartJob.setToolTipText(getResources().getString("menu.job.rerun"));
		btnRestartJob.setEnabled(false);
		btnRestartJob.addClickedListener(x -> {
			final List<String> jobList = new ArrayList<>();
			jobs.runOnLists(() -> {
				int[] temp = finishedJobs.getSelectedRows();
				if (temp != null && temp.length > 0) {
					jobList.addAll(jobs.getFinishedJobs(temp));
				}
			});
			if (jobList.size() > 0) {
				int result;
				if (jobList.size() == 1) {
					result = JOptionPane.showConfirmDialog(MainForm.this, getResources().getString("warning.rerun.job", jobList.get(0)),
							getResources().getString("warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				}
				else {
					result = JOptionPane.showConfirmDialog(MainForm.this, getResources().getString("warning.rerun.jobs", jobList.size()),
							getResources().getString("warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				}
				
				if (result == JOptionPane.YES_OPTION) {
					for (String job : jobList) {
						jobs.rerunFinishedJob(job, true);
					}
                    startJobs();
				}
			}
		});
		topToolstrip.add(btnRestartJob);
		
		final JImageLabel btnArchiveJob = new JImageLabel(new ImageIcon(getClass().getResource("/archive_item.png")));
		btnArchiveJob.setToolTipText(getResources().getString("menu.job.archive"));
		btnArchiveJob.setEnabled(false);
		btnArchiveJob.addClickedListener(x -> {
			final List<String> jobList = new ArrayList<>();
			jobs.runOnLists(() -> {
				int[] temp = finishedJobs.getSelectedRows();
				if (temp != null && temp.length > 0) {
					jobList.addAll(jobs.getFinishedJobs(temp));
				}
			});
			if (jobList.size() > 0) {
				JPopupMenu menu = new JPopupMenu();
				JMenuItem item = new JMenuItem(getResources().getString("menu.archive.zip"), new ImageIcon(getClass().getResource("/archive_item.png")));
				item.addActionListener(y -> {
					BusyDlg dlg = new BusyDlg(this, () -> {
						for (String job : jobList) {
							jobs.zipFinishedJob(job);
						}
					});
					dlg.setVisible(true);
					if (jobList.size() == 1) {
						JOptionPane.showMessageDialog(MainForm.this, getResources().getString("menu.message.archive", jobList.get(0)),
								getResources().getString("message"), JOptionPane.INFORMATION_MESSAGE);
					}
					else {
						JOptionPane.showMessageDialog(MainForm.this, getResources().getString("menu.message.archives", String.valueOf(jobList.size())),
								getResources().getString("message"), JOptionPane.INFORMATION_MESSAGE);
					}
				});
				menu.add(item);
				item = new JMenuItem(getResources().getString("menu.archive.tar"), new ImageIcon(getClass().getResource("/container.png")));
				item.addActionListener(y -> {
					BusyDlg dlg = new BusyDlg(this, () -> {
						for (String job : jobList) {
							jobs.tarFinishedJob(job);
						}
					});
					dlg.setVisible(true);
					if (jobList.size() == 1) {
						JOptionPane.showMessageDialog(MainForm.this, getResources().getString("menu.message.archive", jobList.get(0)),
								getResources().getString("message"), JOptionPane.INFORMATION_MESSAGE);
					}
					else {
						JOptionPane.showMessageDialog(MainForm.this, getResources().getString("menu.message.archives", String.valueOf(jobList.size())),
								getResources().getString("message"), JOptionPane.INFORMATION_MESSAGE);
					}
				});
				menu.add(item);
				menu.show(btnArchiveJob, 0, btnArchiveJob.getHeight());
			}
		});
		topToolstrip.add(btnArchiveJob);
		
		JImageLabel btnDeleteJob = new JImageLabel(new ImageIcon(getClass().getResource("/delete_item.png")));
		btnDeleteJob.setToolTipText(getResources().getString("menu.job.delete"));
		btnDeleteJob.setEnabled(false);
		btnDeleteJob.addClickedListener(x -> {
			final List<String> jobList = new ArrayList<>();
			jobs.runOnLists(() -> {
				int[] temp = finishedJobs.getSelectedRows();
				if (temp != null && temp.length > 0) {
					jobList.addAll(jobs.getFinishedJobs(temp));
				}
			});
			if (jobList.size() > 0) {
				try {
					int result = JOptionPane.CANCEL_OPTION;
					if (jobList.size() == 1) {
						result = JOptionPane.showConfirmDialog(MainForm.this, getResources().getString("warning.delete.job", jobList.get(0)),
								getResources().getString("warning"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					}
					else {
						result = JOptionPane.showConfirmDialog(MainForm.this, getResources().getString("warning.delete.jobs", String.valueOf(jobList.size())),
								getResources().getString("warning"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					}
					if (result == JOptionPane.OK_OPTION) {
						BusyDlg dlg = new BusyDlg(this, () -> {
							for (String job : jobList) {
								jobs.deleteFinishedJob(job);
							}
						});
						dlg.setVisible(true);
					}
				}
				catch (Exception e) { }
			}
		});
		topToolstrip.add(btnDeleteJob);
		
		JImageLabel btnOpenFolder = new JImageLabel(new ImageIcon(getClass().getResource("/folder_open.png")));
		btnOpenFolder.setToolTipText(getResources().getString("menu.job.open"));
		btnOpenFolder.setEnabled(false);
		btnOpenFolder.addClickedListener(x -> {
			int index = finishedJobs.getSelectedRow();
			if (index >= 0)
				((FinishedJobsTableModel)finishedJobs.getModel()).doubleClick(index);
		});
		topToolstrip.add(btnOpenFolder);
		
		splitPane = new JSplitPane();
		if (Settings.getSplitterPosition() != null && Settings.getSplitterPosition() > 0)
			splitPane.setDividerLocation(Settings.getSplitterPosition());
		panel_2.add(splitPane, BorderLayout.CENTER);
		
		finishedJobs = new JTable();
		finishedJobs.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Point point = event.getPoint();
					int row = finishedJobs.rowAtPoint(point);
					if (row >= 0) {
						((FinishedJobsTableModel)finishedJobs.getModel()).doubleClick(row);
					}
				}
			}
		});
		finishedJobs.getSelectionModel().addListSelectionListener(e -> {
			if (finishedJobs.getSelectedRowCount() >= 1) {
				btnDeleteJob.setEnabled(true);
				btnArchiveJob.setEnabled(true);
				btnRestartJob.setEnabled(true);
				
				if (finishedJobs.getSelectedRowCount() == 1) {
					lblOpenPrometheus.setEnabled(true);
					btnOpenFolder.setEnabled(true);
				}
				else {
					lblOpenPrometheus.setEnabled(false);
					btnOpenFolder.setEnabled(false);
				}
			}
			else {
				btnDeleteJob.setEnabled(false);
				btnArchiveJob.setEnabled(false);
				btnRestartJob.setEnabled(false);
				lblOpenPrometheus.setEnabled(true);
				btnOpenFolder.setEnabled(false);
			}
		});
		JScrollPane rightPane = new JScrollPane(finishedJobs);
		splitPane.setRightComponent(rightPane);
		
		queuedJobs = new JTable();
		queuedJobs.setRowSelectionAllowed(true);
		queuedJobs.setColumnSelectionAllowed(false);
		queuedJobs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		queuedJobs.getSelectionModel().addListSelectionListener(this::queuedJobSelectionChanged);
		JScrollPane leftPane = new JScrollPane(queuedJobs);
		splitPane.setLeftComponent(leftPane);
		
		JTabbedPane tabPane = new JTabbedPane();
		panel_1.add(tabPane);
		
		JPanel panel_3 = new JPanel();
		tabPane.addTab(getResources().getString("tab.manager"), panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JPanel bottomToolstrip = new JPanel();
		bottomToolstrip.setBorder(new EmptyBorder(0, 10, 0, 10));
		bottomToolstrip.setPreferredSize(new Dimension(getWidth(), 24));
		panel_3.add(bottomToolstrip, BorderLayout.NORTH);
		bottomToolstrip.setBackground(Color.WHITE);
		bottomToolstrip.setLayout(new BoxLayout(bottomToolstrip, BoxLayout.X_AXIS));
		
		txtMqttFilter = new JTextField();
		txtMqttFilter.setPreferredSize(new Dimension(100, 20));
		txtMqttFilter.setMaximumSize(new Dimension(100, 24));
		bottomToolstrip.add(txtMqttFilter);
		txtMqttFilter.setColumns(10);
		
		JImageLabel btnZap = new JImageLabel(new ImageIcon(getClass().getResource("/current_filter.png")));
		btnZap.addClickedListener(l -> {
			btnZap.setSelected(!btnZap.isSelected());
			messageTable.setOnlyActive(btnZap.isSelected());
		});
		
		JImageLabel btnRefresh = new JImageLabel(new ImageIcon(getClass().getResource("/reset_filter.png")));
		btnRefresh.addClickedListener(l -> {
			String filter = txtMqttFilter.getText();
			if (filter.length() == 0) {
				messageTable.setFilter(null);
				btnZap.setEnabled(true);
			}
			else {
				messageTable.setFilter(filter);
				btnZap.setEnabled(false);
			}
		});
		bottomToolstrip.add(btnRefresh);
		bottomToolstrip.add(btnZap);
		
		JPanel panel_4 = new JPanel();
		panel_4.setBackground(Color.WHITE);
		panel_4.setBorder(new EmptyBorder(3, 2, 3, 2));
		panel_4.setPreferredSize(new Dimension(5, 0));
		panel_4.setMaximumSize(new Dimension(5, 100));
		bottomToolstrip.add(panel_4);
		panel_4.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_5 = new JPanel();
		panel_5.setBackground(new Color(189, 189, 189));
		panel_5.setPreferredSize(new Dimension(1, 0));
		panel_5.setMaximumSize(new Dimension(1, 100));
		panel_4.add(panel_5, BorderLayout.CENTER);
		
		JImageLabel btnOrder = new JImageLabel(new ImageIcon(getClass().getResource("/autoscroll.png")));
		btnOrder.setSelected(true);
		btnOrder.addClickedListener(e -> {
			btnOrder.setSelected(!btnOrder.isSelected());
			messageTable.setAutoScroll(btnOrder.isSelected());
		});
		bottomToolstrip.add(btnOrder);
		
		JPanel panel_6 = new JPanel();
		panel_6.setBackground(Color.WHITE);
		panel_6.setBorder(new EmptyBorder(3, 2, 3, 2));
		panel_6.setPreferredSize(new Dimension(5, 0));
		panel_6.setMaximumSize(new Dimension(5, 100));
		bottomToolstrip.add(panel_6);
		panel_6.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_7 = new JPanel();
		panel_7.setBackground(new Color(189, 189, 189));
		panel_7.setPreferredSize(new Dimension(1, 0));
		panel_7.setMaximumSize(new Dimension(1, 100));
		panel_6.add(panel_7, BorderLayout.CENTER);
		
		JImageLabel btnClear = new JImageLabel(new ImageIcon(getClass().getResource("/clear.png")));
		btnClear.addClickedListener(l -> messageTable.clearMessages());
		bottomToolstrip.add(btnClear);
		
		JTable mqttTable = new JTable();
		mqttTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Point point = event.getPoint();
					int row = mqttTable.rowAtPoint(point);
					if (row >= 0) {
						messageTable.showMessageBody(row, MainForm.this);
					}
				}
			}
		});
		messageTable = new MqttMessageTableModel(mqttTable);
		mqttTable.setModel(messageTable);
		
		JScrollPane scrollPane = new JScrollPane(mqttTable);
		panel_3.add(scrollPane, BorderLayout.CENTER);
		
		JPanel panel_8 = new JPanel();
		tabPane.addTab(getResources().getString("tab.builder"), panel_8);
		panel_8.setLayout(new BorderLayout(0, 0));
		
		JPanel builderToolstrip = new JPanel();
		builderToolstrip.setBorder(new EmptyBorder(0, 10, 0, 10));
		builderToolstrip.setPreferredSize(new Dimension(getWidth(), 24));
		panel_8.add(builderToolstrip, BorderLayout.NORTH);
		builderToolstrip.setBackground(Color.WHITE);
		builderToolstrip.setLayout(new BoxLayout(builderToolstrip, BoxLayout.X_AXIS));
		
		JImageLabel btnBuilderSettings = new JImageLabel(new ImageIcon(getClass().getResource("/Settings_16x.png")));
		btnBuilderSettings.addClickedListener((e) -> {
			BuilderSettingsDlg dlg = new BuilderSettingsDlg(MainForm.this);
			dlg.setVisible(true);
			if (dlg.getResult() == JOptionPane.OK_OPTION) {
				if (dlg.isJobDirectoryChanged())
					repopulateLists();
			}
		});
		builderToolstrip.add(btnBuilderSettings);
		
		btnBuilderStart = new JImageLabel(new ImageIcon(getClass().getResource("/PlayHS.png")));
		btnBuilderStart.addClickedListener(this::startWiseBuilder);
		builderToolstrip.add(btnBuilderStart);
		
		txtBuilderCommandLine = new JTable();
		txtBuilderCommandLine.setModel(new TextAreaTableModel(50));
		txtBuilderCommandLine.setTableHeader(null);
		JScrollPane scrlBuilderCommandLine = new JScrollPane(txtBuilderCommandLine);
		panel_8.add(scrlBuilderCommandLine);
		
		if (isSystemTraySupported())
			this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		else
			this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				if (Settings.getMinmizeTray() && trayIcon != null && tray != null) {
					try {
						tray.add(trayIcon);
						setVisible(false);
					}
					catch (AWTException e1) { }
				}
				else {
					shutdown();
				}
			}
		});
	}
	
	/**
	 * Cleanup resources and close the application.
	 * @param kill If true the application will be forcefully closed after resources are cleaned up.
	 */
	private void shutdown() {
		if (MainForm.this.getState() == JFrame.NORMAL) {
			Settings.setWindowLocation(MainForm.this.getLocation());
			Settings.setWindowSize(MainForm.this.getSize());
		}
		Settings.setSplitterPosition(splitPane.getDividerLocation());
		if (tray != null && trayIcon != null)
			tray.remove(trayIcon);
		if (builderSubprocess != null) {
			builderSubprocess.destroyForcibly();
			builderSubprocess = null;
		}
		stopListener();
		stopMqttListener();
		try {
			jobs.close();
		}
		catch (IOException e) { }
		executor.shutdown();
		if (timer != null)
			timer.cancel();
		//if (getDefaultCloseOperation() == WindowConstants.DO_NOTHING_ON_CLOSE)
			System.exit(0);
	}
}
