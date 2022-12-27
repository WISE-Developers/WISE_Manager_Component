package ca.wise.lib;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.time.Instant;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;

import ca.wise.config.proto.ServerConfiguration;
import ca.wise.config.proto.ServerConfiguration.OutputType;
import ca.wise.config.proto.ServerConfiguration.Verbosity;
import ca.hss.platform.OperatingSystem;
import ca.wise.lib.windows.Registry;

/**
 * Get application settings.
 * @author Travis Redpath
 */
public class Settings {

	private static ISettings settings;
	
	private static Integer _processes = null;
	private static Integer _skipProcesses = null;
	private static Boolean _numaLock = null;
	private static Boolean _restart = null;
	private static Point _location = null;
	private static Dimension _size = null;
	private static Integer _splitter = null;
	private static Integer _update = null;
	private static String _job = null;
	private static String _wise = null;
	private static Boolean _tray = null;
	private static String _language = null;
	private static Boolean _lockCPU = null;
	private static Boolean _nativeLAF = null;
	private static Boolean _startPaused = null;
	private static String _builderLocation = null;
	private static String _builderLogLevel = null;
	private static String _builderOutputType = null;
	private static Boolean _builderStartAtStart = null;
	private static Integer _builderMaxBufferSize = null;
	private static Instant _lastImport = null;
    private static Boolean _enableShmem = null;
	
	private static Boolean _rpcEnabled = null;
	private static String _rpcAddress = null;
	private static String _rpcInternalAddress = null;
	private static Integer _rpcPort = null;
	private static Integer _rpcInternalPort = null;
	private static Integer _rpcExternalPort = null;

    private static String _mqttHost = null;
    private static Integer _mqttPort = null;
    private static String _mqttTopic = null;
    private static String _mqttUser = null;
    private static String _mqttPassword = null;
    private static String _mqttId = null;
    private static Boolean _mqttInternalBroker = null;
    private static Boolean _mqttEnableUsername = null;
	
	static {
	    ISettings core;
    	if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows)
    	    core = new WindowsSettings();
    	else
    	    core = new DefaultSettings();

