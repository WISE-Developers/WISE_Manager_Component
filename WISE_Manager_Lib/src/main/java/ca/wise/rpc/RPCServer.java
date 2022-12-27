package ca.wise.rpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Strings;

import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.rpc.proto.FileChunk;
import ca.wise.rpc.proto.FileResult;
import ca.wise.rpc.proto.FileServerGrpc.FileServerImplBase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * A wrapper for an RPC server for received protobuf files
 * from Builder.
 */
public class RPCServer {

	private final int port;
	private final String address;
	private final String internalAddress;
	private final Server server;
	private final String jobDirectory;
	private final Integer internalDisplayPort;
	private final Integer externalDisplayPort;
	
	public RPCServer(String jobDirectory, String address, String internalAddress, int port,
	        Integer internalDisplayPort, Integer externalDisplayPort) throws IOException {
		this(jobDirectory, ServerBuilder.forPort(port), address, internalAddress, port, internalDisplayPort, externalDisplayPort);
	}
	
	public RPCServer(String jobDirectory, ServerBuilder<?> serverBuilder, String address, String internalAddress, int port,
	        Integer internalDisplayPort, Integer externalDisplayPort) {
	    WISELogger.getSpecial(LogName.Backend).debug("Starting RPC server on port " + port);
		server = serverBuilder.addService(new FileServerService())
				.build();
		this.jobDirectory = jobDirectory;
		this.port = port;
		this.address = address;
		this.internalAddress = internalAddress;
		this.internalDisplayPort = internalDisplayPort;
		this.externalDisplayPort = externalDisplayPort;
	}
	
	public String getFullAddress() {
	    if (externalDisplayPort != null && externalDisplayPort != 0)
	        return address + ":" + externalDisplayPort;
		return address + ":" + port;
	}
	
	public boolean hasInternalAddress() {
	    return !Strings.isNullOrEmpty(internalAddress);
	}
	
	public String getFullInternalAddress() {
	    if (Strings.isNullOrEmpty(internalAddress))
	        return null;
	    if (internalDisplayPort != null && internalDisplayPort != 0)
	        return internalAddress + ":" + internalDisplayPort;
	    return internalAddress + ":" + port;
	}
	
	public void start() throws IOException {
		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				RPCServer.this.stop();
			}
		});
	}
	
	public void stop() {
		if (server != null) {
			server.shutdown();
		}
	}
	
	/**
	 * Get the file extension for the requested file type.
	 * @param type The type of file that is going to be written.
	 * @return The file extension for the file type, or an empty string
	 * if the file type is unknown.
	 */
	private static String getExtension(FileChunk.OutputType type) {
		switch (type) {
		case BINARY:
			return ".fgmb";
		case JSON_MINIMAL:
		case JSON_PRETTY:
			return ".fgmj";
		case XML:
			return ".xml";
		default:
			return "";
		}
	}
	
	private class FileServerService extends FileServerImplBase {
		
		@Override
		public StreamObserver<FileChunk> sendFile(StreamObserver<FileResult> responseObserver) {
			return new StreamObserver<FileChunk>() {
			    private long counter = 0;

				@Override
				public void onNext(FileChunk value) {
                    if (!Strings.isNullOrEmpty(value.getName())) {
                        Path path = Paths.get(jobDirectory, value.getName());
                        Path file = path.resolve("job" + getExtension(value.getType()));
                        if (counter == 0) {
                            counter++;
                            
                            try {
                                //create the job directory
                                Files.createDirectories(path);
                                //if the file already exists but we're restarting the transfer, delete the original file
                                if (Files.exists(file))
                                    Files.delete(file);
                            }
                            catch (IOException e) {
															WISELogger.getNamed("rpc").error("Failed to initialize job directory");
                            }
                        }
                        
    					try {
							try (FileOutputStream stream = new FileOutputStream(file.toFile(), true)) {
								stream.write(value.getData().toByteArray());
							}
    					}
    					catch (IOException e) {
    						WISELogger.getNamed("rpc").error("Unable to save job data", e);
    					}
                    }
				}

				@Override
				public void onError(Throwable t) {
					WISELogger.getNamed("rpc").error("Failed to receive message", t);
				}

				@Override
				public void onCompleted() {
					responseObserver.onNext(
							FileResult.newBuilder()
								.build()
						);
					responseObserver.onCompleted();
				}
			};
		}
	}
}
