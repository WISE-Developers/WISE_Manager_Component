package ca.wise.rpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * Server methods for receiving fgm files.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.26.0)",
    comments = "Source: file_transfer.proto")
public final class GenericFileServerGrpc {

  private GenericFileServerGrpc() {}

  public static final String SERVICE_NAME = "rpc.GenericFileServer";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ca.wise.rpc.proto.GenericFileChunk,
      ca.wise.rpc.proto.GenericFileResult> getSendGenericFileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendGenericFile",
      requestType = ca.wise.rpc.proto.GenericFileChunk.class,
      responseType = ca.wise.rpc.proto.GenericFileResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<ca.wise.rpc.proto.GenericFileChunk,
      ca.wise.rpc.proto.GenericFileResult> getSendGenericFileMethod() {
    io.grpc.MethodDescriptor<ca.wise.rpc.proto.GenericFileChunk, ca.wise.rpc.proto.GenericFileResult> getSendGenericFileMethod;
    if ((getSendGenericFileMethod = GenericFileServerGrpc.getSendGenericFileMethod) == null) {
      synchronized (GenericFileServerGrpc.class) {
        if ((getSendGenericFileMethod = GenericFileServerGrpc.getSendGenericFileMethod) == null) {
          GenericFileServerGrpc.getSendGenericFileMethod = getSendGenericFileMethod =
              io.grpc.MethodDescriptor.<ca.wise.rpc.proto.GenericFileChunk, ca.wise.rpc.proto.GenericFileResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendGenericFile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ca.wise.rpc.proto.GenericFileChunk.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ca.wise.rpc.proto.GenericFileResult.getDefaultInstance()))
              .setSchemaDescriptor(new GenericFileServerMethodDescriptorSupplier("SendGenericFile"))
              .build();
        }
      }
    }
    return getSendGenericFileMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GenericFileServerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericFileServerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericFileServerStub>() {
        @java.lang.Override
        public GenericFileServerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericFileServerStub(channel, callOptions);
        }
      };
    return GenericFileServerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GenericFileServerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericFileServerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericFileServerBlockingStub>() {
        @java.lang.Override
        public GenericFileServerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericFileServerBlockingStub(channel, callOptions);
        }
      };
    return GenericFileServerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GenericFileServerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericFileServerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericFileServerFutureStub>() {
        @java.lang.Override
        public GenericFileServerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericFileServerFutureStub(channel, callOptions);
        }
      };
    return GenericFileServerFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static abstract class GenericFileServerImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Stream a generic file from the client to the server.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<ca.wise.rpc.proto.GenericFileChunk> sendGenericFile(
        io.grpc.stub.StreamObserver<ca.wise.rpc.proto.GenericFileResult> responseObserver) {
      return asyncUnimplementedStreamingCall(getSendGenericFileMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendGenericFileMethod(),
            asyncClientStreamingCall(
              new MethodHandlers<
                ca.wise.rpc.proto.GenericFileChunk,
                ca.wise.rpc.proto.GenericFileResult>(
                  this, METHODID_SEND_GENERIC_FILE)))
          .build();
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class GenericFileServerStub extends io.grpc.stub.AbstractAsyncStub<GenericFileServerStub> {
    private GenericFileServerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericFileServerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericFileServerStub(channel, callOptions);
    }

    /**
     * <pre>
     * Stream a generic file from the client to the server.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<ca.wise.rpc.proto.GenericFileChunk> sendGenericFile(
        io.grpc.stub.StreamObserver<ca.wise.rpc.proto.GenericFileResult> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(getSendGenericFileMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class GenericFileServerBlockingStub extends io.grpc.stub.AbstractBlockingStub<GenericFileServerBlockingStub> {
    private GenericFileServerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericFileServerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericFileServerBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class GenericFileServerFutureStub extends io.grpc.stub.AbstractFutureStub<GenericFileServerFutureStub> {
    private GenericFileServerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericFileServerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericFileServerFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SEND_GENERIC_FILE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GenericFileServerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GenericFileServerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_GENERIC_FILE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sendGenericFile(
              (io.grpc.stub.StreamObserver<ca.wise.rpc.proto.GenericFileResult>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class GenericFileServerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GenericFileServerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ca.wise.rpc.proto.GenericFileServerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GenericFileServer");
    }
  }

  private static final class GenericFileServerFileDescriptorSupplier
      extends GenericFileServerBaseDescriptorSupplier {
    GenericFileServerFileDescriptorSupplier() {}
  }

  private static final class GenericFileServerMethodDescriptorSupplier
      extends GenericFileServerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GenericFileServerMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GenericFileServerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GenericFileServerFileDescriptorSupplier())
              .addMethod(getSendGenericFileMethod())
              .build();
        }
      }
    }
    return result;
  }
}
