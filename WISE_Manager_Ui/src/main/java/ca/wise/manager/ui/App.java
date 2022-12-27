package ca.wise.manager.ui;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.UIManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

import com.sun.jna.platform.win32.Ole32;

import ca.hss.platform.OperatingSystem;
import ca.hss.tr.Translations;
import ca.wise.cli.RangeOption;
import ca.wise.lib.MqttSettings;
import ca.wise.lib.WISELogger;
import ca.wise.lib.Settings;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.settings.SettingsHelper;

/**
 * Application entry point.
 */
public class App {
	
	private static String restartCommand;
	
	/**
	 * Get the command used to start this instance of the application.
	 */
	public static String getRestartCommand() {
		return restartCommand;
	}
	
	/**
	 * Recreate the command used to run the application so it can
	 * be used later if a restart is requested.
	 */
	private static void buildRestartCommand() {
		StringBuilder builder = new StringBuilder("\"");
		builder.append(System.getProperty("java.home"));
		builder.append(File.separator);
		builder.append("bin");
		builder.append(File.separator);
		if (System.console() == null)
			builder.append("javaw\" ");
		else
			builder.append("java\" ");
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArgs = bean.getInputArguments();
		if (jvmArgs != null && jvmArgs.size() > 0) {
			builder.append(String.join(" ", jvmArgs));
		}
		String path = System.getProperty("java.class.path");
		String command = System.getProperty("sun.java.command");
		//try to detect when run with -jar
		if (command.startsWith(path) && path.endsWith(".jar"))
		    builder.append(" -jar ");
		else {
    		builder.append(" -classpath \"");
    		builder.append(path);
            builder.append("\" ");
		}
		builder.append(command);
		restartCommand = builder.toString().trim();
	}
	
	/**
	 * Parse command line options.
	 * @param args The command line arguments.
	 * @return The parsed options.
	 */
	public static WISEArguments parseCommandLine(String[] args) {
		WISEArguments retval = new WISEArguments();
		
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Show this help message."));
        options.addOption(new RangeOption("l", "log", true, "Log level: valid values range from 0 (none) to 5 (all).", 0, 5));
        options.addOption(new Option("c", "command", false, "Output log messages to the command line."));
        options.addOption(new Option("m", "mqtt", true, "The MQTT host to connect to."));
        options.addOption(new Option("p", "port", true, "The MQTT port to connect to."));
        options.addOption(new Option("j", "jobs", true, "The shared job directory."));
        options.addOption(new Option("e", "exe", true, "The path to the W.I.S.E. executable."));
        //options.addOption(new Option("n", "noui", false, "Run W.I.S.E. Manager without a UI."));
        
        DefaultParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        
        try {
        	cmd = parser.parse(options, args);
        }
        catch (ParseException e) {
        	System.out.println(e.getMessage());
        	formatter.printHelp("WISE_Manager", options);
        	
        	System.exit(1);
        }
        
        if (cmd.hasOption('h')) {
        	formatter.printHelp("WISE_Manager", options);
        	System.exit(0);
        }
        
        //the user has specified a custom log level
        if (cmd.hasOption("log")) {
        	Optional<Option> o = Arrays.stream(cmd.getOptions()).filter(x -> x.getOpt().equals("l")).findFirst();
        	if (o.isPresent()) {
        		RangeOption ro = (RangeOption)o.get();
        		if (ro.isValid()) {
        			retval.logLevel = ro.getIValue();
        		}
        	}
        }
        
        //the user wants to log to the command line
        if (cmd.hasOption('c')) {
        	retval.logCommandLine = true;
        }
        //the user asked to run headless
        //if (cmd.hasOption('n')) {
        //	retval.runHeadless = true;
        //}
        if (cmd.hasOption('m')) {
        	MqttSettings.setHost(cmd.getOptionValue('m'));
        }
        if (cmd.hasOption('p')) {
        	try {
        		String val = cmd.getOptionValue('p');
        		int i = Integer.parseInt(val);
        		MqttSettings.setPort(i);
        	}
        	catch (NumberFormatException e) { }
        }
        if (cmd.hasOption('j')) {
        	Settings.setJobDirectory(cmd.getOptionValue('j'));
        }
        if (cmd.hasOption('e')) {
        	Settings.setWiseExe(cmd.getOptionValue('e'));
        }
        
        return retval;
	}
	
	private static File lockFile;
	private static RandomAccessFile randomAccessFile;
	private static FileLock fileLock;
	public static void unlockInstance() throws IOException {
	    if (lockFile != null) {
            fileLock.release();
            randomAccessFile.close();
            lockFile.delete();
	    }
	}
	
