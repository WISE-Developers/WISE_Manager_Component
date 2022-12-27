package ca.wise.rpc;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

import ca.wise.comms.client.proto.FileStreamResponse;
import ca.wise.lib.WISELogger;
import ca.wise.lib.Settings;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.mqtt.MqttListener;
import ca.wise.rpc.proto.GenericFileChunk;
import ca.wise.rpc.proto.GenericFileResult;
import ca.wise.rpc.proto.GenericFileServerGrpc;
import ca.wise.rpc.proto.GenericFileServerGrpc.GenericFileServerStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

public class GenericRPCClient {

    private final MqttListener client;
    private final FileStreamResponse response;
    private final Path filePath;
    private final String managerId;
    private ManagedChannel channel;
    private GenericFileServerStub asyncStub;
    private String rpcAddress;
    
    /**
     * Create a new RPC client to transfer a W.I.S.E. output file to an RPC server.
     * @param client The MQTT client to send the results of the file transfer to.
     * @param response The MQTT message that started the file transfer.
     * @param managerId The ID of the current W.I.S.E. Manager instance.
     * @param filePath The full path to the file that is to be transferred.
     */
    public GenericRPCClient(MqttListener client, FileStreamResponse response, String managerId, Path filePath) {
        this.client = client;
        this.response = response;
        this.managerId = managerId;
        this.filePath = filePath;
    }
    
    public void runAsync() {
        new Thread(() -> {
            try {
                start();
                sendFileAsync();
            }
            catch (Exception e) {
                WISELogger.getSpecial(LogName.Backend).error("Error submitting file via RPC", e);
            }
            finally {
                try {
                    shutdown();
                }
                catch (Exception e) {
                    WISELogger.getSpecial(LogName.Backend).error("Error shutting down file RPC", e);
                }
            }
        }).start();
    }
    
    public void start() {
        //if the remote server specified both an internal and external address
        if (response.hasInternalRpcAddress() && !Strings.isNullOrEmpty(response.getInternalRpcAddress().getValue())) {
            //get my address
            String myAddress = Settings.getRpcAddress();
            //if I know my address
            if (!Strings.isNullOrEmpty(myAddress)) {
                //remove the port from the end of the remote external address
                String externalAddress;
                int index = response.getRpcAddress().getValue().lastIndexOf(':');
                if (index > 0)
                    externalAddress = response.getRpcAddress().getValue().substring(0, index);
                else
                    externalAddress = response.getRpcAddress().getValue();
                //if my external address is the same as the remote external address use the internal address
                if (myAddress.equals(externalAddress))
                    rpcAddress = response.getInternalRpcAddress().getValue();
                //if my external address is different than the remote external address use the external address
                else
                    rpcAddress = response.getRpcAddress().getValue();
            }
            //if I don't know my address just use the remote external address
            else
                rpcAddress = response.getRpcAddress().getValue();
        }
        //if the remote server only has an external address use that
        else
            rpcAddress = response.getRpcAddress().getValue();
        
        WISELogger.getSpecial(LogName.Backend).warn("Creating connection to gRPC server at " + rpcAddress);
        channel = ManagedChannelBuilder
                    .forTarget(rpcAddress)
                    .usePlaintext()
                    .build();
        asyncStub = GenericFileServerGrpc.newStub(channel);
    }
    
    public void shutdown() throws InterruptedException {
        if (channel != null && !channel.isShutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    
    public void sendFileAsync() throws Exception {
        CountDownLatch finishedLatch = new CountDownLatch(1);
        AtomicBoolean wasError = new AtomicBoolean(false);
        
        ClientResponseObserver<GenericFileChunk, GenericFileResult> responseObserver = new ClientResponseObserver<GenericFileChunk, GenericFileResult>() {
            
            @Override
            public void onNext(GenericFileResult value) {
                //the server doesn't return anything at this time
            }

            @Override
            public void onError(Throwable t) {
                WISELogger.getSpecial(LogName.Backend).error("Failed to send file data.", t);
                wasError.set(true);
                finishedLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishedLatch.countDown();
            }

            @Override
            public void beforeStart(final ClientCallStreamObserver<GenericFileChunk> requestStream) {
                
                Runnable drain = new Runnable() {
                    int offset = 0;
                    boolean isComplete = false;
                    long fileSize = -1;
                    
                    @Override
                    public void run() {
                        if (fileSize < 0) {
                            try {
                                fileSize = Files.size(filePath);
                                //WISELogger.getSpecial(LogName.Backend).warn("Sending " + fileSize + "B file " + filePath + " to " + rpcAddress);
                            }
                            catch (IOException e) {
                                WISELogger.getSpecial(LogName.Backend).error("Failed to get the size of the file to transfer.", e);
                            }
                        }

                        try {
                            final int size = 2097152;//2MiB
                            
                            try (RandomAccessFile reader = new RandomAccessFile(filePath.toFile(), "r");
                                 FileChannel channel = reader.getChannel()) {
                                for (; offset < fileSize && requestStream.isReady(); offset += size) {
                                    int length = (int)(fileSize - offset);
                                    if (length > size)
                                        length = size;
                                    ByteBuffer buffer = ByteBuffer.allocate(length);
                                    channel.read(buffer, offset);
                                    ByteString string = ByteString.copyFrom(buffer.array());
                                    GenericFileChunk.Builder builder = GenericFileChunk.newBuilder()
                                            .setFilename(response.getFilename())
                                            .setName(response.getJob())
                                            .setData(string);
                                    requestStream.onNext(builder.build());
                                    //an error occurred
                                    if (finishedLatch.getCount() == 0)
                                        break;
                                }
                            }
                            catch (IOException e) {
                                WISELogger.getSpecial(LogName.Backend).error("Failed to read file transfer data.", e);
                            }
                            
                            //the file transfer has completed
                            if (offset > fileSize && !isComplete) {
                                isComplete = true;
                                requestStream.onCompleted();
                            }
                        }
                        catch (Exception e) {
                            WISELogger.getSpecial(LogName.Backend).error("Error transfering file data.", e);
                            throw e;
                        }
                    }
                };
                requestStream.setOnReadyHandler(drain);
            }
        };

        @SuppressWarnings("unused")
        StreamObserver<GenericFileChunk> requestObserver = asyncStub.withCompression("gzip").sendGenericFile(responseObserver);
        
        if (!finishedLatch.await(15, TimeUnit.MINUTES)) {
            WISELogger.getSpecial(LogName.Backend).warn("An error may have occurred while transferring file data, the connection is taking a long time");
        }
        
        if (wasError.get()) {
            FileStreamResponse.Builder builder = FileStreamResponse.newBuilder()
                    .setFilename(response.getFilename())
                    .setJob(response.getJob())
                    .setOffset(response.getOffset());
            client.handleFileStreamRequest(managerId, builder.build());
        }
    }
}
