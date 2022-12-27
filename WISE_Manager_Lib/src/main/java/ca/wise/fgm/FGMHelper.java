package ca.wise.fgm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.wise.fgm.proto.WISEData;

/**
 * A helper class for loading FGM details.
 */
public final class FGMHelper {

	/**
	 * Get the number of cores requested for a protobuf job.
	 * @param path The path to the protobuf file.
	 * @return The number of cores requested. Returns -1 if not found.
	 */
	public static int getCores(Path path) {
		int cores = -1;
		if (Files.exists(path)) {
			WISEData data = null;
			//json protobuf
			if (path.toString().endsWith(".fgmj")) {
				//loading an entire fgmj file to get the core count takes too long.
				//use the below class to search for only a single property.
				try (FileInputStream stream = new FileInputStream(path.toFile())) {
					Optional<String> sub = new JSONHelper("settings.hardware.cores").getWithin(stream);
					if (sub.isPresent()) {
						cores = Integer.parseInt(sub.get());
					}
				}
				catch (Exception e) { }
			}
			//binary protobuf
			else if (path.toString().endsWith("fgmb")) {
				try (FileInputStream stream = new FileInputStream(path.toString())) {
					data = WISEData.parseFrom(stream);
				}
				catch (IOException e) {
				}
				
				if (data != null) {
					if (data.getSettings() != null && data.getSettings().getHardware() != null) {
						cores = data.getSettings().getHardware().getCores();
					}
				}
			}
		}
		return cores;
	}
	
	/**
	 * Get the type of load balancing that should be used for the job.
     * @param path The path to the protobuf file.
	 * @return
	 */
	public static LoadBalance getLoadBalancing(Path path) {
	    LoadBalance retval = LoadBalance.NONE;
        if (Files.exists(path)) {
            WISEData data = null;
            //json protobuf
            if (path.toString().endsWith(".fgmj")) {
                //loading an entire fgmj file to get the load balancing type takes too long.
                //use the below class to search for only a single property.
                try (FileInputStream stream = new FileInputStream(path.toFile())) {
                    Optional<String> sub = Optional.empty();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(stream);
                    node = node.get("project");
                    if (!node.isNull()) {
                        node = node.get("loadBalancing");
                        if (!node.isNull()) {
                            if (node.isTextual())
                                sub = Optional.of(node.asText());
                            else if (node.isNumber())
                                sub = Optional.of(String.valueOf(node.asInt()));
                        }
                    }
                    if (sub.isPresent()) {
                        retval = LoadBalance.fromString(sub.get());
                    }
                }
                catch (Exception e) { }
            }
            //binary protobuf
            else if (path.toString().endsWith("fgmb")) {
                try (FileInputStream stream = new FileInputStream(path.toString())) {
                    data = WISEData.parseFrom(stream);
                }
                catch (IOException e) {
                }
                
                if (data != null) {
                    if (data.getSettings() != null && data.getSettings().getHardware() != null) {
                        retval = LoadBalance.fromInt(data.getProject().getLoadBalancing().getNumber());
                    }
                }
            }
        }
        return retval;
	}
	
	private static class JSONHelper {
		private final List<JSONKey> keys;
		protected static final JsonFactory JSON_FACTORY = new JsonFactory();
		
		public JSONHelper(final String from) {
			this.keys = Arrays.stream((from.startsWith("[") ? from : String.valueOf("." + from)).split("(?=\\[|\\]|\\.)"))
					.filter(x -> !"]".equals(x))
					.map(JSONKey::new)
					.collect(Collectors.toList());
		}

	    public Optional<String> getWithin(final InputStream json) throws IOException {
	        return this.getWithin(json, false);
	    }

	    public Optional<String> getWithin(final InputStream json, final boolean strict) throws IOException {
	        return getValueAt(JSON_FACTORY.createParser(json), 0, strict);
	    }

