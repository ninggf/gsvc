// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: greeting_service.proto

package com.apzda.cloud.gsvc.proto;

/**
 * Protobuf type {@code inventory.proto.HelloRequest}
 */
public final class HelloRequest extends com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:inventory.proto.HelloRequest)
        HelloRequestOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use HelloRequest.newBuilder() to construct.
    private HelloRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private HelloRequest() {
        name_ = "";
        file_ = com.google.protobuf.ByteString.EMPTY;
    }

    @Override
    @SuppressWarnings({ "unused" })
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new HelloRequest();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return GreetingServiceProto.internal_static_inventory_proto_HelloRequest_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return GreetingServiceProto.internal_static_inventory_proto_HelloRequest_fieldAccessorTable
            .ensureFieldAccessorsInitialized(HelloRequest.class, Builder.class);
    }

    public static final int NAME_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object name_ = "";

    /**
     * <code>string name = 1;</code>
     * @return The name.
     */
    @Override
    public String getName() {
        Object ref = name_;
        if (ref instanceof String) {
            return (String) ref;
        }
        else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            name_ = s;
            return s;
        }
    }

    /**
     * <code>string name = 1;</code>
     * @return The bytes for name.
     */
    @Override
    public com.google.protobuf.ByteString getNameBytes() {
        Object ref = name_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            name_ = b;
            return b;
        }
        else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int FILE_FIELD_NUMBER = 2;

    private com.google.protobuf.ByteString file_ = com.google.protobuf.ByteString.EMPTY;

    /**
     * <code>bytes file = 2;</code>
     * @return The file.
     */
    @Override
    public com.google.protobuf.ByteString getFile() {
        return file_;
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1)
            return true;
        if (isInitialized == 0)
            return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(name_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
        }
        if (!file_.isEmpty()) {
            output.writeBytes(2, file_);
        }
        getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(name_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
        }
        if (!file_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream.computeBytesSize(2, file_);
        }
        size += getUnknownFields().getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HelloRequest)) {
            return super.equals(obj);
        }
        HelloRequest other = (HelloRequest) obj;

        if (!getName().equals(other.getName()))
            return false;
        if (!getFile().equals(other.getFile()))
            return false;
        if (!getUnknownFields().equals(other.getUnknownFields()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + NAME_FIELD_NUMBER;
        hash = (53 * hash) + getName().hashCode();
        hash = (37 * hash) + FILE_FIELD_NUMBER;
        hash = (53 * hash) + getFile().hashCode();
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static HelloRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static HelloRequest parseFrom(java.nio.ByteBuffer data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static HelloRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static HelloRequest parseFrom(com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static HelloRequest parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static HelloRequest parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static HelloRequest parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static HelloRequest parseFrom(java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static HelloRequest parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static HelloRequest parseDelimitedFrom(java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static HelloRequest parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static HelloRequest parseFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(HelloRequest prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code inventory.proto.HelloRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:inventory.proto.HelloRequest)
            HelloRequestOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return GreetingServiceProto.internal_static_inventory_proto_HelloRequest_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return GreetingServiceProto.internal_static_inventory_proto_HelloRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(HelloRequest.class, Builder.class);
        }

        // Construct using com.apzda.cloud.gsvc.proto.HelloRequest.newBuilder()
        private Builder() {

        }

        private Builder(BuilderParent parent) {
            super(parent);

        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            name_ = "";
            file_ = com.google.protobuf.ByteString.EMPTY;
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return GreetingServiceProto.internal_static_inventory_proto_HelloRequest_descriptor;
        }

        @Override
        public HelloRequest getDefaultInstanceForType() {
            return HelloRequest.getDefaultInstance();
        }

        @Override
        public HelloRequest build() {
            HelloRequest result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public HelloRequest buildPartial() {
            HelloRequest result = new HelloRequest(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(HelloRequest result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.name_ = name_;
            }
            if (((from_bitField0_ & 0x00000002) != 0)) {
                result.file_ = file_;
            }
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof HelloRequest) {
                return mergeFrom((HelloRequest) other);
            }
            else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(HelloRequest other) {
            if (other == HelloRequest.getDefaultInstance())
                return this;
            if (!other.getName().isEmpty()) {
                name_ = other.name_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            if (other.getFile() != com.google.protobuf.ByteString.EMPTY) {
                setFile(other.getFile());
            }
            this.mergeUnknownFields(other.getUnknownFields());
            onChanged();
            return this;
        }

        @Override
        public final boolean isInitialized() {
            return true;
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            if (extensionRegistry == null) {
                throw new NullPointerException();
            }
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10: {
                            name_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
                        case 18: {
                            file_ = input.readBytes();
                            bitField0_ |= 0x00000002;
                            break;
                        } // case 18
                        default: {
                            if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                                done = true; // was an endgroup tag
                            }
                            break;
                        } // default:
                    } // switch (tag)
                } // while (!done)
            }
            catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.unwrapIOException();
            }
            finally {
                onChanged();
            } // finally
            return this;
        }

        private int bitField0_;

        private Object name_ = "";

        /**
         * <code>string name = 1;</code>
         * @return The name.
         */
        public String getName() {
            Object ref = name_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                name_ = s;
                return s;
            }
            else {
                return (String) ref;
            }
        }

        /**
         * <code>string name = 1;</code>
         * @return The bytes for name.
         */
        public com.google.protobuf.ByteString getNameBytes() {
            Object ref = name_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                name_ = b;
                return b;
            }
            else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string name = 1;</code>
         * @param value The name to set.
         * @return This builder for chaining.
         */
        public Builder setName(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            name_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <code>string name = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearName() {
            name_ = getDefaultInstance().getName();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <code>string name = 1;</code>
         * @param value The bytes for name to set.
         * @return This builder for chaining.
         */
        public Builder setNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            name_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        private com.google.protobuf.ByteString file_ = com.google.protobuf.ByteString.EMPTY;

        /**
         * <code>bytes file = 2;</code>
         * @return The file.
         */
        @Override
        public com.google.protobuf.ByteString getFile() {
            return file_;
        }

        /**
         * <code>bytes file = 2;</code>
         * @param value The file to set.
         * @return This builder for chaining.
         */
        public Builder setFile(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            file_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        /**
         * <code>bytes file = 2;</code>
         * @return This builder for chaining.
         */
        public Builder clearFile() {
            bitField0_ = (bitField0_ & ~0x00000002);
            file_ = getDefaultInstance().getFile();
            onChanged();
            return this;
        }

        @Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }

        // @@protoc_insertion_point(builder_scope:inventory.proto.HelloRequest)

    }

    // @@protoc_insertion_point(class_scope:inventory.proto.HelloRequest)
    private static final HelloRequest DEFAULT_INSTANCE;
    static {
        DEFAULT_INSTANCE = new HelloRequest();
    }

    public static HelloRequest getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<HelloRequest> PARSER = new com.google.protobuf.AbstractParser<HelloRequest>() {
        @Override
        public HelloRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
                builder.mergeFrom(input, extensionRegistry);
            }
            catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(builder.buildPartial());
            }
            catch (com.google.protobuf.UninitializedMessageException e) {
                throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
            }
            catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(e)
                    .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
        }
    };

    public static com.google.protobuf.Parser<HelloRequest> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<HelloRequest> getParserForType() {
        return PARSER;
    }

    @Override
    public HelloRequest getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}
