// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: file_transfer.proto

package ca.wise.rpc.proto;

public final class GenericFileServerProto {
  private GenericFileServerProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rpc_GenericFileChunk_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rpc_GenericFileChunk_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rpc_GenericFileResult_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rpc_GenericFileResult_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\023file_transfer.proto\022\003rpc\"@\n\020GenericFil" +
      "eChunk\022\014\n\004name\030\001 \001(\t\022\020\n\010filename\030\002 \001(\t\022\014" +
      "\n\004data\030\003 \001(\014\"\023\n\021GenericFileResult2Y\n\021Gen" +
      "ericFileServer\022D\n\017SendGenericFile\022\025.rpc." +
      "GenericFileChunk\032\026.rpc.GenericFileResult" +
      "\"\000(\001B-\n\021ca.wise.rpc.protoB\026GenericFileSe" +
      "rverProtoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_rpc_GenericFileChunk_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_rpc_GenericFileChunk_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rpc_GenericFileChunk_descriptor,
        new java.lang.String[] { "Name", "Filename", "Data", });
    internal_static_rpc_GenericFileResult_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_rpc_GenericFileResult_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rpc_GenericFileResult_descriptor,
        new java.lang.String[] { });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
