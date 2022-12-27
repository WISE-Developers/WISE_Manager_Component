package ca.wise.lib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * A logger for events that occur in W.I.S.E. Manager.
 * 
 * @author Travis Redpath
 */
public class WISELogger {

	private static final HashMap<String, Logger> loggers = new HashMap<>();
	
	public static Logger getNamed(String name) {
		return loggers.computeIfAbsent(name, (n) -> LogManager.getLogger(n));
	}
	
	public static Logger getSpecial(LogName name) {
		return getNamed(name.name);
	}
	
	public static void setLogLevel(Level level) {
		setLogLevel(level, LogManager.ROOT_LOGGER_NAME);
		setLogLevel(level, LogName.Backend.name);
		setLogLevel(level, LogName.CommandLine.name);
		setLogLevel(level, LogName.Ui.name);
	}
	
	private static void setLogLevel(Level level, String logger) {
		LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		Map<String, LoggerConfig> loggerConfigs = config.getLoggers();
		if (loggerConfigs.containsKey(logger)) {
			LoggerConfig loggerConfig = loggerConfigs.get(logger);
			loggerConfig.setLevel(level);
			ctx.updateLoggers();
		}
	}
	
	public static void setOutputCommandLine(Level level) {
		LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		Appender appender = config.getAppender("stdout");
		if (appender != null) {
			Map<String, LoggerConfig> loggerConfigs = config.getLoggers();
			for (LoggerConfig logger : loggerConfigs.values()) {
				if (logger.getName().length() > 0)
					logger.addAppender(appender, level, null);
			}
			ctx.updateLoggers();
		}
	}
	
	public static void cleanupOldFiles(String directory, Instant date) {
    	try {
    		Files.list(Paths.get(directory))
	    		.filter(f -> f.getFileName().toString().endsWith(".gz"))
	    		.map(f -> {
	    			BasicFileAttributes bfa = null;
	    			try {
	    				bfa = Files.readAttributes(f, BasicFileAttributes.class);
	    			}
	    			catch (IOException ioe) { }
	    			return new Object[] { f, bfa };
	    		})
	    		.filter(g -> g[1] != null && ((BasicFileAttributes)g[1]).creationTime().toInstant().isBefore(date))
	    		.map(g -> (Path)g[0])
	    		.forEach(f -> {
	    			try {
	    				Files.delete(f);
	    			}
	    			catch (IOException ioe) { }
	    		});
    	}
    	catch (Exception e) {
    	}
	}
	
	public enum LogName {
		Backend("ManagerLib"),
		CommandLine("Manager"),
		Ui("ManagerUi");
		
		private final String name;
		
		LogName(String name) {
			this.name= name;
		}
	}
}
