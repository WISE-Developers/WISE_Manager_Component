// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: file_server.proto

package ca.wise.rpc.proto;

public final class FileServerProto {
  private FileServerProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rpc_FileChunk_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rpc_FileChunk_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rpc_FileResult_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rpc_FileResult_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021file_server.proto\022\003rpc\"\226\001\n\tFileChunk\022\014" +
      "\n\004name\030\001 \001(\t\022\014\n\004data\030\002 \001(\014\022\'\n\004type\030\003 \001(\016" +
      "2\031.rpc.FileChunk.OutputType\"D\n\nOutputTyp" +
      "e\022\007\n\003XML\020\000\022\017\n\013JSON_PRETTY\020\001\022\020\n\014JSON_MINI" +
      "MAL\020\002\022\n\n\006BINARY\020\003\"\014\n\nFileResult2=\n\nFileS" +
      "erver\022/\n\010SendFile\022\016.rpc.FileChunk\032\017.rpc." +
      "FileResult\"\000(\001B&\n\021ca.wise.rpc.protoB\017Fil" +
      "eServerProtoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_rpc_FileChunk_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_rpc_FileChunk_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rpc_FileChunk_descriptor,
        new java.lang.String[] { "Name", "Data", "Type", });
    internal_static_rpc_FileResult_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_rpc_FileResult_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rpc_FileResult_descriptor,
        new java.lang.String[] { });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
