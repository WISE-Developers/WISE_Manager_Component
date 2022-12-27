package ca.wise.settings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

import ca.wise.lib.Settings;
import ca.wise.settings.proto.ManagerSettings;

public class SettingsHelper {
	
	private static ManagerSettings loadSettings(String folder) {
		ManagerSettings retval = null;
		Path path = Paths.get(folder);
		if (Files.exists(path)) {
			try (FileReader reader = new FileReader(path.toFile())) {
				ManagerSettings.Builder builder = ManagerSettings.newBuilder();
				JsonFormat.parser()
					.ignoringUnknownFields()
					.merge(reader, builder);
				retval = builder.build();
			}
			catch (IOException e) {
			}
		}
		
		return retval;
	}
	
	/**
	 * If the settings in in the specified folder are valid
	 * and have been updated more recently than the internal
	 * settings the internal settings will be updated.
	 * @param path The path to the settings JSON file.
	 */
	public static void reloadSettings(String path) {
		ManagerSettings settings = loadSettings(path);
		if (settings != null) {
			Instant internalDate = Settings.getLastImport();
			Instant fileDate = Instant.ofEpochSecond(settings.getLastUpdate().getSeconds());
			
			if (fileDate.isAfter(internalDate)) {
				Settings.setJobDirectory(settings.getJobDirectory());
				
				Settings.setLastImport(Instant.now());
			}
		}
	}
	
	public static void exportSettings(String path) {
		ManagerSettings.Builder builder = ManagerSettings.newBuilder()
				.setJobDirectory(Settings.getJobDirectory())
				.setLastUpdate(Timestamp.newBuilder()
						.setSeconds(Instant.now().getEpochSecond()));
		
		Path output = Paths.get(path);
		try {
			Files.createDirectories(output.getParent());
			try(FileWriter writer = new FileWriter(output.toFile())) {
				String json = JsonFormat.printer()
						.includingDefaultValueFields()
						.print(builder);
				writer.write(json);
			}
		}
		catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
