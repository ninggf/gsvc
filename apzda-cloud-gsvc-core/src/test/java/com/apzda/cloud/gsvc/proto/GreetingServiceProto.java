// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: greeting_service.proto

package com.apzda.cloud.gsvc.proto;

public final class GreetingServiceProto {

    private GreetingServiceProto() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_inventory_proto_HelloRequest_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_inventory_proto_HelloRequest_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor internal_static_inventory_proto_HelloResponse_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_inventory_proto_HelloResponse_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
    static {
        String[] descriptorData = { "\n\026greeting_service.proto\022\017inventory.prot"
                + "o\"*\n\014HelloRequest\022\014\n\004name\030\001 \001(\t\022\014\n\004file\030"
                + "\002 \001(\014\"@\n\rHelloResponse\022\020\n\010err_code\030\001 \001(\005"
                + "\022\017\n\007err_msg\030\002 \001(\t\022\014\n\004name\030\003 \001(\t2\363\001\n\017Gree"
                + "tingService\022I\n\010sayHello\022\035.inventory.prot"
                + "o.HelloRequest\032\036.inventory.proto.HelloRe"
                + "sponse\022I\n\006sayHei\022\035.inventory.proto.Hello"
                + "Request\032\036.inventory.proto.HelloResponse0"
                + "\001\022J\n\005sayHi\022\035.inventory.proto.HelloReques"
                + "t\032\036.inventory.proto.HelloResponse(\0010\001B4\n"
                + "\032com.apzda.cloud.gsvc.protoB\024GreetingSer" + "viceProtoP\001b\006proto3" };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
                new com.google.protobuf.Descriptors.FileDescriptor[] {});
        internal_static_inventory_proto_HelloRequest_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_inventory_proto_HelloRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_inventory_proto_HelloRequest_descriptor, new String[] { "Name", "File", });
        internal_static_inventory_proto_HelloResponse_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_inventory_proto_HelloResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_inventory_proto_HelloResponse_descriptor,
                new String[] { "ErrCode", "ErrMsg", "Name", });
    }

    // @@protoc_insertion_point(outer_class_scope)

}