	/**
	 * Try to create the lock file so that only a single instance
	 * can be run at once.
	 * @param programData The folder to store application data in.
	 * @return True if a lock was obtained, false if another instance is already running.
	 */
	private static boolean tryLockInstance(String programData) {
		try {
			if (programData == null)
				programData = "";
			else if (programData.length() > 0 && !programData.endsWith(File.separator))
				programData += File.separator;
			lockFile = new File(programData + "RUNNING_LOCK");
			randomAccessFile = new RandomAccessFile(lockFile, "rw");
			randomAccessFile.writeChars(ManagementFactory.getRuntimeMXBean().getName());
			fileLock = randomAccessFile.getChannel().tryLock();
			if (fileLock == null) {
			    lockFile = null;
			    randomAccessFile = null;
			}
			else {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					
					@Override
					public void run() {
						try {
							unlockInstance();
						}
						catch (Exception e) { }
					}
				});
				return true;
			}
		}
		catch (Exception e) { }
		
		return false;
	}
	
    public static void main(String[] args) {
    	//exit if Java only allows headless applications
    	//we don't support that
    	if (GraphicsEnvironment.isHeadless()) {
    		Instant inst = Instant.now();
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    				.withZone(ZoneId.systemDefault());
    		System.out.println(formatter.format(inst) + " ERROR Headless mode is not currently supported.");
    		return;
    	}
    	
    	String workingDirectory;
    	//find the location where application data should be stored
    	OperatingSystem os = OperatingSystem.getOperatingSystem();
    	if (os.getType() == OperatingSystem.Type.Windows) {
            Ole32.INSTANCE.CoInitialize(null);
            Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null,
                    Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
            
            WindowsHelper.setCurrentProcessExplicitAppUserModelID("ca.wise.manager");
    		workingDirectory = "";
        	Iterable<Path> paths = FileSystems.getDefault().getRootDirectories();
        	for (Path path : paths) {
        		Path test = path.resolve("\\ProgramData\\");
        		if (Files.exists(test)) {
        			workingDirectory = test.toString() + "\\CWFGM\\WISE_Manager\\";
        			break;
        		}
        	}
    	}
    	else if (os.getType() == OperatingSystem.Type.Mac) {
    		workingDirectory = "/Library/Application Support/CWFGM/WISE_Manager/";
    	}
    	else {
    		workingDirectory = "/var/log/CWFGM/WISE_Manager/";
    	}
    	
    	try {
			String location = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
			Path path = Paths.get(location);
			path = path.getParent().resolve("manager.json");
			if (Files.exists(path))
				SettingsHelper.reloadSettings(path.toString());
		}
    	catch (URISyntaxException e1) { }

    	//create the application data directory if it doesn't exist
		try {
			Files.createDirectories(Paths.get(workingDirectory));
		}
		catch (IOException e) {
			workingDirectory = "";
		}
		//if an application data directory was found cleanup old log files
    	if (workingDirectory.length() > 0) {
    		Instant instant = Instant.now().minus(1, ChronoUnit.DAYS);
    		WISELogger.cleanupOldFiles(workingDirectory, instant);
    	}
    	//set the logger to output to the application data directory (or current directory if one wasn't found)
    	System.setProperty("logging.outputdir", workingDirectory);
    	//parse the command line arguments
    	WISEArguments cmds = parseCommandLine(args);
    	//set the log level
    	Level logLevel = Level.ERROR;
    	switch (cmds.logLevel) {
    	case 0:
    		logLevel = Level.OFF;
    		break;
    	case 1:
    		logLevel = Level.INFO;
    		break;
    	case 2:
    		logLevel = Level.FATAL;
    		break;
    	case 3:
    		logLevel = Level.ERROR;
    		break;
    	case 4:
    		logLevel = Level.DEBUG;
    		break;
    	case 5:
    		logLevel = Level.ALL;
    		break;
    	}
		WISELogger.setLogLevel(logLevel);
		//if the user requested it, enable logging to the command line
		if (cmds.logCommandLine)
			WISELogger.setOutputCommandLine(logLevel);
    	//exit if there is already another instance running
		if (!tryLockInstance(workingDirectory)) {
			WISELogger.getSpecial(LogName.Ui).info("Instance already running");
			System.exit(0);
		}
		
		//build the command used to restart the application
    	buildRestartCommand();
    	//set the application language
    	if (Settings.getLanguage().equals("fr"))
    		Translations.setLocale(Locale.FRENCH);
    	else
    		Translations.setLocale(Locale.ENGLISH);
    	
    	if (!Settings.isLoadValid()) {
    	    WISELogger.getSpecial(LogName.Ui).error("Failed to load a valid config.json. Check the file and restart.");
    	    System.exit(1);
    	}
    	
    	//log that the application is starting
    	WISELogger.getSpecial(LogName.Ui).debug("Starting W.I.S.E. Manager UI");
    	
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					if (cmds.runHeadless)
						WISELogger.getSpecial(LogName.Ui).info("Headless mode is not currently supported.");
					if (Settings.getNativeLAF())
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	
					//start the UI
					MainForm window = new MainForm();
					window.setVisible(true);
				} catch (Exception e) {
					WISELogger.getSpecial(LogName.Ui).fatal("Program terminating unexpectedly.", e);
				}
			}
		});
    }
    
    private static class WISEArguments {
    	
    	/**
    	 * The log level to output.
    	 */
    	public int logLevel = 3;
    	
    	/**
    	 * Should log messages be written to the command line.
    	 */
    	public boolean logCommandLine = false;
    	
    	/**
    	 * Should W.I.S.E. Manager start in console mode, without a UI.
    	 */
    	public boolean runHeadless = false;
    }
}