        settings = new JobFolderSettings(core);
	}
	
	public static boolean isLoadValid() {
	    return settings.isLoadValid();
	}
	
	/**
	 * Get the maximum number of processes to run at once.
	 */
	public static int getProcesses() {
		if (_processes == null)
			_processes = settings.getProcesses();
		return _processes;
	}
	
	/**
	 * Set the maximum number of processes to run at once.
	 */
	public static void setProcesses(int value) {
		_processes = value;
		settings.setProcesses(value);
	}
    
    /**
     * The number of processors/cores to leave idle.
     */
    public static int getSkipProcesses() {
        if (_skipProcesses == null)
            _skipProcesses = settings.getSkipProcesses();
        return _skipProcesses;
    }
    
    /**
     * Set the number of processors/cores to leave idle.
     */
    public static void setSkipProcesses(int value) {
        _skipProcesses = value;
        settings.setSkipProcesses(value);
    }
    
    /**
     * Get whether W.I.S.E. instances should be locked to a single NUMA node.
     */
    public static boolean getNumaLock() {
        if (_numaLock == null)
            _numaLock = settings.getNumaLock();
        return _numaLock;
    }
    
    /**
     * Set whether W.I.S.E. instances should be locked to a single NUMA node.
     */
    public static void setNumaLock(boolean value) {
        _numaLock = value;
        settings.setNumaLock(value);
    }

	/**
	 * Get whether to restart.
	 */
	public static boolean getRestartOld() {
		if (_restart == null)
			_restart = settings.getRestartOld();
		return _restart;
	}
	
	/**
	 * Set whether the application should restart.
	 */
	public static void setRestartOld(boolean value) {
		_restart = value;
		settings.setRestartOld(value);
	}

	/**
	 * Get the window location when the application was last closed.
	 */
	public static Point getWindowLocation() {
		if (_location == null)
			_location = settings.getWindowLocation();
		return _location;
	}
	
	/**
	 * Set the window location.
	 */
	public static void setWindowLocation(Point location) {
		_location = location;
		settings.setWindowLocation(location);
	}

	/**
	 * Get the window size when the application was last closed.
	 */
	public static Dimension getWindowSize() {
		if (_size == null)
			_size = settings.getWindowSize();
		return _size;
	}
	
	/**
	 * Set the window size.
	 */
	public static void setWindowSize(Dimension size) {
		_size = size;
		settings.setWindowSize(size);
	}

	/**
	 * Get the position of the splitter between the running and completed jobs.
	 */
	public static Integer getSplitterPosition() {
		if (_splitter == null)
			_splitter = settings.getSplitterPosition();
		return _splitter;
	}
	
	/**
	 * Set the location of the splitter between the running and completed jobs.
	 */
	public static void setSplitterPosition(int value) {
		_splitter = value;
		settings.setSplitterPosition(value);
	}

	/**
	 * Get the frequency at which the CPU usage should be updated.
	 */
	public static int getCpuUpdateFrequency() {
		if (_update == null)
			_update = settings.getCpuUpdateFrequency();
		return _update;
	}
	
	/**
	 * Set the frequency at which the CPU usage should be updated.
	 */
	public static void setCpuUpdateFrequency(int value) {
		_update = value;
		settings.setCpuUpdateFrequency(value);
	}

	/**
	 * Get the directory where job information is stored.
	 */
	public static String getJobDirectory() {
		if (_job == null)
			_job = settings.getJobDirectory();
		return _job;
	}
	
	/**
	 * Set the job directory.
	 */
	public static void setJobDirectory(String value) {
		_job = value;
		settings.setJobDirectory(value);
	}

	/**
	 * Get whether to close the application to the system tray.
	 */
	public static boolean getMinmizeTray() {
		if (_tray == null)
			_tray = settings.getMinimizeTray();
		return _tray;
	}
	
	/**
	 * Set whether the application should close to the system tray.
	 */
	public static void setMinimizeTray(boolean value) {
		_tray = value;
		settings.setMinimizeTray(value);
	}

	/**
	 * Get the user's preferred language
	 */
	public static String getLanguage() {
		if (_language == null)
			_language = settings.getLanguage();
		return _language;
	}
	
	/**
	 * Set the user's preferred language.
	 */
	public static void setLanguage(String value) {
		_language = value;
		settings.setLanguage(value);
	}

	/**
	 * Get the directory where the W.I.S.E. executable is located.
	 */
	public static String getWiseExe() {
		if (_wise == null)
			_wise = settings.getWiseExe();
		return _wise;
	}
	
	/**
	 * Set the location of the W.I.S.E. executable.
	 */
	public static void setWiseExe(String value) {
		_wise = value;
		settings.setWiseExe(value);
	}
	
	/**
	 * Should the manager lock itself to core 0 and W.I.S.E. to the other cores.
	 * @return True to lock the process affinity.
	 */
	public static boolean getLockCPU() {
		if (_lockCPU == null)
			_lockCPU = settings.getLockCPU();
		return _lockCPU;
	}
	
	/**
	 * Set whether the manager should lock itself to core 0 when it starts.
	 */
	public static void setLockCPU(boolean lock) {
		_lockCPU = lock;
		settings.setLockCPU(lock);
	}
	
	/**
	 * Should the UI use the native look-and-feel.
	 * @return True to use the native LAF.
	 */
	public static boolean getNativeLAF() {
		if (_nativeLAF == null)
			_nativeLAF = settings.getNativeLAF();
		return _nativeLAF;
	}
	
	/**
	 * Set whether the manager should lock itself to core 0 when it starts.
	 */
	public static void setNativeLAF(boolean lock) {
		_nativeLAF = lock;
		settings.setNativeLAF(lock);
	}
	
	/**
	 * Should the job queue be paused when the manager is first opened.
	 */
	public static boolean getStartPaused() {
		if (_startPaused == null)
			_startPaused = settings.getStartPaused();
		return _startPaused;
	}
	
	/**
	 * Should the job queue be paused when the manager is first opened.
	 */
	public static void setStartPaused(boolean paused) {
		_startPaused = paused;
		settings.setStartPaused(paused);
	}
	
	/**
	 * Get the location of W.I.S.E. Builder.
	 */
	public static String getBuilderLocation() {
		if (_builderLocation == null)
			_builderLocation = settings.getBuilderLocation();
		return _builderLocation;
	}
	
	/**
	 * Set the location of W.I.S.E. Builder.
	 * @param location The path to the W.I.S.E. Builder jar file.
	 */
	public static void setBuilderLocation(String location) {
		_builderLocation = location;
		settings.setBuilderLocation(location);
	}
	
	/**
	 * Get the log level for W.I.S.E. Builder.
	 * @return The log level.
	 */
	public static String getBuilderLogLevel() {
		if (_builderLogLevel == null)
			_builderLogLevel = settings.getBuilderLogLevel();
		return _builderLogLevel;
	}
	
	/**
	 * Set the log level for W.I.S.E. Builder.
	 * @param level W.I.S.E. Builder's log level.
	 */
	public static void setBuilderLogLevel(String level) {
		_builderLogLevel = level;
		settings.setBuilderLogLevel(level);
	}
	
	/**
	 * Get the output format for W.I.S.E. Builder.
	 */
	public static String getBuilderOutputType() {
		if (_builderOutputType == null)
			_builderOutputType = settings.getBuilderOutputType();
		return _builderOutputType;
	}
	
	/**
	 * Set the output format for W.I.S.E. Builder.
	 * @param type The output format.
	 */
	public static void setBuilderOutputType(String type) {
		_builderOutputType = type;
		settings.setBuilderOutputType(type);
	}
	
	/**
	 * Get whether or not W.I.S.E. Builder should be started with W.I.S.E. Manager is started.
	 * @return True if W.I.S.E. Builder should be started at the same time as W.I.S.E. Manager.
	 */
	public static Boolean getBuilderStartAtStart() {
		if (_builderStartAtStart == null)
			_builderStartAtStart = settings.getBuilderStartAtStart();
		return _builderStartAtStart;
	}
	
	/**
	 * Set whether W.I.S.E. Builder should be started at the same time as W.I.S.E. Manager.
	 * @param start True if W.I.S.E. Builder should be started at the same time as W.I.S.E. Manager.
	 */
	public static void setBuilderStartAtStart(boolean start) {
		_builderStartAtStart = start;
		settings.setBuilderStartAtStart(start);
	}
	
	/**
	 * Get whether or not W.I.S.E. Builder should bundle all input files into the output file.
	 */
	public static Boolean getBuilderSingleFile() {
		return true;
	}
	
	/**
	 * Set whether or not W.I.S.E. Builder should bundle all input files into the output file.
	 */
	public static void setBuilderSingleFile(boolean value) {
	}
	
	/**
	 * The maximum buffer size that should be used when transfering a file from builder
	 * to manager using MQTT.
	 */
	public static Integer getBuilderMaxBufferSize() {
		if (_builderMaxBufferSize == null)
			_builderMaxBufferSize = settings.getBuilderMaxBufferSize();
		return _builderMaxBufferSize;
	}
	
	/**
	 * Set the maximum buffer size that should be used when transfering a file from builder
	 * to manager using MQTT.
	 */
	public static void setBuilderMaxBufferSize(int size) {
		_builderMaxBufferSize = size;
		settings.setBuilderMaxBufferSize(size);
	}
	
	/**
	 * Has the user enabled the RPC server.
	 */
	public static Boolean isRpcEnabled() {
		if (_rpcEnabled == null)
			_rpcEnabled = settings.isRpcEnabled();
		return _rpcEnabled;
	}
	
	/**
	 * Set whether the user has enabled the RPC server.
	 */
	public static void setRpcEnabled(boolean value) {
		_rpcEnabled = null;
		settings.setRpcEnabled(value);
	}
	
	/**
	 * Get the address that can be used to connect to the
	 * local RPC server.
	 */
	public static String getRpcAddress() {
		if (_rpcAddress == null)
			_rpcAddress = settings.getRpcAddress();
		return _rpcAddress;
	}
	
	/**
	 * Set the address that can be used to connect to the
	 * local RPC server.
	 */
	public static void setRpcAddress(String value) {
		_rpcAddress = value;
		settings.setRpcAddress(value);
	}
	
	/**
	 * Get the internal address that can be used to connect
	 * to the local RPC server.
	 */
	public static String getInternalRpcAddress() {
	    if (_rpcInternalAddress == null)
	        _rpcInternalAddress = settings.getInternalRpcAddress();
	    return _rpcInternalAddress;
	}
	
	/**
	 * Set the internal address that can be used to connect
	 * to the local RPC server.
	 */
	public static void setInternalRpcAddress(String address) {
	    _rpcInternalAddress = address;
	    settings.setInternalRpcAddress(address);
	}
	
	/**
	 * Get the port that the RPC server should listen on.
	 */
	public static int getRpcPort() {
		if (_rpcPort == null)
			_rpcPort = settings.getRpcPort();
		return _rpcPort;
	}
	
	/**
	 * Set the port that the RPC server should listen on.
	 */
	public static void setRpcPort(int value) {
		_rpcPort = value;
		settings.setRpcPort(value);
	}
    
    /**
     * Get the port that should be advertised as the external address.
     */
    public static Integer getExternalRpcPort() {
        if (_rpcExternalPort == null)
            _rpcExternalPort = settings.getExternalRpcPort();
        return _rpcExternalPort;
    }
    
    /**
     * Set the port that should be advertised as the external address.
     */
    public static void setExternalRpcPort(Integer value) {
        _rpcExternalPort = value;
        settings.setExternalRpcPort(value);
    }
    
    /**
     * Get the port that should be advertised as the internal address.
     */
    public static Integer getInternalRpcPort() {
        if (_rpcInternalPort == null)
            _rpcInternalPort = settings.getInternalRpcPort();
        return _rpcInternalPort;
    }
    
    /**
     * Set the port that should be advertised as the internal address.
     */
    public static void setInternalRpcPort(Integer value) {
        _rpcInternalPort = value;
        settings.setInternalRpcPort(value);
    }
    
    public static String getHost() {
        if (_mqttHost == null)
            _mqttHost = settings.getHost();
        return _mqttHost;
    }
    
    public static void setHost(String host) {
        _mqttHost = host;
        settings.setHost(host);
    }
    
    public static int getPort() {
        if (_mqttPort == null)
            _mqttPort = settings.getPort();
        return _mqttPort;
    }
    
    public static void setPort(int port) {
        _mqttPort = port;
        settings.setPort(port);
    }
    
    public static String getTopic() {
        if (_mqttTopic == null)
            _mqttTopic = settings.getTopic();
        return _mqttTopic;
    }
    
    public static void setTopic(String topic) {
        _mqttTopic = topic;
        settings.setTopic(topic);
    }
    
    public static String getUser() {
        if (_mqttUser == null)
            _mqttUser = settings.getUser();
        return _mqttUser;
    }
    
    public static void setUser(String user) {
        _mqttUser = user;
        settings.setUser(user);
    }
    
    public static String getPassword() {
        if (_mqttPassword == null)
            _mqttPassword = settings.getPassword();
        return _mqttPassword;
    }
    
    public static void setPassword(String pass) {
        _mqttPassword = pass;
        settings.setPassword(pass);
    }
    
    public static String getMqttId() {
        if (_mqttId == null)
            _mqttId = settings.getMqttId();
        return _mqttId;
    }
    
    public static void setMqttId(String value) {
        _mqttId = value;
        settings.setMqttId(value);
    }
    
    public static boolean useInternalBroker() {
        if (_mqttInternalBroker == null)
            _mqttInternalBroker = settings.useInternalBroker();
        return _mqttInternalBroker;
    }
    
    public static void setUseInternalBroker(boolean value) {
        _mqttInternalBroker = value;
        settings.setUseInternalBroker(value);
    }
    
    public static boolean useAuthentication() {
        if (_mqttEnableUsername == null)
            _mqttEnableUsername = settings.useAuthentication();
        return _mqttEnableUsername;
    }
    
    public static void setUseAuthentication(boolean value) {
        _mqttEnableUsername = value;
        settings.setUseAuthentication(value);
    }
    
    public static boolean isRespectShmem() {
        if (_enableShmem == null)
            _enableShmem = settings.isRespectShmem();
        return _enableShmem;
    }
    
    public static void setRespectShmem(boolean value) {
        _enableShmem = value;
        settings.setRespectShmem(value);
    }
	
	/**
	 * Get the last time that the settings file was imported.
	 */
	public static Instant getLastImport() {
		if (_lastImport == null) {
			String val = settings.getLastUpdate();
			if (val == null)
				_lastImport = Instant.EPOCH;
			else
				_lastImport = Instant.parse(val);
		}
		return _lastImport;
	}
		
	/**
	 * Set the time that the settings file was last imported.
	 */
	public static void setLastImport(Instant time) {
		_lastImport = time;
		settings.setLastUpdate(time.toString());
	}
	
	public static void snapshot() {
	    settings.snapshot();
	}
	
	private static String defaultWiseBuilderLocation() {
		String currentLocation = null;
		try {
			final URL codeSourceLocation = Settings.class.getProtectionDomain().getCodeSource().getLocation();
			if (codeSourceLocation != null) {
				currentLocation = urlToFile(codeSourceLocation.toString());
			}
		}
		catch (SecurityException|NullPointerException e) { }
		
		if (currentLocation == null) {
			URL classResource = Settings.class.getResource(Settings.class.getSimpleName() + ".class");
			if (classResource == null)
				return "";
			
			String url = classResource.toString();
			String suffix = Settings.class.getCanonicalName().replace('.', '/') + ".class";
			if (!url.endsWith(suffix))
				return "";
			String base = url.substring(0, url.length() - suffix.length());
			String path = base;
			if (path.startsWith("jar:"))
				path = path.substring(4, path.length() - 2);
			
			try {
				currentLocation = urlToFile(new URL(path).toString());
			}
			catch (MalformedURLException|NullPointerException e) { }
		}
		
		if (currentLocation != null) {
			Path path = Paths.get(currentLocation);
			path = path.getParent().getParent().getParent().resolve("Other").resolve("WISE_Builder").resolve("WISE_Builder.jar");
			return path.toString();
		}
		return "";
	}
	
	private static String urlToFile(final String url) {
		String path = url;
		if (path.startsWith("jar:")) {
			final int index = path.indexOf("!/");
			path = path.substring(4, index);
		}
		try {
			if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows &&
					path.matches("file:[A-Za-z]:.*")) {
				path = "file:/" + path.substring(5);
			}
			return new File(new URL(path).toURI()).getAbsolutePath();
		}
		catch (MalformedURLException|URISyntaxException e) { }
		if (path.startsWith("file:")) {
			path = path.substring(5);
			return new File(path).getAbsolutePath();
		}
		return null;
	}
	
	
	private static class WindowsSettings implements ISettings {
		
		public WindowsSettings() {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager");
		}
		
		@Override
		public boolean isLoadValid() {
		    return true;
		}

		@Override
		public int getProcesses() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "Processes");
			if (val != null && val.length() > 0) {
				try {
					if (val.startsWith("0x"))
						return Integer.parseInt(val.substring(2), 16);
					else
						return Integer.parseInt(val);
				}
				catch (NumberFormatException e) { }
			}
			return 2;
		}

		@Override
		public void setProcesses(int value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "Processes", value);
		}
		
		@Override
		public boolean deleteProcesses() {
		    return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "Processes");
		}

        @Override
        public int getSkipProcesses() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "SkipProcesses");
            if (val != null && val.length() > 0) {
                try {
                    if (val.startsWith("0x"))
                        return Integer.parseInt(val.substring(2), 16);
                    else
                        return Integer.parseInt(val);
                }
                catch (NumberFormatException e) { }
            }
            return 0;
        }

        @Override
        public void setSkipProcesses(int value) {
            Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "SkipProcesses", value);
        }
        
        @Override
        public boolean deleteSkipProcesses() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "SkipProcesses");
        }

        @Override
        public boolean getNumaLock() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "NumaLock");
            if (val != null && val.length() > 0) {
                return val.equals("true");
            }
            return true;
        }

        @Override
        public void setNumaLock(boolean value) {
            Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "NumaLock", value ? "true" : "false");
        }
        
        @Override
        public boolean deleteNumaLock() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "NumaLock");
        }

		@Override
		public boolean getRestartOld() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "Restart");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return false;
		}

		@Override
		public void setRestartOld(boolean value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "Restart", value ? "true" : "false");
		}
        
        @Override
        public boolean deleteRestartOld() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "Restart");
        }

		@Override
		public Point getWindowLocation() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "WindowLocation");
			if (val != null && val.length() > 0) {
				String[] split = val.split(",");
				if (split.length == 2) {
					try {
						int x = Integer.parseInt(split[0]);
						int y = Integer.parseInt(split[1]);
						return new Point(x, y);
					}
					catch (NumberFormatException e) { }
				}
			}
			return null;
		}

		@Override
		public void setWindowLocation(Point location) {
			String value = location.x + "," + location.y;
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "WindowLocation", value);
		}

		@Override
		public Dimension getWindowSize() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "WindowSize");
			if (val != null && val.length() > 0) {
				String[] split = val.split(",");
				if (split.length == 2) {
					try {
						int x = Integer.parseInt(split[0]);
						int y = Integer.parseInt(split[1]);
						return new Dimension(x, y);
					}
					catch (NumberFormatException e) { }
				}
			}
			return null;
		}

		@Override
		public void setWindowSize(Dimension size) {
			String value = size.width + "," + size.height;
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "WindowSize", value);
		}

		@Override
		public Integer getSplitterPosition() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "SplitterPosition");
			if (val != null && val.length() > 0) {
				try {
					if (val.startsWith("0x"))
						return Integer.parseInt(val.substring(2), 16);
					else
						return Integer.parseInt(val);
				}
				catch (NumberFormatException e) { }
			}
			return null;
		}

		@Override
		public void setSplitterPosition(int value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "SplitterPosition", value);
		}

		@Override
		public int getCpuUpdateFrequency() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "CPUUpdateFrequency");
			if (val != null && val.length() > 0) {
				try {
					if (val.startsWith("0x"))
						return Integer.parseInt(val.substring(2), 16);
					else
						return Integer.parseInt(val);
				}
				catch (NumberFormatException e) { }
			}
			return -1;
		}

		@Override
		public void setCpuUpdateFrequency(int value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "CPUUpdateFrequency", value);
		}

		@Override
		public String getJobDirectory() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "JobDirectory");
			if (val != null && val.length() > 0) {
				return val;
			}
			return System.getProperty("user.home");
		}

		@Override
		public void setJobDirectory(String value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "JobDirectory", value);
		}

		@Override
		public boolean getMinimizeTray() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "Minimize_Tray");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return true;
		}

		@Override
		public void setMinimizeTray(boolean value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "Minimize_Tray", value ? "true" : "false");
		}

		@Override
		public String getLanguage() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "language");
			if (val != null && val.length() > 0) {
				return val;
			}
			return "en";
		}

		@Override
		public void setLanguage(String value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "language", value);
		}

		@Override
		public String getWiseExe() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "WISEExe");
			if (val != null && val.length() > 0) {
				return val;
			}
			try {
				return getEntryLocation() + "/WISE.exe";
			}
			catch (URISyntaxException e) { }
			return Paths.get(System.getProperty("user.home"), "WISE", "WISE.exe").toString();
		}

		@Override
		public void setWiseExe(String value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "WISEExe", value);
		}
        
        @Override
        public boolean deleteWiseExe() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "WISEExe");
        }

		@Override
		public boolean getLockCPU() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "lock_cpu");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return false;
		}

		@Override
		public void setLockCPU(boolean lock) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "lock_cpu", lock ? "true" : "false");
		}
        
        @Override
        public boolean deleteLockCPU() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "lock_cpu");
        }

		@Override
		public boolean getNativeLAF() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "native_laf");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return true;
		}

		@Override
		public void setNativeLAF(boolean laf) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "native_laf", laf ? "true" : "false");
		}

		@Override
		public boolean getStartPaused() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "start_paused");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return false;
		}

		@Override
		public void setStartPaused(boolean paused) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "start_paused", paused ? "true" : "false");
		}
        
        @Override
        public boolean deleteStartPaused() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "start_paused");
        }

		@Override
		public String getBuilderLocation() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_location");
			if (val == null)
				val = defaultWiseBuilderLocation();
			return val;
		}

		@Override
		public void setBuilderLocation(String location) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_location", location);
		}
        
        @Override
        public boolean deleteBuilderLocation() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_location");
        }

		@Override
		public String getBuilderLogLevel() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_log_level");
			if (val != null && val.length() > 0) {
				return val;
			}
			return "warn";
		}

		@Override
		public void setBuilderLogLevel(String level) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_log_level", level);
		}
        
        @Override
        public boolean deleteBuilderLogLevel() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_log_level");
        }

		@Override
		public String getBuilderOutputType() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_output_type");
			if (val != null && val.length() > 0) {
				return val;
			}
			return "json";
		}

		@Override
		public void setBuilderOutputType(String type) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_output_type", type);
		}
        
        @Override
        public boolean deleteBuilderOutputType() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_output_type");
        }

		@Override
		public boolean getBuilderStartAtStart() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_start_at_start");
			if (val != null && val.length() > 0) {
				return val.equals("true");
			}
			return false;
		}

		@Override
		public void setBuilderStartAtStart(boolean start) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_start_at_start", start ? "true" : "false");
		}
        
        @Override
        public boolean deleteBuilderStartAtStart() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_start_at_start");
        }
		
		@Override
		public int getBuilderMaxBufferSize() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_buffer_size");
			if (val != null && val.length() > 0) {
				try { return Integer.parseInt(val); } catch (NumberFormatException e) { }
			}
			return 4096;
		}
		
		@Override
		public void setBuilderMaxBufferSize(int value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_buffer_size", String.valueOf(value));
		}
        
        @Override
        public boolean deleteBuilderMaxBufferSize() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "builder_buffer_size");
        }
		
		@Override
		public boolean isRpcEnabled() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_enable");
			if (val != null && val.equalsIgnoreCase("true"))
				return true;
			return false;
		}
		
		@Override
		public void setRpcEnabled(boolean value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_enable", String.valueOf(value));
		}
        
        @Override
        public boolean deleteRpcEnabled() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_enable");
        }
		
		@Override
		public String getRpcAddress() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_address");
			if (val == null)
				return "127.0.0.1";
			return val;
		}
		
		@Override
		public void setRpcAddress(String value) {
			if (value == null)
				value = "";
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_address", value);
		}
        
        @Override
        public boolean deleteRpcAddress() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_address");
        }
		
		@Override
		public String getInternalRpcAddress() {
		    String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_address");
		    if (Strings.isNullOrEmpty(val))
		        return null;
		    return val;
		}
		
		@Override
		public void setInternalRpcAddress(String value) {
		    if (value == null)
		        value = "";
		    Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_address", value);
		}
        
        @Override
        public boolean deleteInternalRpcAddress() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_address");
        }
		
		@Override
		public int getRpcPort() {
			String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_port");
			if (val == null)
				return 32480;
			try {
				return Integer.parseInt(val);
			}
			catch (NumberFormatException e) { }
			return 32480;
		}
		
		@Override
		public void setRpcPort(int value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_port", String.valueOf(value));
		}
        
        @Override
        public boolean deleteRpcPort() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_port");
        }
        
        @Override
        public Integer getExternalRpcPort() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_external_port");
            if (val == null)
                return null;
            try {
                Integer i = Integer.parseInt(val);
                if (i == 0)
                    return null;
                return i;
            }
            catch (NumberFormatException e) { }
            return null;
        }
        
        @Override
        public void setExternalRpcPort(Integer value) {
            if (value == null)
                Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_external_port", "0");
            else
                Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_external_port", String.valueOf(value));
        }
        
        @Override
        public boolean deleteExternalRpcPort() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_external_port");
        }
        
        @Override
        public Integer getInternalRpcPort() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_port");
            if (val == null)
                return null;
            try {
                Integer i = Integer.parseInt(val);
                if (i == 0)
                    return null;
                return i;
            }
            catch (NumberFormatException e) { }
            return null;
        }
        
        @Override
        public void setInternalRpcPort(Integer value) {
            if (value == null)
                Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_port", "0");
            else
                Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_port", String.valueOf(value));
        }
        
        @Override
        public boolean deleteInternalRpcPort() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "WISE_Manager", "rpc_internal_port");
        }

        @Override
        public String getHost() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Host Name");
            if (Strings.isNullOrEmpty(val))
                return "";
            return val;
        }

        @Override
        public void setHost(String host) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Host Name", host);
        }

        @Override
        public boolean deleteHost() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Host Name");
        }

        @Override
        public int getPort() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Port Number");
            if (val == null)
                return 1883;
            try {
                Integer i = Integer.parseInt(val);
                if (i == 0)
                    return 1883;
                return i;
            }
            catch (NumberFormatException e) { }
            return 1883;
        }

        @Override
        public void setPort(int port) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Port Number", String.valueOf(port));
        }

        @Override
        public boolean deletePort() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Port Number");
        }

        @Override
        public String getTopic() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Topic");
            if (Strings.isNullOrEmpty(val))
                return "wise";
            return val;
        }

        @Override
        public void setTopic(String topic) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Topic", topic);
        }

        @Override
        public boolean deleteTopic() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Topic");
        }

        @Override
        public String getUser() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "User Name");
            if (Strings.isNullOrEmpty(val))
                return "";
            return val;
        }

        @Override
        public void setUser(String user) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "User Name", user);
        }

        @Override
        public boolean deleteUser() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "User Name");
        }

        @Override
        public String getPassword() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Password");
            if (Strings.isNullOrEmpty(val))
                return "";
            return val;
        }

        @Override
        public void setPassword(String pass) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Password", pass);
        }

        @Override
        public boolean deletePassword() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Password");
        }

        @Override
        public String getMqttId() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "MqttId");
            if (Strings.isNullOrEmpty(val))
                return null;
            return val;
        }

        @Override
        public void setMqttId(String value) {
            if (value == null)
                value = "";
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "MqttId", value);
        }

        @Override
        public boolean deleteMqttId() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "MqttId");
        }

        @Override
        public boolean useInternalBroker() {
            String temp = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Internal Broker");
            if (temp != null && (temp.equals("true") || temp.equals("True")))
                return true;
            return false;
        }

        @Override
        public void setUseInternalBroker(boolean value) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Internal Broker", String.valueOf(value));
        }

        @Override
        public boolean deleteUseInternalBroker() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Internal Broker");
        }

        @Override
        public boolean useAuthentication() {
            String temp = Registry.readRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Enable Auth");
            if (temp != null && (temp.equals("true") || temp.equals("True")))
                return true;
            return false;
        }

        @Override
        public void setUseAuthentication(boolean value) {
            Registry.writeRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Enable Auth", String.valueOf(value));
        }

        @Override
        public boolean deleteUseAuthentication() {
            return Registry.deleteRegistry(Registry.SoftwareFolder + "CWFGM Project Steering Committee\\WISE Manager\\MQTT", "Enable Auth");
        }
        
        @Override
        public boolean isRespectShmem() {
            return true;
        }
        
        @Override
        public void setRespectShmem(boolean value) { }
        
        @Override
        public Boolean isV2() {
            String val = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "is_v2_settings");
            if (val != null && val.equalsIgnoreCase("true"))
                return true;
            //check if the W.I.S.E. exe setting exists, if it doesn't assume we're already v2
            String test = Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "WISEExe");
            if (Strings.isNullOrEmpty(test))
                return true;
            return false;
        }
        
        @Override
        public void makeV2() {
            Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "is_v2_settings", "true");
        }
        
        @Override
        public void snapshot() { }
		
		@Override
		public String getLastUpdate() {
			return Registry.readRegistry(Registry.SoftwareFolder + "WISE_Manager", "last_update");
		}
		
		@Override
		public void setLastUpdate(String value) {
			Registry.writeRegistry(Registry.SoftwareFolder + "WISE_Manager", "last_update", value);
		}
		
		private static String getEntryLocation() throws URISyntaxException {
			return new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		}
	}
	
	private static class DefaultSettings implements ISettings {
		
		private Preferences preferences;
		private Preferences mqttPreferences;

		public DefaultSettings() {
			preferences = Preferences.userRoot().node("manager");
			mqttPreferences = Preferences.userRoot().node("mqtt");
		}
		
		@Override
		public boolean isLoadValid() {
		    return true;
		}

		@Override
		public int getProcesses() {
			return preferences.getInt("max_processes", 2);
		}

		@Override
		public void setProcesses(int value) {
			preferences.putInt("max_processes", value);
		}
		
		@Override
		public boolean deleteProcesses() {
		    preferences.remove("max_processes");
		    return true;
		}
		
		@Override
		public int getSkipProcesses() {
		    return preferences.getInt("skip_processes", 0);
		}
		
		@Override
		public void setSkipProcesses(int value) {
		    preferences.putInt("skip_processes", value);
		}
        
        @Override
        public boolean deleteSkipProcesses() {
            preferences.remove("skip_processes");
            return true;
        }

        @Override
        public boolean getNumaLock() {
            return preferences.getBoolean("numa_lock", true);
        }

        @Override
        public void setNumaLock(boolean value) {
            preferences.putBoolean("numa_lock", value);
        }
        
        @Override
        public boolean deleteNumaLock() {
            preferences.remove("numa_lock");
            return true;
        }

		@Override
		public boolean getRestartOld() {
			return preferences.getBoolean("restart", false);
		}

		@Override
		public void setRestartOld(boolean value) {
			preferences.putBoolean("restart", value);
		}
        
        @Override
        public boolean deleteRestartOld() {
            preferences.remove("restart");
            return true;
        }

		@Override
		public Point getWindowLocation() {
			int x = preferences.getInt("window_location_x", Integer.MAX_VALUE);
			int y = preferences.getInt("window_location_y", Integer.MAX_VALUE);
			if (x == Integer.MAX_VALUE || y == Integer.MAX_VALUE)
				return null;
			return new Point(x, y);
		}

		@Override
		public void setWindowLocation(Point location) {
			preferences.putInt("window_location_x", location.x);
			preferences.putInt("window_location_y", location.y);
		}

		@Override
		public Dimension getWindowSize() {
			int w = preferences.getInt("window_size_width", Integer.MAX_VALUE);
			int h = preferences.getInt("window_size_height", Integer.MAX_VALUE);
			if (w == Integer.MAX_VALUE || h == Integer.MAX_VALUE)
				return null;
			return new Dimension(w, h);
		}

		@Override
		public void setWindowSize(Dimension size) {
			preferences.putInt("window_size_width", size.width);
			preferences.putInt("window_size_height", size.height);
		}

		@Override
		public Integer getSplitterPosition() {
			int val = preferences.getInt("splitter_distance", -1);
			if (val == -1)
				return null;
			return val;
		}

		@Override
		public void setSplitterPosition(int value) {
			preferences.putInt("splitter_distance", value);
		}

		@Override
		public int getCpuUpdateFrequency() {
			return preferences.getInt("cpu_update_frequency", -1);
		}

		@Override
		public void setCpuUpdateFrequency(int value) {
			preferences.putInt("cpu_update_frequency", value);
		}

		@Override
		public String getJobDirectory() {
			return preferences.get("job_directory", "/user/home");
		}

		@Override
		public void setJobDirectory(String value) {
            String v = value == null ? "" : value;
			preferences.put("job_directory", v);
		}

		@Override
		public boolean getMinimizeTray() {
			return preferences.getBoolean("minimize_tray", true);
		}

		@Override
		public void setMinimizeTray(boolean value) {
			preferences.putBoolean("minimize_tray", value);
		}

		@Override
		public String getLanguage() {
			return preferences.get("language", "en");
		}

		@Override
		public void setLanguage(String value) {
            String v = value == null ? "" : value;
			preferences.put("language", v);
		}

		@Override
		public String getWiseExe() {
			return preferences.get("wise_exe", "/usr/bin/wise");
		}

		@Override
		public void setWiseExe(String value) {
            String v = value == null ? "" : value;
			preferences.put("wise_exe", v);
		}
        
        @Override
        public boolean deleteWiseExe() {
            preferences.remove("wise_exe");
            return true;
        }

		@Override
		public boolean getLockCPU() {
			return preferences.getBoolean("lock_cpu", false);
		}

		@Override
		public void setLockCPU(boolean lock) {
			preferences.putBoolean("lock_cpu", lock);
		}
        
        @Override
        public boolean deleteLockCPU() {
            preferences.remove("lock_cpu");
            return true;
        }

		@Override
		public boolean getNativeLAF() {
			return preferences.getBoolean("native_laf", true);
		}
		
		@Override
		public void setNativeLAF(boolean laf) {
			preferences.putBoolean("native_laf", laf);
		}

		@Override
		public boolean getStartPaused() {
			return preferences.getBoolean("start_paused", false);
		}

		@Override
		public void setStartPaused(boolean paused) {
			preferences.putBoolean("start_paused", paused);
		}
        
        @Override
        public boolean deleteStartPaused() {
            preferences.remove("start_paused");
            return true;
        }

		@Override
		public String getBuilderLocation() {
			String val = preferences.get("builder_location", null);
			if (val == null)
				val = defaultWiseBuilderLocation();
			return val;
		}

		@Override
		public void setBuilderLocation(String location) {
            String v = location == null ? "" : location;
			preferences.put("builder_location", v);
		}
        
        @Override
        public boolean deleteBuilderLocation() {
            preferences.remove("builder_location");
            return true;
        }

		@Override
		public String getBuilderLogLevel() {
			return preferences.get("builder_log_level", "warn");
		}

		@Override
		public void setBuilderLogLevel(String level) {
            String v = level == null ? "" : level;
			preferences.put("builder_log_level", v);
		}
        
        @Override
        public boolean deleteBuilderLogLevel() {
            preferences.remove("builder_log_level");
            return true;
        }

		@Override
		public String getBuilderOutputType() {
			return preferences.get("builder_output_type", "json");
		}

		@Override
		public void setBuilderOutputType(String type) {
            String t = type == null ? "" : type;
			preferences.put("builder_output_type", t);
		}
        
        @Override
        public boolean deleteBuilderOutputType() {
            preferences.remove("builder_output_type");
            return true;
        }

		@Override
		public boolean getBuilderStartAtStart() {
			return preferences.getBoolean("builder_start_at_start", false);
		}

		@Override
		public void setBuilderStartAtStart(boolean start) {
			preferences.putBoolean("builder_start_at_start", start);
		}
        
        @Override
        public boolean deleteBuilderStartAtStart() {
            preferences.remove("builder_start_at_start");
            return true;
        }
		
		@Override
		public int getBuilderMaxBufferSize() {
			return preferences.getInt("builder_buffer_size", 4096);
		}
		
		@Override
		public void setBuilderMaxBufferSize(int value) {
			preferences.putInt("builder_buffer_size", value);
		}
        
        @Override
        public boolean deleteBuilderMaxBufferSize() {
            preferences.remove("builder_buffer_size");
            return true;
        }
		
		@Override
		public boolean isRpcEnabled() {
			return preferences.getBoolean("rpc_enable", false);
		}
		
		@Override
		public void setRpcEnabled(boolean value) {
			preferences.putBoolean("rpc_enable", value);
		}
        
        @Override
        public boolean deleteRpcEnabled() {
            preferences.remove("rpc_enable");
            return true;
        }
		
		@Override
		public String getRpcAddress() {
			return preferences.get("rpc_address", "127.0.0.1");
		}
		
		@Override
		public void setRpcAddress(String value) {
            String v = value == null ? "" : value;
			preferences.put("rpc_address", v);
		}
        
        @Override
        public boolean deleteRpcAddress() {
            preferences.remove("rpc_address");
            return true;
        }
		
		@Override
		public String getInternalRpcAddress() {
		    return preferences.get("rpc_internal_address", null);
		}
		
		@Override
		public void setInternalRpcAddress(String value) {
		    String v = value == null ? "" : value;
		    preferences.put("rpc_internal_address", v);
		}
        
        @Override
        public boolean deleteInternalRpcAddress() {
            preferences.remove("rpc_internal_address");
            return true;
        }
		
		@Override
		public int getRpcPort() {
			return preferences.getInt("rpc_port", 32480);
		}
		
		@Override
		public void setRpcPort(int value) {
			preferences.putInt("rpc_port", value);
		}
        
        @Override
        public boolean deleteRpcPort() {
            preferences.remove("rpc_port");
            return true;
        }
        
        @Override
        public Integer getExternalRpcPort() {
            int p = preferences.getInt("rpc_external_port", 0);
            if (p == 0)
                return null;
            return p;
        }
        
        @Override
        public void setExternalRpcPort(Integer value) {
            if (value == null)
                preferences.remove("rpc_external_port");
            else
                preferences.putInt("rpc_external_port", value);
        }
        
        @Override
        public boolean deleteExternalRpcPort() {
            preferences.remove("rpc_external_port");
            return true;
        }
        
        @Override
        public Integer getInternalRpcPort() {
            int p = preferences.getInt("rpc_internal_port", 0);
            if (p == 0)
                return null;
            return p;
        }
        
        @Override
        public void setInternalRpcPort(Integer value) {
            if (value == null)
                preferences.remove("rpc_internal_port");
            else
                preferences.putInt("rpc_internal_port", value);
        }
        
        @Override
        public boolean deleteInternalRpcPort() {
            preferences.remove("rpc_internal_port");
            return true;
        }

        @Override
        public String getHost() {
            return preferences.get("Host Name", "");
        }

        @Override
        public void setHost(String host) {
            preferences.put("Host Name", host);
        }

        @Override
        public boolean deleteHost() {
            mqttPreferences.remove("Host Name");
            return true;
        }

        @Override
        public int getPort() {
            return preferences.getInt("Port Number", 1883);
        }

        @Override
        public void setPort(int port) {
            preferences.putInt("Port Number", port);
        }

        @Override
        public boolean deletePort() {
            mqttPreferences.remove("Port Number");
            return true;
        }

        @Override
        public String getTopic() {
            return preferences.get("Topic", "wise");
        }

        @Override
        public void setTopic(String topic) {
            preferences.put("Topic", topic);
        }

        @Override
        public boolean deleteTopic() {
            mqttPreferences.remove("Topic");
            return true;
        }

        @Override
        public String getUser() {
            return preferences.get("User Name", "");
        }

        @Override
        public void setUser(String user) {
            preferences.put("User Name", user);
        }

        @Override
        public boolean deleteUser() {
            mqttPreferences.remove("User Name");
            return true;
        }

        @Override
        public String getPassword() {
            return preferences.get("Password", "");
        }

        @Override
        public void setPassword(String pass) {
            preferences.put("Password", pass);
        }

        @Override
        public boolean deletePassword() {
            mqttPreferences.remove("Password");
            return true;
        }

        @Override
        public String getMqttId() {
            return preferences.get("MqttId", null);
        }

        @Override
        public void setMqttId(String value) {
            preferences.put("MqttId", value);
        }

        @Override
        public boolean deleteMqttId() {
            mqttPreferences.remove("MqttId");
            return true;
        }

        @Override
        public boolean useInternalBroker() {
            return preferences.getBoolean("Internal Broker", false);
        }

        @Override
        public void setUseInternalBroker(boolean value) {
            preferences.putBoolean("Internal Broker", value);
        }

        @Override
        public boolean deleteUseInternalBroker() {
            mqttPreferences.remove("Internal Broker");
            return true;
        }

        @Override
        public boolean useAuthentication() {
            return preferences.getBoolean("Enable Auth", false);
        }

        @Override
        public void setUseAuthentication(boolean value) {
            preferences.putBoolean("Enable Auth", value);
        }

        @Override
        public boolean deleteUseAuthentication() {
            mqttPreferences.remove("Enable Auth");
            return true;
        }
        
        @Override
        public boolean isRespectShmem() {
            return true;
        }
        
        @Override
        public void setRespectShmem(boolean value) { }
        
        @Override
        public Boolean isV2() {
            boolean val = preferences.getBoolean("is_v2_settings", false);
            if (val)
                return true;
            else {
                //check if the W.I.S.E. exe setting exists, if it doesn't assume we're already v2
                String test = preferences.get("WISEExe", null);
                if (Strings.isNullOrEmpty(test))
                    return true;
            }
            return false;
        }
        
        @Override
        public void makeV2() {
            preferences.putBoolean("is_v2_settings", true);
        }
        
        @Override
        public void snapshot() { }
		
		@Override
		public String getLastUpdate() {
			return preferences.get("last_update", null);
		}
		
		@Override
		public void setLastUpdate(String value) {
            String v = value == null ? "" : value;
			preferences.put("last_update", v);
		}
	}
    
    private static class JobFolderSettings implements ISettings {
        
        private ISettings coreSettings;
        private ServerConfiguration.Builder config;
        private boolean lock = false;
        private boolean loadValid = false;
        
        protected JobFolderSettings(ISettings core) {
            coreSettings = core;
            try {
                String text = new String(Files.readAllBytes(Paths.get(core.getJobDirectory() + "/config.json")), StandardCharsets.UTF_8);

                ObjectMapper mapper = new ObjectMapper();
                //do fail if there is information after the JSON object, even if the object is valid
                mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
                //don't fail if properties that aren't known by manager exist
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                mapper.readTree(text);

                config = ServerConfiguration.newBuilder();
                
                JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(text, config);
                loadValid = true;
            }
            catch (IOException e) {
                config = getDefaultServerConfiguration(core.getJobDirectory());
                e.printStackTrace();
                loadValid = false;
            }
            if (!core.isV2())
                makeV2();
        }

        /**
         * Get the default server configuration settings.
         * @param jobDirectory The job directory.
         * @return The default server configuration settings.
         */
        private static ServerConfiguration.Builder getDefaultServerConfiguration(String jobDirectory) {
            ServerConfiguration.Builder builder = ServerConfiguration.newBuilder();
            builder.getLogBuilder()
                .setFilename("logfile.log")
                .setVerbosity(Verbosity.WARN);
            builder.getSignalsBuilder()
                .setStart("start.txt")
                .setComplete("complete.txt");
            builder.getHardwareBuilder()
                .setProcesses(4)
                .setCores(2);
            builder.getMqttBuilder()
                .setHostname("127.0.0.1")
                .setPort(1883)
                .setTopic("wise")
                .setVerbosity(Verbosity.INFO)
                .setQos(1)
                .setUsername("")
                .setPassword("");
            builder.getBuilderBuilder()
                .setHostname("127.0.0.1")
                .setPort(32479)
                .setFormat(OutputType.JSON);
            builder.getManagerSettingsBuilder()
                .setMaxConcurrent(2)
                .setRestartOld(false)
                .setWiseLocation("C:\\Program Files\\Prometheus\\WISE.exe")
                .setLockCpu(false)
                .setStartPaused(false)
                .setNumaLock(BoolValue.of(true))
                .setSkipCores(Int32Value.of(0))
                .getMqttSettingsBuilder()
                    .setMaxBufferSize(4096)
                    .setUseInternalBroker(BoolValue.of(false));
            builder.getManagerSettingsBuilder()
                .getRpcSettingsBuilder()
                    .setEnabled(false)
                    .setExternalAddress("127.0.0.1")
                    .setPort(32480);
            builder.setExampleDirectory(jobDirectory);
            return builder;
        }
        
        @Override
        public boolean isLoadValid() {
            return loadValid;
        }
        
        @Override
        public String getJobDirectory() {
            return coreSettings.getJobDirectory();
        }

        @Override
        public void setJobDirectory(String value) {
            coreSettings.setJobDirectory(value);
        }

        @Override
        public Point getWindowLocation() {
            return coreSettings.getWindowLocation();
        }

        @Override
        public void setWindowLocation(Point location) {
            coreSettings.setWindowLocation(location);
        }

        @Override
        public Dimension getWindowSize() {
            return coreSettings.getWindowSize();
        }

        @Override
        public void setWindowSize(Dimension size) {
            coreSettings.setWindowSize(size);
        }

        @Override
        public Integer getSplitterPosition() {
            return coreSettings.getSplitterPosition();
        }

        @Override
        public void setSplitterPosition(int value) {
            coreSettings.setSplitterPosition(value);
        }

        @Override
        public int getCpuUpdateFrequency() {
            return coreSettings.getCpuUpdateFrequency();
        }

        @Override
        public void setCpuUpdateFrequency(int value) {
            coreSettings.setCpuUpdateFrequency(value);
        }

        @Override
        public boolean getMinimizeTray() {
            return coreSettings.getMinimizeTray();
        }

        @Override
        public void setMinimizeTray(boolean value) {
            coreSettings.setMinimizeTray(value);
        }

        @Override
        public String getLanguage() {
            return coreSettings.getLanguage();
        }

        @Override
        public void setLanguage(String langauge) {
            coreSettings.setLanguage(langauge);
        }

        @Override
        public boolean getNativeLAF() {
            return coreSettings.getNativeLAF();
        }

        @Override
        public void setNativeLAF(boolean lock) {
            coreSettings.setNativeLAF(lock);
        }

        @Override
        public String getLastUpdate() {
            return coreSettings.getLastUpdate();
        }

        @Override
        public void setLastUpdate(String value) {
            coreSettings.setLastUpdate(value);
        }
        
        @Override
        public Boolean isV2() {
            return true;
        }
        
        @Override
        public void snapshot() {
            Path path = Paths.get(getJobDirectory(), "config.json");
            try {
                if (Files.exists(path)) {
                    Path back = Paths.get(getJobDirectory(), ".config.json.bak");
                    if (Files.exists(back))
                        Files.delete(back);
                    Files.copy(path, back);
                    if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows) {
                        DosFileAttributeView attrs = Files.getFileAttributeView(back, DosFileAttributeView.class);
                        attrs.setHidden(true);
                    }
                }
            }
            catch (IOException e) { }
        }

        @Override
        public int getProcesses() {
            return config.getManagerSettings() != null ? config.getManagerSettings().getMaxConcurrent() : 2;
        }

        @Override
        public void setProcesses(int value) {
            config.getManagerSettingsBuilder().setMaxConcurrent(value);
            save();
        }

        @Override
        public boolean deleteProcesses() {
            config.getManagerSettingsBuilder().setMaxConcurrent(2);
            save();
            return true;
        }

        @Override
        public int getSkipProcesses() {
            return config.getManagerSettingsBuilder().hasSkipCores() ? config.getManagerSettingsBuilder().getSkipCores().getValue() : 0;
        }
        
        @Override
        public void setSkipProcesses(int value) {
            config.getManagerSettingsBuilder().setSkipCores(Int32Value.of(value));
            save();
        }
        
        @Override
        public boolean deleteSkipProcesses() {
            config.getManagerSettingsBuilder().clearSkipCores();
            save();
            return true;
        }

        @Override
        public boolean getNumaLock() {
            return config.getManagerSettingsBuilder().hasNumaLock() ? config.getManagerSettingsBuilder().getNumaLock().getValue() : true;
        }
        
        @Override
        public void setNumaLock(boolean value) {
            config.getManagerSettingsBuilder().setNumaLock(BoolValue.of(value));
            save();
        }
        
        @Override
        public boolean deleteNumaLock() {
            config.getManagerSettingsBuilder().clearNumaLock();
            save();
            return true;
        }

        @Override
        public boolean getRestartOld() {
            return config.getManagerSettingsBuilder().getRestartOld();
        }

        @Override
        public void setRestartOld(boolean value) {
            config.getManagerSettingsBuilder().setRestartOld(value);
            save();
        }

        @Override
        public boolean deleteRestartOld() {
            config.getManagerSettingsBuilder().setRestartOld(false);
            save();
            return true;
        }

        @Override
        public String getWiseExe() {
            return config.getManagerSettingsBuilder().getWiseLocation();
        }

        @Override
        public void setWiseExe(String value) {
            config.getManagerSettingsBuilder().setWiseLocation(value);
            save();
        }

        @Override
        public boolean deleteWiseExe() {
            config.getManagerSettingsBuilder().setWiseLocation("");
            save();
            return true;
        }

        @Override
        public boolean getLockCPU() {
            return config.getManagerSettingsBuilder().getLockCpu();
        }

        @Override
        public void setLockCPU(boolean lock) {
            config.getManagerSettingsBuilder().setLockCpu(lock);
            save();
        }

        @Override
        public boolean deleteLockCPU() {
            config.getManagerSettingsBuilder().setLockCpu(true);
            save();
            return true;
        }

        @Override
        public boolean getStartPaused() {
            return config.getManagerSettingsBuilder().getStartPaused();
        }

        @Override
        public void setStartPaused(boolean lock) {
            config.getManagerSettingsBuilder().setStartPaused(lock);
            save();
        }

        @Override
        public boolean deleteStartPaused() {
            config.getManagerSettingsBuilder().setStartPaused(false);
            save();
            return true;
        }

        @Override
        public String getBuilderLocation() {
            return config.getManagerSettingsBuilder().getInternalBuilderBuilder().getBuilderLocation();
        }

        @Override
        public void setBuilderLocation(String location) {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setBuilderLocation(location);
            save();
        }

        @Override
        public boolean deleteBuilderLocation() {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setBuilderLocation("");
            save();
            return true;
        }

        @Override
        public String getBuilderLogLevel() {
            return config.getManagerSettingsBuilder().getInternalBuilderBuilder().getLogLevel();
        }

        @Override
        public void setBuilderLogLevel(String level) {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setLogLevel(level);
            save();
        }

        @Override
        public boolean deleteBuilderLogLevel() {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setLogLevel("warn");
            save();
            return true;
        }

        @Override
        public String getBuilderOutputType() {
            OutputType type = config.getBuilderBuilder().getFormat();
            switch (type) {
            case BINARY:
                return "binary";
            case XML:
                return "xml";
            case JSON_MINIMAL:
                return "json_mini";
            default:
                return "json";
            }
        }

        @Override
        public void setBuilderOutputType(String type) {
            switch (type) {
            case "binary":
                config.getBuilderBuilder().setFormat(OutputType.BINARY);
                break;
            case "xml":
                config.getBuilderBuilder().setFormat(OutputType.XML);
                break;
            case "json":
                config.getBuilderBuilder().setFormat(OutputType.JSON);
                break;
            case "json_mini":
                config.getBuilderBuilder().setFormat(OutputType.JSON_MINIMAL);
                break;
            }
            save();
        }

        @Override
        public boolean deleteBuilderOutputType() {
            config.getBuilderBuilder().clearFormat();
            save();
            return true;
        }

        @Override
        public boolean getBuilderStartAtStart() {
            return config.getManagerSettingsBuilder().getInternalBuilderBuilder().getStartWithManager();
        }

        @Override
        public void setBuilderStartAtStart(boolean start) {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setStartWithManager(start);
            save();
        }

        @Override
        public boolean deleteBuilderStartAtStart() {
            config.getManagerSettingsBuilder().getInternalBuilderBuilder().setStartWithManager(false);
            save();
            return true;
        }

        @Override
        public int getBuilderMaxBufferSize() {
            return config.getManagerSettingsBuilder().getMqttSettingsBuilder().getMaxBufferSize();
        }

        @Override
        public void setBuilderMaxBufferSize(int size) {
            config.getManagerSettingsBuilder().getMqttSettingsBuilder().setMaxBufferSize(size);
            save();
        }

        @Override
        public boolean deleteBuilderMaxBufferSize() {
            config.getManagerSettingsBuilder().getMqttSettingsBuilder().setMaxBufferSize(4096);
            save();
            return true;
        }

        @Override
        public boolean isRpcEnabled() {
            return config.getManagerSettingsBuilder().hasRpcSettings() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getEnabled() : false;
        }

        @Override
        public void setRpcEnabled(boolean value) {
            config.getManagerSettingsBuilder().getRpcSettingsBuilder().setEnabled(value);
            save();
        }

        @Override
        public boolean deleteRpcEnabled() {
            config.getManagerSettingsBuilder().getRpcSettingsBuilder().setEnabled(false);
            save();
            return true;
        }

        @Override
        public String getRpcAddress() {
            return config.getManagerSettingsBuilder().hasRpcSettings() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getExternalAddress() : "";
        }

        @Override
        public void setRpcAddress(String value) {
            config.getManagerSettingsBuilder().getRpcSettingsBuilder().setExternalAddress(value);
            save();
        }

        @Override
        public boolean deleteRpcAddress() {
            if (config.getManagerSettingsBuilder().hasRpcSettings())
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().setExternalAddress("127.0.0.1");
            save();
            return true;
        }

        @Override
        public String getInternalRpcAddress() {
            return config.getManagerSettingsBuilder().hasRpcSettings() && config.getManagerSettingsBuilder().getRpcSettingsBuilder().hasInternalAddress() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getInternalAddress().getValue() : null;
        }

        @Override
        public void setInternalRpcAddress(String value) {
            if (Strings.isNullOrEmpty(value))
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearInternalAddress();
            else
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().setInternalAddress(StringValue.of(value));
            save();
        }

        @Override
        public boolean deleteInternalRpcAddress() {
            if (config.getManagerSettingsBuilder().hasRpcSettings())
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearInternalAddress();
            save();
            return true;
        }

        @Override
        public int getRpcPort() {
            return config.getManagerSettingsBuilder().hasRpcSettings() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getPort() : 32480;
        }

        @Override
        public void setRpcPort(int value) {
            config.getManagerSettingsBuilder().getRpcSettingsBuilder().setPort(value);
            save();
        }

        @Override
        public boolean deleteRpcPort() {
            if (config.getManagerSettingsBuilder().hasRpcSettings())
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().setPort(32480);
            save();
            return true;
        }

        @Override
        public Integer getExternalRpcPort() {
            return config.getManagerSettingsBuilder().hasRpcSettings() && config.getManagerSettingsBuilder().getRpcSettingsBuilder().hasExternalPort() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getExternalPort().getValue() : null;
        }

        @Override
        public void setExternalRpcPort(Integer value) {
            if (value == null || value == 0)
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearExternalPort();
            else
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().setExternalPort(Int32Value.of(value));
            save();
        }

        @Override
        public boolean deleteExternalRpcPort() {
            if (config.getManagerSettingsBuilder().hasRpcSettings())
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearExternalPort();
            save();
            return true;
        }

        @Override
        public Integer getInternalRpcPort() {
            return config.getManagerSettingsBuilder().hasRpcSettings() && config.getManagerSettingsBuilder().getRpcSettingsBuilder().hasInternalPort() ? config.getManagerSettingsBuilder().getRpcSettingsBuilder().getInternalPort().getValue() : null;
        }

        @Override
        public void setInternalRpcPort(Integer value) {
            if (value == null || value == 0)
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearInternalPort();
            else
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().setInternalPort(Int32Value.of(value));
            save();
        }

        @Override
        public boolean deleteInternalRpcPort() {
            if (config.getManagerSettingsBuilder().hasRpcSettings())
                config.getManagerSettingsBuilder().getRpcSettingsBuilder().clearInternalPort();
            save();
            return true;
        }

        @Override
        public String getHost() {
            return config.hasMqtt() ? config.getMqttBuilder().getHostname() : "";
        }

        @Override
        public void setHost(String host) {
            if (host == null)
                host = "";
            config.getMqttBuilder().setHostname(host);
            save();
        }

        @Override
        public boolean deleteHost() {
            if (config.hasMqtt())
                config.getMqttBuilder().setHostname("");
            save();
            return true;
        }

        @Override
        public int getPort() {
            return config.hasMqtt() ? config.getMqttBuilder().getPort() : 1883;
        }

        @Override
        public void setPort(int port) {
            config.getMqttBuilder().setPort(port);
            save();
        }

        @Override
        public boolean deletePort() {
            if (config.hasMqtt())
                config.getMqttBuilder().setPort(1883);
            save();
            return true;
        }

        @Override
        public String getTopic() {
            return config.hasMqtt() ? config.getMqttBuilder().getTopic() : "wise";
        }

        @Override
        public void setTopic(String topic) {
            if (!Strings.isNullOrEmpty(topic)) {
                config.getMqttBuilder().setTopic(topic);
                save();
            }
            else
                deleteTopic();
        }

        @Override
        public boolean deleteTopic() {
            if (config.hasMqtt())
                config.getMqttBuilder().setTopic("wise");
            save();
            return true;
        }

        @Override
        public String getUser() {
            return config.hasMqtt() ? config.getMqttBuilder().getUsername() : "";
        }

        @Override
        public void setUser(String user) {
            if (!Strings.isNullOrEmpty(user)) {
                config.getMqttBuilder().setUsername(user);
                save();
            }
            else
                deleteUser();
        }

        @Override
        public boolean deleteUser() {
            if (config.hasMqtt())
                config.getMqttBuilder().setUsername("");
            save();
            return true;
        }

        @Override
        public String getPassword() {
            return config.hasMqtt() ? config.getMqttBuilder().getPassword() : "";
        }

        @Override
        public void setPassword(String pass) {
            if (!Strings.isNullOrEmpty(pass)) {
                config.getMqttBuilder().setPassword(pass);
                save();
            }
            else
                deletePassword();
        }

        @Override
        public boolean deletePassword() {
            if (config.hasMqtt())
                config.getMqttBuilder().setPassword("");
            save();
            return true;
        }

        @Override
        public String getMqttId() {
            return (config.getManagerSettingsBuilder().hasMqttSettings() && config.getManagerSettingsBuilder().getMqttSettingsBuilder().hasMqttId()) ?
                    config.getManagerSettingsBuilder().getMqttSettingsBuilder().getMqttId().getValue() : null;
        }

        @Override
        public void setMqttId(String value) {
            if (!Strings.isNullOrEmpty(value)) {
                config.getManagerSettingsBuilder().getMqttSettingsBuilder().setMqttId(StringValue.of(value));
                save();
            }
            else
                deleteMqttId();
        }

        @Override
        public boolean deleteMqttId() {
            if (config.getManagerSettingsBuilder().hasMqttSettings())
                config.getManagerSettingsBuilder().getMqttSettingsBuilder().clearMqttId();
            save();
            return true;
        }

        @Override
        public boolean useInternalBroker() {
            return config.getManagerSettingsBuilder().hasMqttSettings() && config.getManagerSettingsBuilder().getMqttSettingsBuilder().hasUseInternalBroker() ?
                    config.getManagerSettingsBuilder().getMqttSettingsBuilder().getUseInternalBroker().getValue() : false;
        }

        @Override
        public void setUseInternalBroker(boolean value) {
            config.getManagerSettingsBuilder().getMqttSettingsBuilder().setUseInternalBroker(BoolValue.of(value));
            save();
        }

        @Override
        public boolean deleteUseInternalBroker() {
            if (config.getManagerSettingsBuilder().hasMqttSettings())
                config.getManagerSettingsBuilder().getMqttSettingsBuilder().clearUseInternalBroker();
            save();
            return true;
        }

        @Override
        public boolean useAuthentication() {
            return config.getManagerSettingsBuilder().hasMqttSettings() && config.getManagerSettingsBuilder().getMqttSettingsBuilder().hasUseInternalAuthentication() ?
                    config.getManagerSettingsBuilder().getMqttSettingsBuilder().getUseInternalAuthentication().getValue() : false;
        }

        @Override
        public void setUseAuthentication(boolean value) {
            config.getManagerSettingsBuilder().getMqttSettingsBuilder().setUseInternalAuthentication(BoolValue.of(value));
            save();
        }

        @Override
        public boolean deleteUseAuthentication() {
            if (config.getManagerSettingsBuilder().hasMqttSettings())
                config.getManagerSettingsBuilder().getMqttSettingsBuilder().clearUseInternalAuthentication();
            save();
            return true;
        }
        
        @Override
        public boolean isRespectShmem() {
            if (config.getManagerSettingsBuilder().hasRespectShmem())
                return config.getManagerSettingsBuilder().getRespectShmem().getValue();
            return true;
        }
        
        @Override
        public void setRespectShmem(boolean value) {
            config.getManagerSettingsBuilder().getRespectShmemBuilder().setValue(value);
        }

        /**
         * Convert the existing settings to config.json.
         */
        public void makeV2() {
            //skip the process if it has already happened
            if (!config.hasAlreadyV2() || !config.getAlreadyV2().getValue()) {
                snapshot();
                lock = true;
                
                setProcesses(coreSettings.getProcesses());
                coreSettings.deleteProcesses();
                setSkipProcesses(coreSettings.getSkipProcesses());
                coreSettings.deleteSkipProcesses();
                setNumaLock(coreSettings.getNumaLock());
                coreSettings.deleteNumaLock();
                setRestartOld(coreSettings.getRestartOld());
                coreSettings.deleteRestartOld();
                setWiseExe(coreSettings.getWiseExe());
                coreSettings.deleteWiseExe();
                setLockCPU(coreSettings.getLockCPU());
                coreSettings.deleteLockCPU();
                setStartPaused(coreSettings.getStartPaused());
                coreSettings.deleteStartPaused();
                setBuilderLocation(coreSettings.getBuilderLocation());
                coreSettings.deleteBuilderLocation();
                setBuilderLogLevel(coreSettings.getBuilderLogLevel());
                coreSettings.deleteBuilderLogLevel();
                setBuilderOutputType(coreSettings.getBuilderOutputType());
                coreSettings.deleteBuilderOutputType();
                setBuilderStartAtStart(coreSettings.getBuilderStartAtStart());
                coreSettings.deleteBuilderStartAtStart();
                setBuilderMaxBufferSize(coreSettings.getBuilderMaxBufferSize());
                coreSettings.deleteBuilderMaxBufferSize();
                setRpcEnabled(coreSettings.isRpcEnabled());
                coreSettings.deleteRpcEnabled();
                setRpcAddress(coreSettings.getRpcAddress());
                coreSettings.deleteRpcAddress();
                setInternalRpcAddress(coreSettings.getInternalRpcAddress());
                coreSettings.deleteInternalRpcAddress();
                setRpcPort(coreSettings.getRpcPort());
                coreSettings.deleteRpcPort();
                setExternalRpcPort(coreSettings.getExternalRpcPort());
                coreSettings.deleteExternalRpcPort();
                setInternalRpcPort(coreSettings.getInternalRpcPort());
                coreSettings.deleteInternalRpcPort();
                setHost(coreSettings.getHost());
                coreSettings.deleteHost();
                setPort(coreSettings.getPort());
                coreSettings.deletePort();
                setTopic(coreSettings.getTopic());
                coreSettings.deleteTopic();
                setUser(coreSettings.getUser());
                coreSettings.deleteUser();
                setPassword(coreSettings.getPassword());
                coreSettings.deletePassword();
                setMqttId(coreSettings.getMqttId());
                coreSettings.deleteMqttId();
                setUseInternalBroker(coreSettings.useInternalBroker());
                coreSettings.deleteUseInternalBroker();
                setUseAuthentication(coreSettings.useAuthentication());
                coreSettings.deleteUseAuthentication();
                
                coreSettings.makeV2();
                config.setAlreadyV2(BoolValue.of(true));
                
                lock = false;
                save();
            }
        }
        
        /**
         * Save the updated configuration to the config file.
         */
        private void save() {
            if (!lock) {
                try (FileWriter writer = new FileWriter(coreSettings.getJobDirectory() + "/config.json")) {
                    writer.write(JsonFormat.printer()
                        .print(config));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
	
	private static interface ISettings {
	    
	    public boolean isLoadValid();

		public int getProcesses();
		public void setProcesses(int value);
		public boolean deleteProcesses();

        public int getSkipProcesses();
        public void setSkipProcesses(int value);
        public boolean deleteSkipProcesses();

        public boolean getNumaLock();
        public void setNumaLock(boolean value);
        public boolean deleteNumaLock();
		
		public boolean getRestartOld();
		public void setRestartOld(boolean value);
		public boolean deleteRestartOld();
		
		public Point getWindowLocation();
		public void setWindowLocation(Point location);
		
		public Dimension getWindowSize();
		public void setWindowSize(Dimension size);
		
		public Integer getSplitterPosition();
		public void setSplitterPosition(int value);
		
		public int getCpuUpdateFrequency();
		public void setCpuUpdateFrequency(int value);
		
		public String getJobDirectory();
		public void setJobDirectory(String value);
		
		public String getWiseExe();
		public void setWiseExe(String value);
		public boolean deleteWiseExe();
		
		public boolean getMinimizeTray();
		public void setMinimizeTray(boolean value);
		
		public String getLanguage();
		public void setLanguage(String langauge);
		
		public boolean getLockCPU();
		public void setLockCPU(boolean lock);
		public boolean deleteLockCPU();
		
		public boolean getNativeLAF();
		public void setNativeLAF(boolean lock);
		
		public boolean getStartPaused();
		public void setStartPaused(boolean lock);
		public boolean deleteStartPaused();
		
		public String getBuilderLocation();
		public void setBuilderLocation(String location);
		public boolean deleteBuilderLocation();
		
		public String getBuilderLogLevel();
		public void setBuilderLogLevel(String level);
		public boolean deleteBuilderLogLevel();
		
		public String getBuilderOutputType();
		public void setBuilderOutputType(String type);
		public boolean deleteBuilderOutputType();
		
		public boolean getBuilderStartAtStart();
		public void setBuilderStartAtStart(boolean start);
		public boolean deleteBuilderStartAtStart();
		
		public int getBuilderMaxBufferSize();
		public void setBuilderMaxBufferSize(int size);
		public boolean deleteBuilderMaxBufferSize();
		
		public boolean isRpcEnabled();
		public void setRpcEnabled(boolean value);
		public boolean deleteRpcEnabled();
		
		public String getRpcAddress();
		public void setRpcAddress(String value);
		public boolean deleteRpcAddress();
		
		public String getInternalRpcAddress();
		public void setInternalRpcAddress(String value);
		public boolean deleteInternalRpcAddress();
		
		public int getRpcPort();
		public void setRpcPort(int value);
		public boolean deleteRpcPort();
        
        public Integer getExternalRpcPort();
        public void setExternalRpcPort(Integer value);
        public boolean deleteExternalRpcPort();
        
        public Integer getInternalRpcPort();
        public void setInternalRpcPort(Integer value);
        public boolean deleteInternalRpcPort();
        
        public String getHost();
        public void setHost(String host);
        public boolean deleteHost();
        
        public int getPort();
        public void setPort(int port);
        public boolean deletePort();
        
        public String getTopic();
        public void setTopic(String topic);
        public boolean deleteTopic();
        
        public String getUser();
        public void setUser(String user);
        public boolean deleteUser();
        
        public String getPassword();
        public void setPassword(String pass);
        public boolean deletePassword();
        
        public String getMqttId();
        public void setMqttId(String value);
        public boolean deleteMqttId();
        
        public boolean useInternalBroker();
        public void setUseInternalBroker(boolean value);
        public boolean deleteUseInternalBroker();
        
        public boolean useAuthentication();
        public void setUseAuthentication(boolean value);
        public boolean deleteUseAuthentication();
        
        public Boolean isV2();
        public void makeV2();
		
		public String getLastUpdate();
		public void setLastUpdate(String value);
		
		public boolean isRespectShmem();
		public void setRespectShmem(boolean value);
		
		public void snapshot();
	}
}
