// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: manager_settings.proto

package ca.wise.settings.proto;

public final class ManagerSettingsOuterClass {
  private ManagerSettingsOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_WISE_settings_ManagerSettings_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_WISE_settings_ManagerSettings_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\026manager_settings.proto\022\rWISE.settings\032" +
      "\037google/protobuf/timestamp.proto\032\036google" +
      "/protobuf/wrappers.proto\"W\n\017ManagerSetti" +
      "ngs\022\024\n\014jobDirectory\030\003 \001(\t\022.\n\nlastUpdate\030" +
      "\013 \001(\0132\032.google.protobuf.TimestampB\032\n\026ca." +
      "wise.settings.protoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.TimestampProto.getDescriptor(),
          com.google.protobuf.WrappersProto.getDescriptor(),
        });
    internal_static_WISE_settings_ManagerSettings_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_WISE_settings_ManagerSettings_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_WISE_settings_ManagerSettings_descriptor,
        new java.lang.String[] { "JobDirectory", "LastUpdate", });
    com.google.protobuf.TimestampProto.getDescriptor();
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
