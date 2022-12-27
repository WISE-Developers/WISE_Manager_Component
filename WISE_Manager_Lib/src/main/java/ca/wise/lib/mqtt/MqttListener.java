package ca.wise.lib.mqtt;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import ca.wise.comms.server.proto.FileUpload.FileType;
import ca.wise.comms.server.proto.GeoserverUpload;
import ca.wise.comms.server.proto.ValidationReport;
import ca.wise.fgm.FGMHelper;
import ca.wise.fgm.LoadBalance;
import ca.wise.geoserver.CoverageStores;
import ca.wise.geoserver.Workspaces;
import ca.wise.geoserver.proto.ListCoverageStore.CoverageStoreList;
import ca.wise.geoserver.proto.ListWorkspace.WorkspaceList;
import ca.wise.lib.GlobalConfiguration;
import ca.wise.lib.INewJobListener;
import ca.wise.lib.Job;
import ca.wise.lib.JobLists;
import ca.wise.lib.JobLists.IListChangedListener;
import ca.wise.lib.JobStartDetails;
import ca.wise.lib.MqttSettings;
import ca.wise.lib.WISELogger;
import ca.wise.lib.Settings;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.json.Checkin;
import ca.wise.lib.json.FileList;
import ca.wise.lib.json.JobHistory;
import ca.wise.lib.json.Manage;
import ca.wise.lib.json.Shutdown;
import ca.wise.lib.json.JobHistory.HistoryStatus;
import ca.wise.lib.json.JobHistoryRequest;
import ca.wise.lib.json.JobRequest;
import ca.wise.lib.json.JobResponse;
import ca.wise.lib.mqtt.MqttMessage.MessageType;
import ca.wise.rpc.GenericRPCClient;
import ca.wise.rpc.RPCClient;
import ca.wise.rpc.RPCServer;
import ca.wise.settings.BuildConfig;
import lombok.Getter;
import lombok.Setter;
import oshi.SystemInfo;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class MqttListener implements Closeable, MqttCallback, IMqttActionListener {
	
	public static final String TARGET_ALL = "broadcast";
	public static final String TARGET_MANAGER = "manager";
	
	public static final String TOPIC_CHECKIN = "checkin";
	public static final String TOPIC_SHUTDOWN = "shutdown";
	public static final String TOPIC_HISTORY = "responsehistory";
	public static final String TOPIC_AVAILABLE = "available";
	public static final String TOPIC_REMOTE_AVAILABLE = "remoteavailable";
	public static final String TOPIC_FILE_STREAM_AVAILABLE = "filerequest";
	public static final String TOPIC_STATUS = "status";
	public static final String TOPIC_BALANCE_REQUEST = "balancerequest";
	public static final String TOPIC_BALANCE_AVAILABLE = "balanceavailable";
	public static final String TOPIC_BALANCE_START = "balancestart";
	public static final String TOPIC_BALANCE_RPC_START = "balancerpcstart";
	public static final String TOPIC_BALANCE_LIST = "balancelist";
    public static final String TOPIC_VALIDATE = "validate";

	private MqttAsyncClient client;
	private final JobLists jobs;
	@Getter private final String address;
	private final String username;
	private final String password;
	@Getter private String myId;
	private boolean isStaticId = false;
	@Getter private final String topic;
	@Getter private final boolean useMemoryPersistence;
	@Getter private final String persistenceDirectory;
	private ThreadPoolExecutor executor;
	private ThreadPoolExecutor geoServerExecutor;
	private Lock locker = new ReentrantLock();
	private ObjectMapper mapper = new ObjectMapper();
	private boolean shuttingDown = false;
	@Getter private ConnectionStatus connStatus = ConnectionStatus.DISCONNECTED;
	@Setter private INewJobListener newJobListener;
	private List<IMqttMessageReceivedListener> listeners = new ArrayList<>();
	private List<IShutdownRequestListener> shutdownListeners = new ArrayList<>();
	private List<IRestartRequestedListener> restartListeners = new ArrayList<>();
    private List<IPauseRequestedListener> pauseListeners = new ArrayList<>();
	private List<IConnectionListener> connectionListeners = new ArrayList<>();
	private List<IRequestJobQueueRunListener> runJobQueueListeners = new ArrayList<>();
	private Lock remoteJobRequestLock = new ReentrantLock();
	private Map<RemoteType, List<JobRequest>> remoteJobRequests = new HashMap<>();
	private Map<String, BalanceWrapper> balanceJobRequests = new HashMap<>();
	private ReentrantLock geoserverLock = new ReentrantLock();
	private IPauseRequestedListener.PauseState pauseState = IPauseRequestedListener.PauseState.Unpaused;
	
	private String rpcAddress;
	private String rpcInternalAddress;
	private Integer rpcPort;
    private Integer rpcInternalPort;
    private Integer rpcExternalPort;
	private RPCServer rpcServer;
	
	public static long getPID() {
		Long pid = null;
		try {
			String processName = ManagementFactory.getRuntimeMXBean().getName();
			pid = Long.parseLong(processName.split("@")[0]);
		}
		catch (Exception ex) { }
		if (pid == null) {
			pid = new Random(System.currentTimeMillis()).nextLong();
		}
		return pid;
	}
	
	public static String getHost() {
		SystemInfo system = new oshi.SystemInfo();
		OperatingSystem os = system.getOperatingSystem();
		NetworkParams params = os.getNetworkParams();
		return params.getHostName();
	}
	
	public static String generateComputerId() {
		return "manager_" + getPID() + "-" + getHost().toUpperCase();
	}
	
	public static class MqttListenerBuilder {
		private JobLists jobs;
		private String address;
		private String topic;
		private String username;
		private String password;
		private String persistDirectory;
		private boolean persistMemory = false;
		private boolean rpcEnabled = false;
		private String rpcAddress;
		private String rpcInternalAddress;
		private int rpcPort;
	    private Integer rpcInternalPort;
	    private Integer rpcExternalPort;
		
		protected MqttListenerBuilder(JobLists jobs, String address, String topic) {
			this.jobs = jobs;
			this.address = address;
			this.topic = topic;
		}
		
		/**
		 * The manager for running and complete jobs.
		 */
		public MqttListenerBuilder withJobList(JobLists jobs) {
			this.jobs = jobs;
			return this;
		}
		
		/**
		 * The URL of the MQTT broker.
		 */
		public MqttListenerBuilder withBroker(String address) {
			this.address = address;
			return this;
		}
		
		/**
		 * The base topic to use when accessing the MQTT broker.
		 */
		public MqttListenerBuilder withTopic(String topic) {
			this.topic = topic;
			return this;
		}
		
		/**
		 * The broker requires authentication in order to connect.
		 * @param username The username to authenticate with the MQTT broker. Use null if no username is required.
		 * @param password The password to authenticate with the MQTT broker. Use null if no password is required.
		 */
		public MqttListenerBuilder withAuthentication(String username, String password) {
			this.username = username;
			this.password = password;
			return this;
		}
		
		/**
		 * The type of persistent store to use.
		 * @param inMemory Use memory persistence for QOS 2 messages instead of the default file persistence.
		 * @param fileLocation The directory to persist messages if file persistence is used.
		 * @return
		 */
		public MqttListenerBuilder withPersistence(boolean inMemory, String fileLocation) {
			persistMemory = inMemory;
			persistDirectory = fileLocation;
			return this;
		}
        
        /**
         * Start an RPC server to receive files from builder instances.
         * @param enabled Should the RPC server be enabled.
         * @param address The address that will be used to connect to the RPC server.
         * @param internalAddress The internal IP address that will be used to connect tot he RPC server.
         * @param port The port that the RPC server will listen on.
         */
        public MqttListenerBuilder withRpc(boolean enabled, String address, String internalAddress, int port) {
            rpcEnabled = enabled;
            rpcAddress = address;
            rpcInternalAddress = internalAddress;
            rpcPort = port;
            return this;
        }
        
        /**
         * Advertise different ports than the one W.I.S.E. Manager listens on.
         * @param internalPort The port to advertise with the internal address.
         * @param externalPort The port to advertise with the external address.
         */
        public MqttListenerBuilder withRpcDisplayPorts(Integer internalPort, Integer externalPort) {
            rpcInternalPort = internalPort;
            rpcExternalPort = externalPort;
            return this;
        }
		
		/**
		 * Construct the MQTT listener.
		 */
		public MqttListener build() {
			if (jobs == null || Strings.isNullOrEmpty(address) || Strings.isNullOrEmpty(topic))
				throw new InvalidParameterException();
			
			MqttListener retval = new MqttListener(jobs, address, topic, username, password, persistMemory, persistDirectory);
			if (rpcEnabled) {
				retval.rpcAddress = rpcAddress;
				retval.rpcPort = rpcPort;
				retval.rpcInternalAddress = rpcInternalAddress;
				retval.rpcInternalPort = rpcInternalPort;
				retval.rpcExternalPort = rpcExternalPort;
			}
			return retval;
		}
	}
	
	public static MqttListenerBuilder newBuilder(JobLists jobs, String address, String topic) { return new MqttListenerBuilder(jobs, address, topic); }
	
	/**
	 * Create a new MQTT listener. This call will not open the MQTT connection, just prepare the listener.
	 * @param jobs The manager for running and complete jobs.
	 * @param address The URL of the MQTT broker.
	 * @param topic The base topic to use when accessing the MQTT broker.
	 * @param username The username to authenticate with the MQTT broker. Use null if no username is required.
	 * @param password The password to authenticate with the MQTT broker. Use null if no password is required.
	 * @param useMemory Use memory persistence for QOS 2 messages instead of the default file persistence.
	 * @param persistenceDirectory The directory to persist messages if file persistence is used.
	 */
	private MqttListener(JobLists jobs, String address, String topic, String username, String password, boolean useMemory, String persistenceDirectory) {
		this.jobs = jobs;
		//listen for jobs to be added or removed from the finished job list
		jobs.addFinishedListChangeListener(new IListChangedListener() {
            
            @Override
            public void itemRemoved(Job job, int index) { }
            
            @Override
            public void itemAdded(Job job, int index) {
                //remove the job from the load balance list, if it exists there
                removeBalanceJob(job.getName());
            }
        });
		if (!address.startsWith("tcp:") && !address.startsWith("ws:") && !address.startsWith("wss:"))
			address = "tcp://" + address;
		this.address = address;
		this.topic = topic;
		this.username = username;
		this.password = password;
		this.useMemoryPersistence = useMemory;
		this.persistenceDirectory = persistenceDirectory;
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		executor = new ThreadPoolExecutor(2, 16, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
		geoServerExecutor = new ThreadPoolExecutor(1, 8, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	}
	
	/**
	 * Add a listener for connect/disconnect messages.
	 */
	public void addMqttConnectionListener(IConnectionListener listener) {
		connectionListeners.add(listener);
	}
	
	/**
	 * Remove a listener for connect/disconnect messages.
	 */
	public void removeMqttConnectionListener(IConnectionListener listener) {
		connectionListeners.remove(listener);
	}
	
	private void notifyConnectionListeners(ConnectionStatus status) {
		SwingUtilities.invokeLater(() -> connectionListeners.forEach(l -> l.onConnectionStatusChanged(status)));
	}
	
	/**
	 * Add a listener for requests to run jobs in the queue.
	 */
	public void addJobQueueRunListener(IRequestJobQueueRunListener listener) {
	    runJobQueueListeners.add(listener);
	}
	
	/**
	 * Remove a listener for requests to run jobs in the queue.
	 */
	public void removeJobQueueRunListener(IRequestJobQueueRunListener listener) {
	    runJobQueueListeners.remove(listener);
	}
	
	private void notifyJobQueueRunListeners() {
	    SwingUtilities.invokeLater(() -> runJobQueueListeners.forEach(l -> l.onJobQueueRun()));
	}
	
	/**
	 * Add a listener for new status messages.
	 */
	public void addMqttMessageReceivedListener(IMqttMessageReceivedListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove a listener for status messages.
	 */
	public void removeMqttMessageReceivedListener(IMqttMessageReceivedListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyMqttMessageReceivedListeners(final ca.wise.lib.mqtt.MqttMessage message) {
		SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.onMqttMessageReceived(message)));
	}
	
	/**
	 * Add a new listener for shutdown requests.
	 */
	public void addShutdownRequestListener(IShutdownRequestListener listener) {
		shutdownListeners.add(listener);
	}
	
	/**
	 * Remove a listener for shutdown requests.
	 */
	public void removeShutdownRequestListener(IShutdownRequestListener listener) {
		shutdownListeners.remove(listener);
	}
	
	private void notifyShutdownRequestListeners() {
		SwingUtilities.invokeLater(() -> shutdownListeners.forEach(l -> l.onShutdownRequested()));
	}
	
	/**
	 * Add a new listener for restart requests.
	 */
	public void addRestartRequestListener(IRestartRequestedListener listener) {
		restartListeners.add(listener);
	}
	
	/**
	 * Remove a listener for restart requests.
	 */
	public void removeRestartRequestListener(IRestartRequestedListener listener) {
		restartListeners.remove(listener);
	}
	
	private void notifyRestartRequestListeners() {
		SwingUtilities.invokeLater(() -> restartListeners.forEach(l -> l.onRestartRequested()));
	}
    
    /**
     * Add a new listener for pause requests.
     */
    public void addPauseRequestListener(IPauseRequestedListener listener) {
        pauseListeners.add(listener);
    }
    
    /**
     * Remove a listener for pause requests.
     */
    public void removePauseRequestListener(IPauseRequestedListener listener) {
        pauseListeners.remove(listener);
    }
    
    private void notifyPauseRequestListeners(IPauseRequestedListener.PauseState pause) {
        //don't allow W.I.S.E. Manager's state to be changed if it is offline
        if (pauseState != IPauseRequestedListener.PauseState.Offline)
            SwingUtilities.invokeLater(() -> pauseListeners.forEach(listener -> listener.onPauseRequested(pause)));
        //only remember the state locally when W.I.S.E. Manager is taken offline
        if (pause == IPauseRequestedListener.PauseState.Offline) {
            pauseState = pause;
            //we want to restart jobs in the queue when manager is restarted
            jobs.markQueueForRestart();
        }
    }
    
    /**
     * Check to see if any of the paused listeners have paused the job queue.
     * @return
     */
    private boolean getIsPausedFromListeners() {
        return pauseListeners.stream()
                .map(listener -> {
                    AtomicBoolean retval = new AtomicBoolean(false);
                    try {
                        SwingUtilities.invokeAndWait(() -> retval.set(listener.isPaused()));
                    }
                    catch (InvocationTargetException|InterruptedException e) { }
                    return retval.get();
                })
                .allMatch(val -> val == true);
    }
	
	/**
	 * Get the unique client ID used to connect to the MQTT broker.
	 * @return The MQTT client ID.
	 */
	public String getMqttId() {
	    if (myId == null) {
	        if (!Strings.isNullOrEmpty(MqttSettings.getMqttId())) {
	            myId = MqttSettings.getMqttId();
	            isStaticId = true;
	        }
	        else
	            myId = generateComputerId();
	    }
		return myId;
	}

    /**
     * Get the QoS to use for guaranteed delivery. If a static MQTT ID is being used
     * messages will be delivered with a QoS of 1 to guarantee that other clients will
     * always receive the intended message. If a dynamic MQTT ID is being used this
     * assumes that the other clients will also be dynamic so no guarantee is required.
     * @return {@code 1} if a static ID is being used, {@code 0} otherwise.
     */
    private int getGuaranteeQos() {
        return isStaticId ? 1 : 0;
    }
    
    /**
     * Add the last will and testament to the connection options.
     */
    private void buildLastWillAndTestament(MqttConnectOptions options) {
        Checkin checkin = new Checkin();
        checkin.nodeId = myId;
        checkin.status = Checkin.Status.ConnectionLoss;
        checkin.version = BuildConfig.version;
        checkin.type = Checkin.Type.Manager;

        try {
            options.setWill(buildTopic(TARGET_ALL, TOPIC_CHECKIN), mapper.writeValueAsBytes(checkin), 0, false);
        }
        catch (JsonProcessingException e) {
            WISELogger.getSpecial(LogName.Backend).error("Failed to build last will and testament", e);
        }
    }
	
	/**
	 * Reconnect to the MQTT broker if we got disconnected.
	 */
	private void reconnect() {
		if (client == null || shuttingDown)
			throw new RuntimeException("Cannort restart MqttListener during shutdown");
		locker.lock();
		try {
			WISELogger.getSpecial(LogName.Backend).debug("Reconnect to broker at " + address + " with ID " + myId);
			MqttConnectOptions options = new MqttConnectOptions();
			if (username.length() > 0)
				options.setUserName(username);
			if (password.length() > 0)
				options.setPassword(password.toCharArray());
			//if using a static ID connect with a persistent session
			if (isStaticId)
			    options.setCleanSession(false);
			buildLastWillAndTestament(options);
			client.connect(options, null, this);
		}
		catch (MqttException e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Failed to connect to MQTT broker.", e);
		}
		finally {
			locker.unlock();
		}
	}
	
	/**
	 * Connect to the MQTT broker.
	 */
	public void connect() {
		if (shuttingDown)
			throw new RuntimeException("Cannot restart MqttListener");
		locker.lock();
		try {
			//start an RPC server
			if (rpcServer == null && !Strings.isNullOrEmpty(rpcAddress) && rpcPort > 0) {
				try {
				    rpcServer = new RPCServer(Settings.getJobDirectory(), rpcAddress, rpcInternalAddress, rpcPort,
					        rpcInternalPort, rpcExternalPort);
                    WISELogger.getSpecial(LogName.Backend).info("Broadcasting internal RPC of " + rpcServer.getFullInternalAddress() + " and external RPC of " + rpcServer.getFullAddress());
					rpcServer.start();
				}
				catch (IOException e) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to start RPC server.", e);
				}
			}
			if (client != null)
				return;
			connStatus = ConnectionStatus.CONNECTING;
			notifyConnectionListeners(connStatus);
            getMqttId();
			WISELogger.getSpecial(LogName.Backend).debug("Connect to broker at " + address + " with ID " + myId);
			MqttClientPersistence persistence;
			if (useMemoryPersistence)
				persistence = new MemoryPersistence();
			else if (persistenceDirectory != null && persistenceDirectory.length() > 0)
				persistence = new MqttDefaultFilePersistence(persistenceDirectory);
			else
				persistence = new MqttDefaultFilePersistence();
			client = new MqttAsyncClient(address, myId, persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			if (username.length() > 0)
				options.setUserName(username);
			if (password.length() > 0)
				options.setPassword(password.toCharArray());
			
			//set the last will and testament in case we loose the connection
			buildLastWillAndTestament(options);
			
			client.setCallback(this);
			client.connect(options, null, this);
		}
		catch (MqttException e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Failed to connect to MQTT broker.", e);
		}
		finally {
			locker.unlock();
		}
	}
	
	/**
	 * Connect to the MQTT broker asynchronously.
	 */
	public void connectAsync() {
		if (shuttingDown)
			throw new RuntimeException("Cannot restart MqttListener");
		executor.execute(this::connect);
	}
	
	/**
	 * Repopulate the load balance list.
	 */
	public void repopulateLoadBalanceList() {
	    //clear the load balance list
	    remoteJobRequestLock.lock();
	    try {
	        balanceJobRequests.clear();
	    }
	    finally {
	        remoteJobRequestLock.unlock();
	    }

        executor.execute(() -> {
            //send a message to all managers asking for their load balance list
            String topic = buildTopic(TARGET_MANAGER, TOPIC_BALANCE_LIST);
            locker.lock();
            try {
                if (client != null && client.isConnected()) {
                    client.publish(topic, new byte[0], 0, false);
                }
            }
            catch (MqttException e) {
                WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance list request.", e);
            }
            finally {
                locker.unlock();
            }
        });
	}
	
	/**
	 * Asynchronously reconnect to the MQTT broker if we got disconnected.
	 */
	private void reconnectAsync() {
		if (client == null || shuttingDown)
			throw new RuntimeException("Cannot restart MqttListener while shutting down.");
		executor.execute(this::reconnect);
	}
	
	private List<JobRequest> getRemoteJobRequestList() {
	    return remoteJobRequests.computeIfAbsent(RemoteType.SERVER_CREATE, key -> new ArrayList<JobRequest>());
	}
	
	private List<JobRequest> getBalanceJobRequestList() {
	    return remoteJobRequests.computeIfAbsent(RemoteType.LOAD_BALANCE, key -> new ArrayList<JobRequest>());
	}
	
	/**
	 * Add a new remote job request to the queue. The queue
	 * will be used so that job related information won't
	 * need to be retrieved again on job start. This will
	 * also clean up jobs that have been in the queue for
	 * more than 15 minutes.
	 * @param request
	 */
	private void addRemoteJob(JobRequest request) {
		remoteJobRequestLock.lock();
		try {
		    List<JobRequest> list = getRemoteJobRequestList();
			request.recieveTime = OffsetDateTime.now(ZoneOffset.UTC);
			OffsetDateTime old = request.recieveTime.minusHours(24);
			list.removeIf(x -> x.recieveTime == null || x.recieveTime.isBefore(old) || x.jobName.equals(request.jobName));
			list.add(request);
		}
		finally {
			remoteJobRequestLock.unlock();
		}
	}
	
	/**
	 * Add a new remote load balance request to the queue.
	 * The queue will be used so that job related information
	 * won't need to be retrieved again on job start. This will
	 * also clean up jobs that have been in the queue for
	 * more than 15 minutes.
	 * @param request
	 */
	private void addRemoteBalanceJob(JobRequest request) {
	    remoteJobRequestLock.lock();
	    try {
	        List<JobRequest> list = getBalanceJobRequestList();
	        request.recieveTime = OffsetDateTime.now(ZoneOffset.UTC);
	        OffsetDateTime old = request.recieveTime.minusMinutes(15);
	        list.removeIf(x -> x.recieveTime == null || x.recieveTime.isBefore(old));
	        list.add(request);
	    }
	    finally {
	        remoteJobRequestLock.unlock();
	    }
	}
	
	/**
	 * Retrieve a remote job from the queue. Don't remove it.
	 * @param name The name of the job to get.
	 * @return An optional that may contain a job with the specified name.
	 */
	private Optional<JobRequest> getRemoteJob(String name) {
		remoteJobRequestLock.lock();
		try {
			return getRemoteJobRequestList().stream().filter(x -> x.jobName.equals(name)).findFirst();
		}
		finally {
			remoteJobRequestLock.unlock();
		}
	}
	
	/**
	 * Retrieve a remote balance job from the queue. Don't remove it.
	 * @param name The name of the job to get.
	 * @return An optional that may contain a job with the specified name.
	 */
	private Optional<JobRequest> getRemoteBalanceJob(String name) {
	    remoteJobRequestLock.lock();
	    try {
	        return getBalanceJobRequestList().stream().filter(x -> x.jobName.equals(name)).findFirst();
	    }
	    finally {
	        remoteJobRequestLock.unlock();
	    }
	}
	
	/**
	 * Check to see if a load balance job is being transferred to this instance already.
	 * @param name The name of the job to check.
	 * @return {@code true} if the job is already being transferred, {@code false} otherwise.
	 */
	private boolean hasRemoteBalanceJob(String name) {
	    remoteJobRequestLock.lock();
	    try {
	        return getBalanceJobRequestList().stream().anyMatch(x -> x.jobName.equals(name));
	    }
	    finally {
	        remoteJobRequestLock.unlock();
	    }
	}
	
	/**
	 * Remove a job from the remote job queue.
	 * @param request The job to remove.
	 */
	private void removeRemoteJob(JobRequest request) {
		remoteJobRequestLock.lock();
		try {
			getRemoteJobRequestList().remove(request);
		}
		finally {
			remoteJobRequestLock.unlock();
		}
	}
	
	/**
	 * An RPC transfer of a load balanced job is complete, remove it from the transferring
	 * list and add it to the known balance job list.
	 * @param remove The job details to remove from the transferring list.
	 * @param add The job details to add to the known balance jobs list.
	 */
	private void remoteBalanceJobComplete(JobRequest remove, JobRequest add) {
        remoteJobRequestLock.lock();
        try {
            getBalanceJobRequestList().remove(remove);
            balanceJobRequests.computeIfAbsent(add.jobName, name -> new BalanceWrapper(add));
        }
        finally {
            remoteJobRequestLock.unlock();
        }
	}
	
	/**
	 * Add a new job to the list of jobs that need distributed for load balancing.
	 * @param jobName The name of the job to add.
	 */
	private void createBalanceJob(JobRequest request) {
	    remoteJobRequestLock.lock();
	    try {
	        balanceJobRequests.computeIfAbsent(request.jobName, name -> new BalanceWrapper(request));
	    }
        finally {
            remoteJobRequestLock.unlock();
        }
	}
    
	/**
	 * Remove a balance job that has timed out on being transferred.
	 * @param jobName The name of the job to remove.
	 */
    private void removeBalanceJob(String jobName) {
        remoteJobRequestLock.lock();
        try {
            balanceJobRequests.remove(jobName);
        }
        finally {
            remoteJobRequestLock.unlock();
        }
    }
	
	/**
	 * Get an ongoing job transfer for a load balancing job.
	 * @param jobName The name of the job that is being load balanced.
	 * @param from The Manager instance that is requesting the file.
	 * @return The job transfer details, or {@link Optional#empty()} if they don't exist.
	 */
	private Optional<JobRequest> getBalanceJobRequest(String jobName, String from) {
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(jobName);
            if (wrapper != null) {
                return wrapper.clients.stream().filter(x -> x.from.equals(from)).findFirst();
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
        return Optional.empty();
	}
	
	/**
	 * Get the local job data from a balance wrapper for a given job.
	 * @param jobName The name of the job to get the job data for.
	 * @return The job data, or empty if the job doesn't exist.
	 */
	private Optional<JobRequest> getBalanceJob(String jobName) {
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(jobName);
            if (wrapper != null)
                return Optional.of(wrapper.request);
        }
        finally {
            remoteJobRequestLock.unlock();
        }
        return Optional.empty();
	}
	
	/**
	 * Remove a job transfer that has completed.
	 * @param jobName The name of the job that is being transferred.
	 * @param from The remote Manager instance that the job was being transferred to.
	 */
	private void removeBalanceJobRequest(String jobName, String from) {
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(jobName);
            if (wrapper != null) {
                Optional<JobRequest> request = wrapper.clients.stream().filter(x -> x.from.equals(from)).findFirst();
                if (request.isPresent())
                    wrapper.clients.remove(request.get());
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
	}
	
	/**
	 * A remote Manager instance has requested a job that needs to
	 * be load balanced.
	 * @param request The remote job request.
	 */
	private void addBalanceJob(JobRequest request, String from) {
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(request.jobName);
            if (wrapper != null && !wrapper.clients.stream().anyMatch(x -> x.from.equals(from))) {
                request.from = from;
                wrapper.clients.add(request);
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
	}

	@Override
	public void close() throws IOException {
		locker.lock();
		try {
			shuttingDown = true;
			if (rpcServer != null) {
				rpcServer.stop();
				rpcServer = null;
			}
			if (client != null && client.isConnected()) {
				Checkin checkin = new Checkin();
				checkin.nodeId = myId;
				checkin.status = Checkin.Status.ShuttingDown;
				checkin.version = BuildConfig.version;
				checkin.type = Checkin.Type.Manager;

				String topic = buildTopic(TARGET_ALL, TOPIC_CHECKIN);
				IMqttDeliveryToken token = client.publish(topic, mapper.writeValueAsBytes(checkin), 0, false);
				token.waitForCompletion();
				
				IMqttToken disToken = client.disconnect();
				disToken.waitForCompletion();
				client.close();
				client = null;
				connStatus = ConnectionStatus.DISCONNECTED;
				notifyConnectionListeners(connStatus);
			}
			executor.shutdown();
			geoServerExecutor.shutdown();
		}
		catch (JsonProcessingException | MqttException e) {
			WISELogger.getSpecial(LogName.Backend).error("Unable to disconnect from MQTT broker.", e);
		}
		finally {
			locker.unlock();
		}
	}

	@Override
	public void onSuccess(IMqttToken asyncActionToken) {
		WISELogger.getSpecial(LogName.Backend).info("MQTT connection opened.");
		locker.lock();
		try {
			//if a connection is open subscribe to the required topics
			if (client != null && client.isConnected()) {
				connStatus = ConnectionStatus.CONNECTED;
				notifyConnectionListeners(connStatus);
				client.subscribe(new String[] {
						topic + "/+/+/status",
						topic + "/+/+/listfiles",
                        topic + "/+/+/shutdown",
						topic + "/+/broadcast/reportin",
                        topic + "/+/" + myId + "/reportin",
						topic + "/+/manager/reportin",
                        topic + "/+/manager/request",
                        topic + "/+/manager/remoterequest",
                        topic + "/+/" + myId + "/start",
                        topic + "/+/" + myId + "/remotestart",
                        topic + "/+/" + myId + "/remotestart/+",
                        topic + "/+/" + myId + "/balancerpcstart",
                        topic + "/+/" + myId + "/file",
                        topic + "/+/" + myId + "/fileresponse",
                        topic + "/+/" + myId + "/balanceavailable",
                        topic + "/+/" + myId + "/balancestart",
                        topic + "/+/" + myId + "/balancestart/+",
                        topic + "/+/" + myId + "/remoterequest",
                        topic + "/+/manager/requesthistory",
                        topic + "/+/" + myId + "/requesthistory",
                        topic + "/+/manager/manage",
                        topic + "/+/" + myId + "/manage",
                        topic + "/+/manager/balancerequest",
                        topic + "/+/" + myId + "/balancerequest",
                        topic + "/+/manager/balancelist",
                        topic + "/+/" + myId + "/balancelist"
				}, new int[] {
						0,//status
						0,//listfiles
                        0,//shutdown
						0,//reportin
						0,//reportin
                        0,//reportin
                        0,//request
                        0,//remoterequest
                        0,//start
                        0,//remotestart
                        0,//remotestart/+
                        0,//balancerpcstart
                        getGuaranteeQos(),//file
                        getGuaranteeQos(),//fileresponse
                        0,//balanceavailable
                        0,//balancestart
                        0,//balancestart/+
                        0,//remoterequest
                        0,//requesthistory
                        0,//requesthistory
                        0,//manage
                        0,//manage
                        0,//balancerequest
                        0,//balancerequest
                        0,//balancelist
                        0//balancelist
				});
				sendCheckin(TARGET_ALL, Checkin.Status.StartingUp, 0);
			}
		}
		catch (MqttException e) {
			e.printStackTrace();
		}
		finally {
			locker.unlock();
		}
	}
	
	private String buildTopic(String to, String message) {
		return new StringBuilder(topic)
				.append("/")
				.append(myId)
				.append("/")
				.append(to)
				.append("/")
				.append(message)
				.toString();
	}
	
	public void sendCheckin(final String to, final Checkin.Status status, int verbosityLevel) {
		executor.execute(() -> {
			locker.lock();
			try {
				if (client != null && client.isConnected()) {
					Checkin checkin = new Checkin();
					checkin.nodeId = myId;
					checkin.status = status;
					checkin.version = BuildConfig.version;
					checkin.type = Checkin.Type.Manager;
					checkin.topic = topic;
					checkin.managerDetails = new Checkin.ManagerDetails();
					if (pauseState == IPauseRequestedListener.PauseState.Offline)
					    checkin.managerDetails.isPaused = 2;
					else
					    checkin.managerDetails.isPaused = getIsPausedFromListeners() ? 0 : 1;
					if (verbosityLevel == 1)
					    checkin.managerDetails.runningJobs = jobs.getRunningJobs();

		            SystemInfo system = new SystemInfo();
		            OperatingSystem os = system.getOperatingSystem();
		            OSProcess process = os.getProcess(os.getProcessId());
		            if (process != null) {
		                checkin.managerDetails.memoryUsage = process.getResidentSetSize();
		                int cpuCount = system.getHardware().getProcessor().getLogicalProcessorCount();
		                if (cpuCount > 0) {
		                    long firstTime = process.getKernelTime() + process.getUserTime();
		                    try {
                                Thread.sleep(1000);
                            }
		                    catch (InterruptedException e) { }
		                    long secondTime = process.getKernelTime() + process.getUserTime();
		                    checkin.managerDetails.cpuUsage = (100d * (secondTime - firstTime) / 1000d) / cpuCount;
		                }
		            }
		            //send back OS information
		            if (verbosityLevel == 2) {
		                checkin.computerDetails = new Checkin.ComputerDetails();
		                checkin.computerDetails.physicalCores = system.getHardware().getProcessor().getPhysicalProcessorCount();
		                checkin.computerDetails.logicalCores = system.getHardware().getProcessor().getLogicalProcessorCount();
		                checkin.computerDetails.totalMemory = system.getHardware().getMemory().getTotal();
		                checkin.computerDetails.operatingSystem = system.getOperatingSystem().toString();
		                checkin.computerDetails.machineName = getHost();
		                checkin.computerDetails.jobFolder = Settings.getJobDirectory();
		                
		                try {
    		                String path = Paths.get(checkin.computerDetails.jobFolder).toRealPath().toString();
    		                if (path != null) {
    		                    for (OSFileStore store : system.getOperatingSystem().getFileSystem().getFileStores()) {
    		                        String mount = store.getMount();
    		                        if (!mount.isEmpty() && path.toString().substring(0, mount.length()).equalsIgnoreCase(mount)) {
    		                            checkin.computerDetails.totalSpace = store.getTotalSpace();
    		                            checkin.computerDetails.usedSpace = checkin.computerDetails.totalSpace - store.getFreeSpace();
    		                            break;
    		                        }
    		                    }
    		                }
		                }
		                catch (IOException e) { }
		                
		                checkin.computerDetails.startTime = ZonedDateTime.ofInstant(Instant
    		                        .ofEpochSecond(system.getOperatingSystem().getSystemBootTime()),
    		                        ZoneId.of("UTC"))
		                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		                try {
    		                List<byte[]> filterList = Arrays.asList(
    		                        //virtual box
    		                        new byte[] { 0x8, 0x0, 0x27 }, new byte[] { 0x0, 0xf, 0x4f }, new byte[] { 0xa, 0x0, 0x27 },
    		                        //vmware
    		                        new byte[] { 0x0, 0x5, 0x69 }, new byte[] { 0x0, 0xc, 0x29 }, new byte[] { 0x0, 0x50, 0x56 },
    		                        //parallels
    		                        new byte[] { 0x0, 0x1c, 0x42 },
    		                        //virtual pc
    		                        new byte[] { 0x0, 0x3, (byte)(int)0xff },
    		                        //virtual iron
    		                        new byte[] { 0x0, 0xf, 0x4b },
    		                        //xen
    		                        new byte[] { 0x0, 0x16, 0x3e },
    		                        //generic invalid
    		                        new byte[] { 0x0, (byte)(int)0xff }
    		                    );
    		                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    		                while (interfaces.hasMoreElements()) {
    		                    NetworkInterface network = interfaces.nextElement();
    		                    byte[] bytes = network.getHardwareAddress();
    		                    if (bytes != null) {
    		                        boolean valid = filterList.stream()
    		                            .noneMatch(x -> {
    		                                int length = Math.min(x.length, bytes.length);
    		                                for (int i = 0; i < length; i++) {
    		                                    if (bytes[i] != x[i])
    		                                        return false;
    		                                }
    		                                return true;
    		                            });
    		                        if (valid) {
    		                            for (InterfaceAddress address : network.getInterfaceAddresses()) {
    		                                if (address.getAddress().getAddress().length == 4 && !address.getAddress().isLoopbackAddress()) {
    		                                    checkin.computerDetails.internalIpAddress = address.getAddress().getHostAddress();
    		                                    break;
    		                                }
    		                            }
    		                            if (!Strings.isNullOrEmpty(checkin.computerDetails.internalIpAddress))
    		                                break;
    		                        }
    		                    }
    		                }
    		                
		                }
		                catch (SocketException e) { }
		                
		                try {
                            if (Strings.isNullOrEmpty(checkin.computerDetails.internalIpAddress))
                                checkin.computerDetails.internalIpAddress = InetAddress.getLocalHost().getHostAddress();
		                }
                        catch (UnknownHostException e) { }
		                
		                try {
    		                URL url = new URL("http://checkip.amazonaws.com");
    		                InputStream stream = url.openStream();
    		                try {
    		                    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
    		                    checkin.computerDetails.externalIpAddress = in.readLine();
    		                }
    		                finally {
    		                    stream.close();
    		                }
		                }
		                catch (IOException e) { }
		            }

					String topic = buildTopic(to, TOPIC_CHECKIN);
					client.publish(topic, mapper.writeValueAsBytes(checkin), 0, false);
				}
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.getSpecial(LogName.Backend).warn("Failed to send checkin.", e);
			}
			finally {
				locker.unlock();
			}
		});
	}
	
	/**
	 * Send a validation report failed if W.I.S.E. couldn't be used to validate an FGM.
	 * @param job The job that was supposed to be validated.
	 */
	public void sendValidationReportFailed(Job job) {
        executor.execute(() -> {
            locker.lock();
            try {
                if (client != null && client.isConnected()) {
                    ValidationReport.Builder builder = ValidationReport.newBuilder()
                            .setSuccess(false)
                            .setValid(false)
                            .setLoadWarnings(StringValue.of("Unable to run W.I.S.E. for validation."));

                    Printer printer = JsonFormat.printer()
                            .includingDefaultValueFields()
                            .omittingInsignificantWhitespace();
                    String json = printer.print(builder);

                    String topic = buildTopic(job.getName(), TOPIC_VALIDATE);
                    client.publish(topic, json.getBytes(StandardCharsets.UTF_8), 0, false);
                }
            }
            catch (InvalidProtocolBufferException | MqttException e) {
                WISELogger.getSpecial(LogName.Backend).warn("Failed to send validation failed.", e);
            }
            finally {
                locker.unlock();
            }
        });
	}
    
    /**
     * Send a validation report failed if W.I.S.E. couldn't be used to validate an FGM.
     * @param job The job that was supposed to be validated.
     */
    public void sendValidationReportSuccess(Job job) {
        executor.execute(() -> {
            locker.lock();
            try {
                if (client != null && client.isConnected()) {
                    Path validFile = Paths.get(Settings.getJobDirectory(), job.getName(), "validation.json");
                    String topic = buildTopic(job.getName(), TOPIC_VALIDATE);
                    
                    //if the validation details file exists
                    if (Files.exists(validFile)) {
                        String json = new String(Files.readAllBytes(validFile), StandardCharsets.UTF_8);
                        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(json);
                        
                        client.publish(topic, mapper.writeValueAsBytes(jsonNode), 0, false);
                    }
                    else {
                        ValidationReport.Builder builder = ValidationReport.newBuilder()
                                .setSuccess(false)
                                .setValid(false)
                                .setLoadWarnings(StringValue.of("Unable to run W.I.S.E. for validation."));

                        Printer printer = JsonFormat.printer()
                                .includingDefaultValueFields()
                                .omittingInsignificantWhitespace();
                        String json = printer.print(builder);

                        client.publish(topic, json.getBytes(StandardCharsets.UTF_8), 0, false);
                    }
                }
            }
            catch (InvalidProtocolBufferException | MqttException e) {
                WISELogger.getSpecial(LogName.Backend).warn("Failed to send validation failed.", e);
            } catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).warn("Unable to read validation file.", e);
            }
            finally {
                locker.unlock();
            }
        });
    }
	
	/**
	 * Send a request to a running job to shutdown.
	 * @param shutdown The details of the job to shutdown.
	 */
	public void sendJobShutdown(Shutdown shutdown) {
		executor.execute(() -> {
			locker.lock();
			try {
				if (shutdown.priority == 1) {
					String topic = buildTopic(shutdown.jobId, TOPIC_SHUTDOWN);
					client.publish(topic, mapper.writeValueAsBytes(shutdown), 0, false);
				}
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.getSpecial(LogName.Backend).warn("Failed to send checkin.", e);
			}
			finally {
				locker.unlock();
			}
		});
	}
	
	/**
	 * Handle a request for the job history.
	 * @param message The message that was received.
	 */
	private void handleJobHistory(final ca.wise.lib.mqtt.MqttMessage message) {
		executor.execute(() -> {
			HistoryStatus filter = HistoryStatus.All;
			int count = -1;
			int offset = -1;
			String responseId = null;
			if (message.message != null && message.message.length() > 0) {
				try {
					JobHistoryRequest request = mapper.readValue(message.message, JobHistoryRequest.class);
					filter = request.filter;
					offset = request.offset;
					count = request.count;
					responseId = request.responseId;
				}
				catch (IOException e) { }
			}
            JobHistoryWrapper retval = new JobHistoryWrapper();
            retval.responseId = responseId;
            retval.jobs = new ArrayList<>();
            if (filter == HistoryStatus.Running || filter == HistoryStatus.QueueRun || filter == HistoryStatus.All) {
                List<JobHistory> histories = jobs.getRunningJobDetailsList();
                retval.jobs.addAll(histories);
            }
			if (filter == HistoryStatus.Queued || filter == HistoryStatus.QueueRun || filter == HistoryStatus.All) {
                List<JobHistory> histories = jobs.getQueuedJobDetailsList();
                retval.jobs.addAll(histories);
			}
            if (filter == HistoryStatus.Complete || filter == HistoryStatus.All) {
                List<JobHistory> histories = jobs.getCompleteJobDetailsList();
                retval.jobs.addAll(histories);
            }
			retval.totalCount = retval.jobs.size();
			retval.filter = filter;
            if (count > 0 && offset >= 0) {
                retval.count = count;
                retval.offset = offset;
                retval.jobs = retval.jobs.stream()
                        .skip(offset)
                        .limit(count)
                        .collect(Collectors.toList());
            }
			
			String topic = buildTopic(message.from, TOPIC_HISTORY);
			locker.lock();
			try {
				if (client != null && client.isConnected()) {
					client.publish(topic, mapper.writeValueAsBytes(retval), 0, false);
				}
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.getSpecial(LogName.Backend).warn("Failed to send checkin.", e);
			}
			finally {
				locker.unlock();
			}
		});
	}
	
	/**
	 * Handle a request to cleanup a completed job.
	 * @param options The cleanup options.
	 */
	private void handleJobManagement(Manage options) {
        executor.execute(() -> {
    	    if (options.request.equals("tar")) {
    	        jobs.tarFinishedJob(options.target);
    	    }
    	    else if (options.request.equals("zip")) {
    	        jobs.zipFinishedJob(options.target);
    	    }
    	    else if (options.request.equals("delete")) {
    	        jobs.deleteFinishedJob(options.target);
    	    }
    	    else if (options.request.equals("rerun")) {
    	        jobs.rerunFinishedJob(options.target, options.deleteOld == null ? true : options.deleteOld);
    	        notifyJobQueueRunListeners();
    	    }
        });
	}
	
	/**
	 * Handle a request to run a new job.
	 * @param message The message that was received.
	 */
	private void handleJobRequest(final ca.wise.lib.mqtt.MqttMessage message) {
		executor.execute(() -> {
			byte[] payload = null;
			JobRequest request = null;
			int transferSize = Settings.getBuilderMaxBufferSize();
			try {
				request = mapper.readValue(message.message, JobRequest.class);
				
				JobResponse.JobResponseBuilder response = JobResponse.builder()
						.jobName(request.jobName)
						.coresAvailable((int)jobs.getFreeCount());
				if (message.type == MessageType.JobRemoteRequest) {
					if (rpcServer != null) {
						response.rpcAddress(rpcServer.getFullAddress());
                        if (rpcServer.hasInternalAddress())
                            response.rpcInternalAddress(rpcServer.getFullInternalAddress());
					}
					response.offset(0)
						.size(transferSize);
				}
				payload = mapper.writeValueAsBytes(response.build());
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Unable to parse job request.", e);
			}
			
			if (payload != null) {
				String topic;
				if (message.type == MessageType.JobRemoteRequest) {
					request.requestOffset = transferSize;
					addRemoteJob(request);
					topic = buildTopic(message.from, TOPIC_REMOTE_AVAILABLE);
				}
				else
					topic = buildTopic(message.from, TOPIC_AVAILABLE);
				locker.lock();
				try {
					if (client != null && client.isConnected()) {
						client.publish(topic, payload, 0, false);
					}
				}
				catch (MqttException e) {
					WISELogger.getSpecial(LogName.Backend).warn("Failed to send remote available.", e);
				}
				finally {
					locker.unlock();
				}
			}
		});
	}
    
    /**
     * Handle a message from another instance of W.I.S.E. Manager that has a new job
     * file that requires load balancing.
     * @param from The ID of the application that sent the message.
     * @param message The String JSON message that contains the job details.
     */
	private void handleBalanceRequest(String from, String message) {
        executor.execute(() -> {
            byte[] payload = null;
            int transferSize = Settings.getBuilderMaxBufferSize();
            remoteJobRequestLock.lock();
            try {
                JobRequest request = mapper.readValue(message, JobRequest.class);

                boolean haveRequest =  balanceJobRequests.containsKey(request.jobName);

                //skip this request if we are the Manager that is distributing it
                if (!haveRequest) {
                    int exists = jobs.containsJob(request.jobName);
                    //the job is already running or queued
                    if (exists > 0) {
                        createBalanceJob(request);
                    }
                    //the job isn't yet known
                    else if (exists == 0) {
                        //check to see if the file is being transferred
                        if (!hasRemoteBalanceJob(request.jobName)) {
                            JobResponse.JobResponseBuilder response = JobResponse.builder()
                                    .jobName(request.jobName)
                                    .coresAvailable((int)jobs.getFreeCount());
                            if (rpcServer != null) {
                                response.rpcAddress(rpcServer.getFullAddress());
                                if (rpcServer.hasInternalAddress())
                                    response.rpcInternalAddress(rpcServer.getFullInternalAddress());
                            }
                            response.offset(0)
                                .size(transferSize);
                            payload = mapper.writeValueAsBytes(response.build());
                            request.requestOffset = transferSize;
                            addRemoteBalanceJob(request);
                        }
                    }
                }
            }
            catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).error("Unable to parse balance request.", e);
            }
            finally {
                remoteJobRequestLock.unlock();
            }
            
            if (payload != null) {
                String topic;
                topic = buildTopic(from, TOPIC_BALANCE_AVAILABLE);
                locker.lock();
                try {
                    if (client != null && client.isConnected()) {
                        client.publish(topic, payload, 0, false);
                    }
                }
                catch (MqttException e) {
                    WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance available.", e);
                }
                finally {
                    locker.unlock();
                }
            }
        });
    }
    
    /**
     * A W.I.S.E. Manager instance has requested a job that is to be load balanced.
     * @param message The message contents.
     */
    private void handleBalanceAvailable(final ca.wise.lib.mqtt.MqttMessage message) {
        executor.execute(() -> {
            Optional<JobRequest> request = null;
            JobResponse response = null;
            
            try {
                response = mapper.readValue(message.message, JobResponse.class);
                
                if (response != null) {
                    request = getBalanceJobRequest(response.jobName, message.from);
                    if (!request.isPresent()) {
                        Optional<JobRequest> temp = getBalanceJob(response.jobName);
                        if (temp.isPresent()) {
                            request = Optional.of(new JobRequest(temp.get()));
                            request.get().jobFile = Paths.get(Settings.getJobDirectory(), response.jobName, "job." + request.get().extension);
                            addBalanceJob(request.get(), message.from);
                        }
                    }
                    
                    //the remote manager transfer is available
                    if (request.isPresent()) {
                        //the data can be transferred by RPC
                        if (!Strings.isNullOrEmpty(response.rpcAddress)) {
                            request.get().client = new RPCClient(this, response, request.get(), message.from);
                            startRpcJobs();
                        }
                        //done transferring the job
                        else if (response.size < 0) {
                            removeBalanceJobRequest(response.jobName, message.from);
                        }
                        //send the data over MQTT
                        else {
                            String topic = buildTopic(message.from, TOPIC_BALANCE_START + "/" + response.jobName);
                            int to = response.offset + response.size;
                            if (to > request.get().fileSize)
                                to = request.get().fileSize;
                            
                            if (Files.exists(request.get().jobFile)) {
                                byte[] buffer = new byte[to - response.offset];
                                try (RandomAccessFile file = new RandomAccessFile(request.get().jobFile.toFile(), "r")) {
                                    file.read(buffer, response.offset, buffer.length);
                                }
                                
                                locker.lock();
                                try {
                                    if (client != null && client.isConnected()) {
                                        client.publish(topic, buffer, 0, false);
                                    }
                                }
                                catch (MqttException e) {
                                    WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance data.", e);
                                }
                                finally {
                                    locker.unlock();
                                }
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).error("Unable to parse balance request.", e);
            }
        });
    }
    
    /**
     * Attempted to send a job to manager using RPC but it failed,
     * fall back to sending it via MQTT.
     * @param job The name of the job that failed.
     */
    public void onRPCSendError(String job, String managerId, JobResponse response) {
        Optional<JobRequest> request = Optional.empty();
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(job);
            if (wrapper != null) {
                request = wrapper.clients.stream().filter(x -> x.from.equals(managerId)).findFirst();
                
                if (request.isPresent()) {
                    request.get().client = null;
                    
                    if (response.size < 0) {
                        wrapper.clients.remove(request.get());
                    }
                    //send the data over MQTT
                    else {
                        String topic = buildTopic(managerId, TOPIC_BALANCE_START + "/" + response.jobName);
                        int to = response.offset + response.size;
                        if (to > request.get().fileSize)
                            to = request.get().fileSize;
                        
                        if (Files.exists(request.get().jobFile)) {
                            byte[] buffer = new byte[to - response.offset];
                            try (RandomAccessFile file = new RandomAccessFile(request.get().jobFile.toFile(), "r")) {
                                file.read(buffer, response.offset, buffer.length);
                            }
                            catch (IOException e) {
                                WISELogger.getSpecial(LogName.Backend).warn("Failed to read file data.", e);
                            }
                            
                            locker.lock();
                            try {
                                if (client != null && client.isConnected()) {
                                    client.publish(topic, buffer, 0, false);
                                }
                            }
                            catch (MqttException e) {
                                WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance data.", e);
                            }
                            finally {
                                locker.unlock();
                            }
                        }
                    }
                }
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
        startRpcJobs();
    }
    
    /**
     * Sent a job to manager using RPC, remove the job from the queue
     * and send the start signal.
     * @param job The name of the job that successfully sent.
     */
    public void onRPCSendComplete(String job, String managerId) throws Exception {
        Optional<JobRequest> request = Optional.empty();
        remoteJobRequestLock.lock();
        try {
            BalanceWrapper wrapper = balanceJobRequests.get(job);
            
            if (wrapper != null) {
                request = wrapper.clients.stream().filter(x -> x.from.equals(managerId)).findFirst();
                
                if (request.isPresent()) {
                    wrapper.clients.remove(request.get());
                    request.get().client = null;
                    locker.lock();
                    try {
                        String topic = buildTopic(managerId, TOPIC_BALANCE_RPC_START);

                        if (client != null && client.isConnected()) {
                            client.publish(topic, mapper.writeValueAsBytes(request.get()), 0, false);
                        }
                    }
                    catch (MqttException e) {
                        WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance start.", e);
                    }
                    finally {
                        locker.unlock();
                    }
                }
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
        startRpcJobs();
    }

    /**
     * Start some RPC jobs running. Cap the total amount of transfers allowed.
     */
    private void startRpcJobs() {
        remoteJobRequestLock.lock();
        try {
            long currentCount = balanceJobRequests.values().stream()
                    .flatMap(x -> x.clients.stream())
                    .filter(x -> x.client != null && x.client.isRunning())
                    .count();
            if (currentCount == 0) {
                Optional<JobRequest> first = balanceJobRequests.values().stream()
                        .flatMap(x -> x.clients.stream())
                        .filter(x -> x.client != null && !x.client.isRunning())
                        .findFirst();
                first.ifPresent(job -> job.client.runAsync());
            }
        }
        finally {
            remoteJobRequestLock.unlock();
        }
    }
	
	/**
	 * Handle a message to start executing a new job.
	 * @param message The message that was received.
	 */
	private void handleJobStartRequest(final ca.wise.lib.mqtt.MqttMessage message) {
		executor.execute(() -> {
			try {
				JobRequest request = null;
                String jobDir = Settings.getJobDirectory();
                
                //a job is being transferred over MQTT
				if (message.type == MessageType.JobRemoteStart) {
					Optional<JobRequest> opt = getRemoteJob(message.remoteJobName);
					int transferSize = Settings.getBuilderMaxBufferSize();
					if (opt.isPresent()) {
						Path path = Paths.get(jobDir, opt.get().jobName);
						Files.createDirectories(path);
						String filename = "job." + opt.get().extension;
						path = path.resolve(filename);
						//this if the first set of data, delete the file if it already exists
						if (opt.get().requestOffset == transferSize && Files.exists(path)) {
							try {
								Files.delete(path);
							}
							catch (IOException e) {
							}
						}
						WISELogger.getSpecial(LogName.Backend).info("Found " + message.payload.length + " bytes in the payload");
						try (FileOutputStream stream = new FileOutputStream(path.toFile(), true)) {
							stream.write(message.payload);
						}
						
						//if the entire file has been transfered
						if (opt.get().requestOffset >= opt.get().fileSize) {
							removeRemoteJob(opt.get());
							request = opt.get();
						}
						//request more of the file
						else {
							byte[] payload = null;
							try {
								JobResponse.JobResponseBuilder response = JobResponse.builder()
										.jobName(opt.get().jobName)
										.coresAvailable((int)jobs.getFreeCount())
										.offset(opt.get().requestOffset)
										.size(transferSize);
								payload = mapper.writeValueAsBytes(response.build());
							}
							catch (IOException e) {
								WISELogger.getSpecial(LogName.Backend).error("Unable to parse job request.", e);
							}
							opt.get().requestOffset += transferSize;

							if (payload != null) {
								String topic = buildTopic(message.from, TOPIC_REMOTE_AVAILABLE);
								locker.lock();
								try {
									if (client != null && client.isConnected()) {
										client.publish(topic, payload, 0, false);
									}
								}
								catch (MqttException e) {
									WISELogger.getSpecial(LogName.Backend).warn("Failed to send message.", e);
								}
								finally {
									locker.unlock();
								}
							}
							
							request = null;
						}
					}
				}
				else {
                    request = mapper.readValue(message.message, JobRequest.class);
					Optional<JobRequest> opt = getRemoteJob(message.remoteJobName);
					if (!opt.isPresent()) {
					    opt = getRemoteBalanceJob(message.remoteJobName);
					    if (opt.isPresent())
					        remoteBalanceJobComplete(opt.get(), request);
					}
					else if (opt.isPresent())
						removeRemoteJob(opt.get());
				}
				
				//add the job to the job queue so that it can be run
				if (request != null) {
                    Path path = Paths.get(jobDir, request.jobName, "job." + request.extension);
                    
                    //check for file transfer errors
				    if (message.type == MessageType.BalanceRpcStart) {
				        //verify the transferred file size
				        if (request.fileSize > 0) {
				            try {
				                long size = Files.size(path);
				                //there was an error transferring the file
				                if (size != request.fileSize) {
				                    //delete the broken file
				                    Files.delete(path);
				                    //retry the transfer
				                    handleBalanceRequest(message.from, message.message);
				                    //don't start this job
				                    request = null;
				                }
				            }
				            catch (IOException e) {
				                //I guess I'll just let it fail if this happens
				            }
				        }
				    }
                    //don't check if load balancing is required if this was sent using the load balancing mechanism
				    else {
    					//check to see if load balancing is enabled on the FGM
                        if (Files.exists(path) && !balanceJobRequests.containsKey(request.jobName)) {
                            LoadBalance balance = FGMHelper.getLoadBalancing(path);
                            if (balance == LoadBalance.EXTERNAL_COUNTER) {
                                createBalanceJob(request);
                                byte[] payload = mapper.writeValueAsBytes(request);
                                
                                //send a message to other manager instances that the job is available
                                String topic = buildTopic(TARGET_MANAGER, TOPIC_BALANCE_REQUEST);
                                locker.lock();
                                try {
                                    if (client != null && client.isConnected()) {
                                        client.publish(topic, payload, 0, false);
                                    }
                                }
                                catch (MqttException e) {
                                    WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance request.", e);
                                }
                                finally {
                                    locker.unlock();
                                }
                            }
                        }
					}

				    if (newJobListener != null && request != null)
				        newJobListener.onNewJob(new JobStartDetails(request.jobName, request.cores,
				                request.priority, request.validationState));
				}
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Unable to parse job request.", e);
			}
		});
	}
    
    /**
     * Handle a chunk of a job file being transferred to this instance for load balancing.
     * @param message The received message.
     */
    private void handleBalanceStart(final ca.wise.lib.mqtt.MqttMessage message) {
        executor.execute(() -> {
            try {
                JobRequest request = null;
                String jobDir = Settings.getJobDirectory();
                Optional<JobRequest> opt = getBalanceJobRequest(message.remoteJobName, message.from);
                int transferSize = Settings.getBuilderMaxBufferSize();
                
                if (opt.isPresent()) {
                    Path path = Paths.get(jobDir, opt.get().jobName);
                    Files.createDirectories(path);
                    String filename = "job." + opt.get().extension;
                    path = path.resolve(filename);
                    //this if the first set of data, delete the file if it already exists
                    if (opt.get().requestOffset == transferSize && Files.exists(path)) {
                        try {
                            Files.delete(path);
                        }
                        catch (IOException e) {
                        }
                    }
                    try (FileOutputStream stream = new FileOutputStream(path.toFile(), true)) {
                        stream.write(message.payload);
                    }
                    
                    //if the entire file has been transfered
                    if (opt.get().requestOffset >= opt.get().fileSize) {
                        removeBalanceJobRequest(opt.get().jobName, opt.get().from);
                        request = opt.get();
                    }
                    //request more of the file
                    else {
                        byte[] payload = null;
                        try {
                            JobResponse.JobResponseBuilder response = JobResponse.builder()
                                    .jobName(opt.get().jobName)
                                    .coresAvailable((int)jobs.getFreeCount())
                                    .offset(opt.get().requestOffset)
                                    .size(transferSize);
                            payload = mapper.writeValueAsBytes(response.build());
                        }
                        catch (IOException e) {
                            WISELogger.getSpecial(LogName.Backend).error("Unable to parse balance request.", e);
                        }
                        opt.get().requestOffset += transferSize;

                        if (payload != null) {
                            String topic = buildTopic(message.from, TOPIC_BALANCE_AVAILABLE);
                            locker.lock();
                            try {
                                if (client != null && client.isConnected()) {
                                    client.publish(topic, payload, 0, false);
                                }
                            }
                            catch (MqttException e) {
                                WISELogger.getSpecial(LogName.Backend).warn("Failed to send message.", e);
                            }
                            finally {
                                locker.unlock();
                            }
                        }
                        
                        request = null;
                    }
                }
                
                //add the job to the job queue so that it can be run
                if (request != null) {
                    //add the job to the list of balance jobs so it can be sent out later if requested
                    createBalanceJob(request);
                    //send the job to the queue
                    if (newJobListener != null) {
                        if (request.validationState == JobRequest.VALIDATE_NONE)
                            newJobListener.onNewJob(new JobStartDetails(request.jobName, request.cores,
                                    request.priority, JobRequest.VALIDATE_NONE));
                        else
                            newJobListener.onNewJob(new JobStartDetails(request.jobName, request.cores,
                                    request.priority, JobRequest.VALIDATE_COMPLETE));
                    }
                }
            }
            catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).error("Unable to parse job request.", e);
            }
        });
    }
	
	/**
	 * Handle a message to start executing a new job.
	 * @param message The message that was received.
	 */
	private void handleJobKillRequest(final ca.wise.lib.mqtt.MqttMessage message) {
		executor.execute(() -> {
			try {
				Shutdown request = mapper.readValue(message.message, Shutdown.class);
				//if the target is all managers or this instance the job name has to come from the payload
				if (message.job.equals("manager") || message.job.equals(myId)) {
	                WISELogger.getSpecial(LogName.Backend).warn("Found shutdown for " + request.jobId);
				    //return if the job name wasn't specified
				    if (Strings.isNullOrEmpty(request.jobId))
				        return;
				}
				else
                    request.jobId = message.job;
				jobs.terminateJob(request, this, pauseState == IPauseRequestedListener.PauseState.Offline);
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Unable to parse job request.", e);
			}
		});
	}
	
	/**
	 * Emit, through MQTT, that a file has been created that has been requested
	 * to be streamed using MQTT.
	 * @param job The job that created the file.
	 * @param filename The name and subpath of the file.
	 * @param exists Does the file actually exist, or was the output skipped because it would have been empty.
	 */
	private void emitFileStreamRequest(String job, String filename, boolean exists) {
		List<byte[]> payloads = new ArrayList<>();
		
		try {
			Path jobPath = Paths.get(Settings.getJobDirectory(), job);
			Path path = Paths.get(Settings.getJobDirectory(), job, filename);
			if (!exists || Files.exists(path)) {
    			Printer printer = JsonFormat.printer()
                        .includingDefaultValueFields()
    					.omittingInsignificantWhitespace();
    			
				ca.wise.comms.client.proto.FileStreamRequest.Builder builder = ca.wise.comms.client.proto.FileStreamRequest.newBuilder()
						.setFilename(filename)
						.setJob(job)
						.setFileSize(exists ? Files.size(path) : 0)
						.setWasSkipped(!exists);
				String json = printer.print(builder);
				payloads.add(json.getBytes(StandardCharsets.UTF_8));
				
				//look for files with the same name but different extensions for multi-part files
				String extensionless = com.google.common.io.Files.getNameWithoutExtension(path.toString());
				try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path.getParent(), extensionless + "*")) {
					dirStream.forEach(x -> {
						try {
							if (!Files.isSameFile(x, path)) {
								//get the path of the file relative to the jobs folder
								Path file = jobPath.relativize(x);
								
								ca.wise.comms.client.proto.FileStreamRequest.Builder builder2 = ca.wise.comms.client.proto.FileStreamRequest.newBuilder()
										.setFilename(file.toString())
										.setJob(job)
										.setFileSize(Files.size(x));
								String json2 = printer.print(builder2);
								payloads.add(json2.getBytes(StandardCharsets.UTF_8));
							}
						}
						catch (IOException e) { }
					});
				}
			}
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).error("Failed to get file information to create a stream request.", e);
		}

		//if files were found send an MQTT message for each of them
		if (payloads.size() > 0) {
			String topic = buildTopic(job, TOPIC_FILE_STREAM_AVAILABLE);
			locker.lock();
			try {
				if (client != null && client.isConnected()) {
					for (byte[] payload : payloads) {
						client.publish(topic, payload, getGuaranteeQos(), false);
					}
				}
			}
			catch (MqttException e) {
				WISELogger.getSpecial(LogName.Backend).warn("Failed to send message.", e);
			}
			finally {
				locker.unlock();
			}
		}
	}
	
	/**
	 * Uploads a file to a GeoServer instance. This call blocks until complete.
	 * @param job The name of the job that output the file.
	 * @param filename The file to upload to GeoServer.
	 * @param geoserver Details of the GeoServer instance to upload to.
	 */
	private void startGeoserverUpload(String job, String filename, FileType type, GeoserverUpload geoserver) {
	    //only allow a single file to be uploaded at a time
	    geoserverLock.lock();
		try {
			Path path = Paths.get(Settings.getJobDirectory(), job, filename);
			
			if (Files.exists(path)) {
				if (!Strings.isNullOrEmpty(geoserver.getWorkspace())) {
					Workspaces workspaces = Workspaces.builder()
							.baseUrl(geoserver.getUrl())
			    			.username(geoserver.getUsername())
			    			.password(geoserver.getPassword())
			    			.build();
			    	WorkspaceList list = workspaces.getWorkspaces();
			    	boolean workspaceExists = false;
			    	//check to see if the workspace already exists
			    	if (list != null && list.getWorkspaceList() != null) {
			    		workspaceExists = list.getWorkspaceList().stream().anyMatch(x -> x.getName().equals(geoserver.getWorkspace()));
			    	}
			    	
			    	//create a new workspace if necessary
			    	if (!workspaceExists) {
			    		workspaces.createWorkspace(geoserver.getWorkspace());
			    	}
			    	
			    	CoverageStores coverageStores = workspaces.getCoverageStoresClient(geoserver.getWorkspace());
                    
                    //the full store name to use
                    String store;
                    if (type == FileType.Grid && geoserver.hasRasterCoverage() && !Strings.isNullOrEmpty(geoserver.getRasterCoverage().getValue()))
                        store = geoserver.getRasterCoverage().getValue();
                    else if (type == FileType.Vector && geoserver.hasVectorCoverage() && !Strings.isNullOrEmpty(geoserver.getVectorCoverage().getValue()))
                        store = geoserver.getVectorCoverage().getValue();
                    else if (!Strings.isNullOrEmpty(geoserver.getCoverage()))
                        store = geoserver.getCoverage() + com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString());
                    else
                        store = com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString());

                    if (type == FileType.Grid) {
    			    	CoverageStoreList stores = coverageStores.getCoverageStores();
    			    	
    			    	boolean coverageExists = false;
    			    	//check to see if the coverage store already exists
    			    	if (stores != null && stores.getCoverageStoreList() != null) {
    			    		coverageExists = stores.getCoverageStoreList().stream().anyMatch(x -> x.getName().equals(store));
    			    	}
    			    	
    			    	//create a new coverage store if necessary
    			    	if (!coverageExists) {
    			    		coverageStores.createCoverageStore(store);
    			    	}
    			    	
    			    	//upload the file to the GeoServer instance
    			    	String coverageName = coverageStores.uploadFileToCoverageStore(store, path);
    			    	
    			    	//if the coverage store is new, enable it
    			    	if (!coverageExists) {
    			    		coverageStores.updateCoverageStore(store, true);
    			    	}
    			    	
    			    	//if the workspace is new, enable it
    			    	if (!workspaceExists) {
    			    		workspaces.updateWorkspace(geoserver.getWorkspace(), true);
    			    	}
    			    	
    			    	//if the coverage was uploaded and there is a source SRS specified, enable the coverage layer
    			    	if (!Strings.isNullOrEmpty(coverageName) && !Strings.isNullOrEmpty(geoserver.getSrs())) {
    			    		coverageStores.updateCoverage(store, coverageName, true, geoserver.getSrs());
    			    	}
                    }
                    else if (type == FileType.Vector) {
                        //upload the file to the GeoServer instance
                        coverageStores.uploadFileToDataStore(store, path);
                    }
				}
				else {
					WISELogger.getSpecial(LogName.Backend).error("Missing workspace and coverage store details");
				}
			}
			else {
				WISELogger.getSpecial(LogName.Backend).error("The file to upload to GeoServer doesn't exist");
			}
		}
		catch (Exception e) {
			WISELogger.getSpecial(LogName.Backend).error("Failed to upload file to GeoServer.", e);
		}
		finally {
		    geoserverLock.unlock();
		}
	}
	
	/**
	 * Handle W.I.S.E. notifying that a file was output that requires streaming.
	 * @param message The details of the requested stream.
	 */
	private void handleFileOutput(final ca.wise.lib.mqtt.MqttMessage message) {
		if (!Strings.isNullOrEmpty(message.message)) {
			geoServerExecutor.execute(() -> {
				try {
					ca.wise.comms.server.proto.FileUpload.Builder builder = ca.wise.comms.server.proto.FileUpload.newBuilder();
					JsonFormat.parser()
						.ignoringUnknownFields()
						.merge(message.message, builder);
					ca.wise.comms.server.proto.FileUpload upload = builder.build();
					
					boolean mqttPresent = upload.getUploadTypeList().stream().anyMatch(x -> x.hasMqtt());
					
					for (ca.wise.comms.server.proto.FileUpload.UploadType type : upload.getUploadTypeList()) {
						if (type.hasGeoserver()) {
							startGeoserverUpload(upload.getJob(), upload.getFilename(), upload.getFiletype(), type.getGeoserver());
						}
					}
					
					if (mqttPresent) {
                        emitFileStreamRequest(upload.getJob(), upload.getFilename(), upload.getExists());
					}
				}
				catch (IOException e) {
					WISELogger.getSpecial(LogName.Backend).error("Unable to parse file stream request.", e);
				}
			});
		}
	}
	
	/**
	 * A file stream or upload request has completed, emit a status event.
	 * @param job The name of the job that created the file.
	 * @param filename The name of the file that has been streamed/uploaded.
	 * @param method A single word name describing the method that was used to stream/upload the file.
	 * @throws InvalidProtocolBufferException If the protobuf definition is unable to be printed as JSON.
	 */
	private void fileStreamUploadComplete(String job, String filename, String method) throws InvalidProtocolBufferException {
		String topic = buildTopic(job, TOPIC_STATUS);
		
		ca.wise.fgm.proto.Status.Builder builder = ca.wise.fgm.proto.Status.newBuilder()
				.setStatus(10)
				.setMessage("Streamed " + filename + " to " + method);
		String json = JsonFormat.printer()
				.includingDefaultValueFields()
				.print(builder);
		byte[] payload = json.getBytes(StandardCharsets.UTF_8);
		
		locker.lock();
		try {
			if (client != null && client.isConnected()) {
				client.publish(topic, payload, 0, false);
			}
		}
		catch (MqttException e) {
			WISELogger.getSpecial(LogName.Backend).warn("Failed to send message.", e);
		}
		finally {
			locker.unlock();
		}
	}
	
	/**
	 * Someone has responded to a filerequest message and would like a file streamed
	 * over MQTT.
	 * @param message The details of the file request.
	 */
	private void handleFileStreamRequest(final ca.wise.lib.mqtt.MqttMessage message) {
        if (!Strings.isNullOrEmpty(message.message)) {
            try {
                ca.wise.comms.client.proto.FileStreamResponse.Builder builder = ca.wise.comms.client.proto.FileStreamResponse.newBuilder();
                JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(message.message, builder);
                
                handleFileStreamRequest(message.from, builder.build());
            }
            catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).error("Unable to parse file response.", e);
            }
        }
	}
	
	/**
	 * Handle a request to list the files that have been output by a known job.
	 * @param from The requestor.
	 * @param job The job to list the files for.
	 */
	public void handleJobFileListRequest(final String from, final String job) {
        executor.execute(() -> {
            //if we have this job
            if (jobs.containsJob(job) != 0) {
                FileList retval = new FileList(job);
                Path path = Paths.get(Settings.getJobDirectory(), job);
                Path outPath = path.resolve("Outputs");
                //if the job output folder exists
                if (Files.exists(outPath)) {
                    try {
                        Files.walk(outPath)
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                if (!file.getFileName().toString().endsWith(".gz")) {
                                    String filename = path.relativize(file).toString();
                                    retval.files.add(filename);
                                }
                            });
                    }
                    catch (IOException e) {
                        WISELogger.getSpecial(LogName.Backend).error("Failed to list output files", e);
                    }
                }
                //return the file list
                String topic = buildTopic(from, "filelist");
                try {
                    if (client != null && client.isConnected())
                        client.publish(topic, mapper.writeValueAsBytes(retval), 0, false);
                }
                catch (JsonProcessingException | MqttException e) {
                    WISELogger.getSpecial(LogName.Backend).error("Failed to return output files", e);
                }
            }
        });
	}
	
	/**
	 * Someone has responded to a filerequest message and would like a file streamed
	 * over MQTT.
	 * @param from The remote machine that has requested the file.
	 * @param response The details of the file request.
	 */
	public void handleFileStreamRequest(final String from, final ca.wise.comms.client.proto.FileStreamResponse response) {
		executor.execute(() -> {
		    try {
				if (!Strings.isNullOrEmpty(response.getJob()) && !Strings.isNullOrEmpty(response.getFilename())) {
					//don't allow the user to do anything sketchy and request files outside the job directory
					if (response.getJob().indexOf("..") < 0 && response.getFilename().indexOf("..") < 0) {
						Path path = Paths.get(Settings.getJobDirectory(), response.getJob(), response.getFilename());
						if (Files.exists(path)) {
						    //the file can be transferred using gRPC
						    if (response.hasRpcAddress()) {
						        GenericRPCClient client = new GenericRPCClient(this, response, from, path);
						        client.runAsync();
						    }
						    //the file has to go over MQTT
						    else {
                                Path p;
                                boolean compress = response.hasCompressStream() && response.getCompressStream().getValue();
                                if (compress) {
                                    p = path.getParent().resolve(path.getFileName().toString() + ".gz");
                                    if (!Files.exists(p)) {
                                        try (CompressorOutputStream o = new CompressorStreamFactory()
                                                .createCompressorOutputStream(CompressorStreamFactory.GZIP, Files.newOutputStream(p))) {
                                            try (InputStream in = Files.newInputStream(path)) {
                                                IOUtils.copy(in, o);
                                            }
                                        }
                                        catch (CompressorException e) {
                                            WISELogger.getSpecial(LogName.Backend).error("Failed to compress output file", e);
                                            compress = false;
                                            p = path;
                                        }
                                    }
                                }
                                else
                                    p = path;
								long offset = response.getOffset();
								if (offset < 0)
									offset = 0;
								long size = Files.size(p);
								int bufferSize = Settings.getBuilderMaxBufferSize();
								long remaining = size - offset;
								boolean finished = (offset + bufferSize) >= size;
								if (bufferSize > remaining)
								    bufferSize = (int)remaining;
								ca.wise.comms.client.proto.FileStream.Builder streamBuilder = ca.wise.comms.client.proto.FileStream.newBuilder()
										.setJob(response.getJob())
										.setFilename(response.getFilename())
										.setFileSize(size)
										.setIsEndOfFile(finished);
								if (response.hasCompressStream()) {
								    streamBuilder.setCompressStream(BoolValue.of(compress));
								}
								ByteString str;
								if (offset < size) {
									//read part of the file into a byte array
									try (RandomAccessFile raf = new RandomAccessFile(p.toFile(), "r")) {
										byte[] bytes = new byte[bufferSize];
										raf.seek(offset);
										raf.readFully(bytes);
										str = ByteString.copyFrom(bytes);
									}
								}
								else {
									str = ByteString.EMPTY;
								}
								streamBuilder.setData(str);
								String json2 = JsonFormat.printer()
				    					.omittingInsignificantWhitespace()
				    					.includingDefaultValueFields()
				    					.print(streamBuilder);
								byte[] payload = json2.getBytes(StandardCharsets.UTF_8);

								//if files were found send an MQTT message for each of them
								if (payload != null) {
									String topic = buildTopic(from, TOPIC_FILE_STREAM_AVAILABLE);
									locker.lock();
									try {
										if (client != null && client.isConnected()) {
											client.publish(topic, payload, 0, false);
										}
									}
									catch (MqttException e) {
										WISELogger.getSpecial(LogName.Backend).warn("Failed to send message.", e);
									}
									finally {
										locker.unlock();
									}
									
									//emit a status message saying that the file has been streamed
									if (finished)
										fileStreamUploadComplete(response.getJob(), response.getFilename(), "MQTT");
								}
						    }
						}
					}
				}
            }
            catch (IOException e) {
                WISELogger.getSpecial(LogName.Backend).error("Unable to return file over MQTT.", e);
            }
		});
	}
	
	/**
	 * Send the list of load balancing jobs we are currently managing so that a new instance of
	 * W.I.S.E. Manager can also run them.
	 * @param message The received message.
	 */
	private void handleListLoadBalance(final ca.wise.lib.mqtt.MqttMessage message) {
        executor.execute(() -> {
            remoteJobRequestLock.lock();
            try {
                for (BalanceWrapper wrapper : balanceJobRequests.values()) {
                    byte[] payload = mapper.writeValueAsBytes(wrapper.request);
                    
                    //send a message to the requesting manager instance that the job is available
                    String topic = buildTopic(message.from, TOPIC_BALANCE_REQUEST);
                    locker.lock();
                    try {
                        if (client != null && client.isConnected()) {
                            client.publish(topic, payload, 0, false);
                        }
                    }
                    catch (MqttException e) {
                        WISELogger.getSpecial(LogName.Backend).warn("Failed to send balance request.", e);
                    }
                    finally {
                        locker.unlock();
                    }
                }
            }
            catch (JsonProcessingException e1) {
                WISELogger.getSpecial(LogName.Backend).warn("Failed to send load balance list", e1);
            }
            finally {
                remoteJobRequestLock.unlock();
            }
        });
	}

	@Override
	public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
		if (!shuttingDown) {
			WISELogger.getSpecial(LogName.Backend).error("MQTT failure.", exception);
			connStatus = ConnectionStatus.FAILED;
			notifyConnectionListeners(connStatus);
			reconnectAsync();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		if (!shuttingDown) {
			connStatus = ConnectionStatus.FAILED;
			notifyConnectionListeners(connStatus);
			WISELogger.getSpecial(LogName.Backend).error("MQTT connection lost.", cause);
			reconnectAsync();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		ca.wise.lib.mqtt.MqttMessage libMsg = new ca.wise.lib.mqtt.MqttMessage(topic, message.getPayload());
		if (!topic.endsWith("status"))
		    WISELogger.getSpecial(LogName.Backend).debug("Recieved a message at " + topic);
		if (GlobalConfiguration.debug)
			notifyMqttMessageReceivedListeners(libMsg);
		
		switch (libMsg.type) {
		case Status:
            if (!GlobalConfiguration.debug)
                notifyMqttMessageReceivedListeners(libMsg);
		    break;
		case Shutdown:
            handleJobKillRequest(libMsg);
		    break;
		case JobRequest:
		case JobRemoteRequest:
            //an instance of W.I.S.E. Builder is trying to start a new job, potentially from a remote computer
            if ((libMsg.job.equals(TARGET_MANAGER) || libMsg.job.equals(myId)) && pauseState != IPauseRequestedListener.PauseState.Offline) {
                handleJobRequest(libMsg);
            }
            break;
		case JobStart:
		case JobRemoteStart:
		case BalanceRpcStart:
            //there is a new job to run
            if (libMsg.job.equals(myId)) {
                handleJobStartRequest(libMsg);
            }
		    break;
		case JobHistory:
            //list the job history
            if (libMsg.job.equals(myId) || libMsg.job.equals(TARGET_MANAGER)) {
                handleJobHistory(libMsg);
            }
		    break;
		case Manage:
            //TODO some kind of authentication on these commands
            if (libMsg.job.equals(myId) || libMsg.job.equals(TARGET_MANAGER)) {
                try {
                    Manage request = mapper.readValue(libMsg.message, Manage.class);
                    if (request.request.equals("shutdown"))
                        notifyShutdownRequestListeners();
                    else if (request.request.equals("reboot"))
                        notifyRestartRequestListeners();
                    else if (request.request.equals("pause"))
                        notifyPauseRequestListeners(IPauseRequestedListener.PauseState.Paused);
                    else if (request.request.equals("unpause"))
                        notifyPauseRequestListeners(IPauseRequestedListener.PauseState.Unpaused);
                    else if (request.request.equals("offline"))
                        notifyPauseRequestListeners(IPauseRequestedListener.PauseState.Offline);
                    else if (request.request.equals("zip") || request.request.equals("tar") || request.request.equals("delete") ||
                            request.request.equals("rerun"))
                        handleJobManagement(request);
                }
                catch (IOException e) {
                    WISELogger.getSpecial(LogName.Backend).warn("Unable to parse manage request.");
                }
            }
		    break;
		case Checkin:
            //someone is asking us to checkin
            if (libMsg.job.equals(TARGET_ALL)|| libMsg.job.equals(myId) || libMsg.job.equals(TARGET_MANAGER)) {
                int verbose = 1;
                if (!Strings.isNullOrEmpty(libMsg.message) && libMsg.message.contains("verbosity")) {
                    if (libMsg.message.contains("2"))
                        verbose = 2;
                    else if (libMsg.message.contains("0"))
                        verbose = 0;
                }
                sendCheckin(libMsg.from, Checkin.Status.Running, verbose);
            }
		    break;
        //a file has been written to the job directory that should be streamed
		case FileStream:
            //only handle messages that are directed at this instance
            if (libMsg.job.equals(myId)) {
                handleFileOutput(libMsg);
            }
		    break;
		case FileResponse:
            if (libMsg.job.equals(myId)) {
                handleFileStreamRequest(libMsg);
            }
		    break;
        //an instance of Manager has distributed load balancing enabled and has received a new job
		case BalanceRequest:
            if (pauseState != IPauseRequestedListener.PauseState.Offline)
                if ((libMsg.job.equals(TARGET_MANAGER) && !libMsg.from.equals(myId)) || libMsg.job.equals(myId))
                    handleBalanceRequest(libMsg.from, libMsg.message);
		    break;
        //an instance of Manager has requested a job file as part of the distributed load balancing
		case BalanceAvailable:
            if (libMsg.job.equals(myId))
                handleBalanceAvailable(libMsg);
		    break;
        //an instance of Manager has sent part of the job file as part of load balancing
		case BalanceStart:
            if (libMsg.job.equals(myId))
                handleBalanceStart(libMsg);
		    break;
		case BalanceList:
            //don't respond to messages that I sent
            if (libMsg.job.equals(TARGET_MANAGER) && !libMsg.from.equals(myId))
                handleListLoadBalance(libMsg);
		    break;
		case ListFiles:
            handleJobFileListRequest(libMsg.from, libMsg.job);
		    break;
	    default:
            WISELogger.getSpecial(LogName.Backend).debug("Didn't know how to handle it");
	        break;
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) { }
	
	public enum ConnectionStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		FAILED
	}
	
	/**
	 * The type of remote job request that was received.
	 */
	public enum RemoteType {
	    SERVER_CREATE,
	    LOAD_BALANCE
	}
	
	@FunctionalInterface
	public static interface IMqttMessageReceivedListener {
		
		void onMqttMessageReceived(ca.wise.lib.mqtt.MqttMessage message);
	}
	
	@FunctionalInterface
	public static interface IShutdownRequestListener {
		
		void onShutdownRequested();
	}
	
	@FunctionalInterface
	public static interface IRestartRequestedListener {
		
		 void onRestartRequested();
	}
    
    public static interface IPauseRequestedListener {
        
         void onPauseRequested(PauseState pause);
         
         boolean isPaused();
         
         /**
          * Possible states for the W.I.S.E. Manager job queue.
          */
         public enum PauseState {
             /**
              * Pause the job queue temporarily, allowing it to be restarted later
              */
             Paused,
             /**
              * Restart the job queue
              */
             Unpaused,
             /**
              * Take W.I.S.E. Manager offline. It must be restarted for the job queue to be started again
              */
             Offline
         }
    }
	
	@FunctionalInterface
	public static interface IConnectionListener {
		
		void onConnectionStatusChanged(ConnectionStatus status);
	}
	
	@FunctionalInterface
	public static interface IRequestJobQueueRunListener {
	    
	    void onJobQueueRun();
	}
}
