package ca.wise.rpc;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.Settings;
import ca.wise.lib.json.JobRequest;
import ca.wise.lib.json.JobResponse;
import ca.wise.lib.mqtt.MqttListener;
import ca.wise.rpc.proto.FileChunk;
import ca.wise.rpc.proto.FileResult;
import ca.wise.rpc.proto.FileServerGrpc;
import ca.wise.rpc.proto.FileServerGrpc.FileServerStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

public class RPCClient {

    private final MqttListener client;
    private final JobResponse response;
    private final String managerId;
    private final JobRequest job;
    private ManagedChannel channel;
    private FileServerStub asyncStub;
    @Getter private boolean running = false;
    
    public RPCClient(MqttListener client, JobResponse response, JobRequest job, String managerId) {
        this.client = client;
        this.response = response;
        this.managerId = managerId;
        this.job = job;
    }
    
    public void runAsync() {
        new Thread(() -> {
            try {
                start();
                sendFgmFileAsync();
            }
            catch (Exception e) {
                WISELogger.getSpecial(LogName.Backend).error("Error submitting job via RPC", e);
            }
            finally {
                try {
                    shutdown();
                }
                catch (Exception e) {
                    WISELogger.getSpecial(LogName.Backend).error("Error shutting down RPC", e);
                }
            }
        }).start();
    }
    
    public void start() {
        String rpcAddress;
        //if the remote server specified both an internal and external address
        if (!Strings.isNullOrEmpty(response.rpcInternalAddress)) {
            //get my address
            String myAddress = Settings.getRpcAddress();
            //if I know my address
            if (!Strings.isNullOrEmpty(myAddress)) {
                //remove the port from the end of the remote external address
                String externalAddress;
                int index = response.rpcAddress.lastIndexOf(':');
                if (index > 0)
                    externalAddress = response.rpcAddress.substring(0, index);
                else
                    externalAddress = response.rpcAddress;
                //if my external address is the same as the remote external address use the internal address
                if (myAddress.equals(externalAddress))
                    rpcAddress = response.rpcInternalAddress;
                //if my external address is different than the remote external address use the external address
                else
                    rpcAddress = response.rpcAddress;
            }
            //if I don't know my address just use the remote external address
            else
                rpcAddress = response.rpcAddress;
        }
        //if the remote server only has an external address use that
        else
            rpcAddress = response.rpcAddress;
        
        running = true;
        channel = ManagedChannelBuilder
                    .forTarget(rpcAddress)
                    .usePlaintext()
                    .build();
        asyncStub = FileServerGrpc.newStub(channel);
    }
    
    public void shutdown() throws InterruptedException {
        if (channel != null && !channel.isShutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    
    public void sendFgmFileAsync() throws Exception {
        CountDownLatch finishedLatch = new CountDownLatch(1);
        AtomicBoolean wasError = new AtomicBoolean(false);
        
        FileChunk.OutputType type = FileChunk.OutputType.UNRECOGNIZED;
        if (job.extension.equalsIgnoreCase("fgmj"))
            type = FileChunk.OutputType.JSON_PRETTY;
        else
            type = FileChunk.OutputType.BINARY;
        final FileChunk.OutputType typ = type;
        
        ClientResponseObserver<FileChunk, FileResult> responseObserver = new ClientResponseObserver<FileChunk, FileResult>() {
            
            @Override
            public void onNext(FileResult value) {
              //the server doesn't return anything at this time
            }
            
            @Override
            public void onError(Throwable t) {
                WISELogger.getSpecial(LogName.Backend).error("Failed to send FGM file data.", t);
                wasError.set(true);
                finishedLatch.countDown();
            }
            
            @Override
            public void onCompleted() {
                finishedLatch.countDown();
            }
            
            @Override
            public void beforeStart(final ClientCallStreamObserver<FileChunk> requestStream) {
                Runnable drain = new Runnable() {
                    int offset = 0;
                    boolean isComplete = false;
                    long fileSize = -1;
                    
                    @Override
                    public void run() {
                        try {
                            final int size = 2097152;//2MiB
                            if (fileSize < 0) {
                                fileSize = Files.size(job.jobFile);
                                //WISELogger.getSpecial(LogName.Backend).warn("Sending " + fileSize + "B file " + job.jobFile + " to " + rpcAddress);
                            }
                            
                            for (; offset < fileSize && requestStream.isReady(); offset += size) {
                                try (RandomAccessFile reader = new RandomAccessFile(job.jobFile.toFile(), "r");
                                        FileChannel channel = reader.getChannel()) {
                                    int length = (int)(fileSize - offset);
                                    if (length > size)
                                        length = size;
                                    ByteBuffer bytes = ByteBuffer.allocate(length);
                                    channel.read(bytes, offset);
                                    ByteString string = ByteString.copyFrom(bytes.array());
                                    FileChunk.Builder builder = FileChunk.newBuilder()
                                            .setType(typ)
                                            .setName(job.jobName)
                                            .setData(string);
                                    requestStream.onNext(builder.build());
                                    //an error occurred
                                    if (finishedLatch.getCount() == 0)
                                        break;
                                }
                            }

                            if (offset >= fileSize && !isComplete) {
                                isComplete = true;
                                requestStream.onCompleted();
                            }
                        }
                        catch (Exception e) {
                            WISELogger.getSpecial(LogName.Backend).error("Error transfering FGM file data.", e);
                        }
                        
                    }
                };
                requestStream.setOnReadyHandler(drain);
            }
        };
        
        @SuppressWarnings("unused") StreamObserver<FileChunk> requestObserver = asyncStub.withCompression("gzip").sendFile(responseObserver);
        
        if (!finishedLatch.await(5, TimeUnit.MINUTES)) {
            WISELogger.getSpecial(LogName.Backend).warn("An error may have occurred while transferring FGM data, the connection is taking a long time");
        }
        
        if (wasError.get())
            client.onRPCSendError(job.jobName, managerId, response);
        else {
            try {
                client.onRPCSendComplete(job.jobName, managerId);
            }
            catch (Exception e) {
                WISELogger.getSpecial(LogName.Backend).error("Failed to send job start signal after RPC transfer", e);
            }
        }
    }
}
