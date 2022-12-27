package ca.wise.geoserver;

import kong.unirest.Unirest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class GeoServerBase {
	
	private static boolean hasInitialized = false;
	
	static {
		if (!hasInitialized) {
			hasInitialized = true;
			Unirest.config()
				.setObjectMapper(new ProtobufObjectMapper())
				.addDefaultHeader("User-Agent", "PSaaS Manager");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Unirest.shutDown();
			}));
		}
	}

	@Getter protected String baseUrl;
	@Getter protected String username;
	protected String password;
}
