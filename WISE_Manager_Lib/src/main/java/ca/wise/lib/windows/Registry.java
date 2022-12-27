package ca.wise.lib.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;

import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;

public class Registry {
	
	public static final String HKCU = "HKCU\\";
	public static final String HKLM = "HKLM\\";
	public static final String SoftwareFolder = "HKCU\\Software\\";

    /**
     * Read a value from the registry.
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key) {
        try {
        	while (location.endsWith("\\"))
        		location = location.substring(0, location.length() - 1);
            // Run reg query, then read output with StreamReader (internal class)
            Process process = Runtime.getRuntime().exec("reg query \"" + location + "\" /v \"" + key + "\"");

            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String output = reader.getResult();

            // Output has the following format:
            // \n<Version information>\n\n<key>\t<registry type>\t<value>
            // Parse out the value
            String[] lines = Arrays.stream(output.split("\\n"))
            		.filter(l -> l.trim().replace("\n", "").replace("\r", "").length() > 0)
            		.toArray(String[]::new);
            if (lines.length == 2) {
            	String[] parsed = Arrays.stream(lines[1].split("\\t|[\\s]{4}", -1))
            			.filter(v -> v.trim().length() > 0)
            			.map(v -> v.replace("\n", "").replace("\r", "").trim())
            			.toArray(String[]::new);
            	if (parsed.length == 3)
            		return parsed[2];
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Delete a registry value.
     * @param location The registry location that contains the value to delete.
     * @param key The name of the value to delete.
     * @return True if the delete call was successful, false otherwise.
     */
    public static final boolean deleteRegistry(String location, String key) {
        try {
            while (location.endsWith("\\"))
                location = location.substring(0, location.length() - 1);
            // Run reg query, then read output with StreamReader (internal class)
            Process process = Runtime.getRuntime().exec("reg delete \"" + location + "\" /f /v \"" + key + "\"");

            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            reader.getResult();
        }
        catch (Exception e) {
            return false;
        }
        
        return true;
    }
    
    public static void writeRegistry(String location) {
    	try {
        	while (location.endsWith("\\"))
        		location = location.substring(0, location.length() - 1);
            Runtime.getRuntime().exec("reg add \"" + location + "\" /f");
    	}
    	catch (Exception e) {
    		WISELogger.getSpecial(LogName.Backend).debug("Unable to write to the registry", e);
    	}
    }
    
    public static void writeRegistry(String location, String key, String value) {
    	try {
        	while (location.endsWith("\\"))
        		location = location.substring(0, location.length() - 1);
            Runtime.getRuntime().exec("reg add \"" + location + "\" /v \"" + key + "\" /f /t REG_SZ /d \"" + value + "\"");
    	}
    	catch (Exception e) {
    		WISELogger.getSpecial(LogName.Backend).debug("Unable to write to the registry", e);
    	}
    }
    
    public static void writeRegistry(String location, String key, int value) {
    	try {
        	while (location.endsWith("\\"))
        		location = location.substring(0, location.length() - 1);
            Runtime.getRuntime().exec("reg add \"" + location + "\" /v \"" + key + "\" /f /t REG_DWORD /d \"" + value + "\"");
    	}
    	catch (Exception e) {
    		WISELogger.getSpecial(LogName.Backend).debug("Unable to write to the registry", e);
    	}
    }

    static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw= new StringWriter();

        public StreamReader(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            }
            catch (IOException e) { }
        }

        public String getResult() {
            return sw.toString();
        }
    }
}