	    protected Optional<String> getValueAt(final JsonParser parser, final int idx, final boolean strict) throws IOException {
	        try {
	            if (parser.isClosed()) {
	                return Optional.empty();
	            }

	            if (idx >= this.keys.size()) {
	                parser.nextToken();
	                if (null == parser.getValueAsString()) {
	                    throw new JSONPathException("The selected node is not a leaf");
	                }

	                return Optional.of(parser.getValueAsString());
	            }

	            this.keys.get(idx).advanceCursor(parser);
	            return getValueAt(parser, idx + 1, strict);
	        }
	        catch (final JSONPathException e) {
	            if (strict) {
	                throw (null == e.getCause() ? new JSONPathException(e.getMessage() + String.format(", at path: '%s'", this.toString(idx)), e) : e);
	            }

	            return Optional.empty();
	        }
	    }

	    @Override
	    public String toString() {
	        return ((Function<String, String>) x -> x.startsWith(".") ? x.substring(1) : x)
	                .apply(this.keys.stream().map(JSONKey::toString).collect(Collectors.joining()));
	    }

	    private String toString(final int idx) {
	        return ((Function<String, String>) x -> x.startsWith(".") ? x.substring(1) : x)
	                .apply(this.keys.subList(0, idx).stream().map(JSONKey::toString).collect(Collectors.joining()));
	    }
	}

    @SuppressWarnings("serial")
    public static class JSONPathException extends RuntimeException {

        public JSONPathException() {
            super();
        }

        public JSONPathException(final String message) {
            super(message);
        }

        public JSONPathException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public JSONPathException(final Throwable cause) {
            super(cause);
        }
    }
	
	private static class JSONKey {
        private final String key;
        private final JsonToken startToken;

        public JSONKey(final String str) {
            this(str.substring(1), str.startsWith("[") ? JsonToken.START_ARRAY : JsonToken.START_OBJECT);
        }

        private JSONKey(final String key, final JsonToken startToken) {
            this.key = key;
            this.startToken = startToken;
        }

        /**
         * Advances the cursor until finding the current {@link JSONKey}, or
         * having consumed the entirety of the current JSON Object or Array.
         */
        public void advanceCursor(final JsonParser parser) throws IOException {
            final JsonToken token = parser.nextToken();
            if (!this.startToken.equals(token)) {
                throw new JSONPathException(String.format("Expected token of type '%s', got: '%s'", this.startToken, token));
            }

            if (JsonToken.START_ARRAY.equals(this.startToken)) {
                // Moving cursor within a JSON Array
                for (int i = 0; i != Integer.valueOf(this.key).intValue(); i++) {
                    JSONKey.skipToNext(parser);
                }
            }
            else {
                // Moving cursor in a JSON Object
                String name;
                for (parser.nextToken(), name = parser.getCurrentName(); !this.key.equals(name); parser.nextToken(), name = parser.getCurrentName()) {
                    JSONKey.skipToNext(parser);
                }
            }
        }

        /**
         * Advances the cursor to the next entry in the current JSON Object
         * or Array.
         */
        private static void skipToNext(final JsonParser parser) throws IOException {
            final JsonToken token = parser.nextToken();
            if (JsonToken.START_ARRAY.equals(token) || JsonToken.START_OBJECT.equals(token) || JsonToken.FIELD_NAME.equals(token)) {
                skipToNextImpl(parser, 1);
            }
            else if (JsonToken.END_ARRAY.equals(token) || JsonToken.END_OBJECT.equals(token)) {
                throw new JSONPathException("Could not find requested key");
            }
        }

        /**
         * Consumes whatever is next until getting back to the
         * same depth level.
         */
        private static void skipToNextImpl(final JsonParser parser, int depth) throws IOException {
            JsonToken previous = null;
            while (depth > 0) {
                final JsonToken token = parser.nextToken();
                if (JsonToken.START_ARRAY.equals(token) || JsonToken.START_OBJECT.equals(token) || JsonToken.FIELD_NAME.equals(token)) {
                    if ((JsonToken.START_OBJECT.equals(token) || JsonToken.START_ARRAY.equals(token)) && (previous == null || !JsonToken.FIELD_NAME.equals(previous)))
                        depth++;
                    else if (JsonToken.FIELD_NAME.equals(token))
                        depth++;
                }
                else
                    depth--;
                previous = token;
            }
        }

        @Override
        public String toString() {
            return String.format(this.startToken.equals(JsonToken.START_ARRAY) ? "[%s]" : ".%s", this.key);
        }
	}
}
