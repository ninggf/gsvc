package com.apzda.cloud.gsvc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(value = "by gRPC proto compiler (version 1.57.1)",
        comments = "Source: greeting_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GreetingServiceGrpc {

    private GreetingServiceGrpc() {
    }

    public static final String SERVICE_NAME = "inventory.proto.GreetingService";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHelloMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "sayHello",
            requestType = HelloRequest.class, responseType = HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHelloMethod() {
        io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHelloMethod;
        if ((getSayHelloMethod = GreetingServiceGrpc.getSayHelloMethod) == null) {
            synchronized (GreetingServiceGrpc.class) {
                if ((getSayHelloMethod = GreetingServiceGrpc.getSayHelloMethod) == null) {
                    GreetingServiceGrpc.getSayHelloMethod = getSayHelloMethod = io.grpc.MethodDescriptor
                        .<HelloRequest, HelloResponse>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sayHello"))
                        .setSampledToLocalTracing(true)
                        .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(HelloRequest.getDefaultInstance()))
                        .setResponseMarshaller(
                                io.grpc.protobuf.ProtoUtils.marshaller(HelloResponse.getDefaultInstance()))
                        .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("sayHello"))
                        .build();
                }
            }
        }
        return getSayHelloMethod;
    }

    private static volatile io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHeiMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "sayHei",
            requestType = HelloRequest.class, responseType = HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
    public static io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHeiMethod() {
        io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHeiMethod;
        if ((getSayHeiMethod = GreetingServiceGrpc.getSayHeiMethod) == null) {
            synchronized (GreetingServiceGrpc.class) {
                if ((getSayHeiMethod = GreetingServiceGrpc.getSayHeiMethod) == null) {
                    GreetingServiceGrpc.getSayHeiMethod = getSayHeiMethod = io.grpc.MethodDescriptor
                        .<HelloRequest, HelloResponse>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                        .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sayHei"))
                        .setSampledToLocalTracing(true)
                        .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(HelloRequest.getDefaultInstance()))
                        .setResponseMarshaller(
                                io.grpc.protobuf.ProtoUtils.marshaller(HelloResponse.getDefaultInstance()))
                        .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("sayHei"))
                        .build();
                }
            }
        }
        return getSayHeiMethod;
    }

    private static volatile io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHiMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "sayHi", requestType = HelloRequest.class,
            responseType = HelloResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
    public static io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHiMethod() {
        io.grpc.MethodDescriptor<HelloRequest, HelloResponse> getSayHiMethod;
        if ((getSayHiMethod = GreetingServiceGrpc.getSayHiMethod) == null) {
            synchronized (GreetingServiceGrpc.class) {
                if ((getSayHiMethod = GreetingServiceGrpc.getSayHiMethod) == null) {
                    GreetingServiceGrpc.getSayHiMethod = getSayHiMethod = io.grpc.MethodDescriptor
                        .<HelloRequest, HelloResponse>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
                        .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sayHi"))
                        .setSampledToLocalTracing(true)
                        .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(HelloRequest.getDefaultInstance()))
                        .setResponseMarshaller(
                                io.grpc.protobuf.ProtoUtils.marshaller(HelloResponse.getDefaultInstance()))
                        .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("sayHi"))
                        .build();
                }
            }
        }
        return getSayHiMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static GreetingServiceStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreetingServiceStub> factory = new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceStub>() {
            @Override
            public GreetingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new GreetingServiceStub(channel, callOptions);
            }
        };
        return GreetingServiceStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on
     * the service
     */
    public static GreetingServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreetingServiceBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceBlockingStub>() {
            @Override
            public GreetingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new GreetingServiceBlockingStub(channel, callOptions);
            }
        };
        return GreetingServiceBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static GreetingServiceFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreetingServiceFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceFutureStub>() {
            @Override
            public GreetingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new GreetingServiceFutureStub(channel, callOptions);
            }
        };
        return GreetingServiceFutureStub.newStub(factory, channel);
    }

    /**
     */
    public interface AsyncService {

        /**
         * <pre>
         * unary
         * </pre>
         */
        default void sayHello(HelloRequest request, io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHelloMethod(), responseObserver);
        }

        /**
         * <pre>
         * server-streaming
         * </pre>
         */
        default void sayHei(HelloRequest request, io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHeiMethod(), responseObserver);
        }

        /**
         * <pre>
         * bidirectional-streaming
         * </pre>
         */
        default io.grpc.stub.StreamObserver<HelloRequest> sayHi(
                io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSayHiMethod(), responseObserver);
        }

    }

    /**
     * Base class for the server implementation of the service GreetingService.
     */
    public static abstract class GreetingServiceImplBase implements io.grpc.BindableService, AsyncService {

        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return GreetingServiceGrpc.bindService(this);
        }

    }

    /**
     * A stub to allow clients to do asynchronous rpc calls to service GreetingService.
     */
    public static final class GreetingServiceStub extends io.grpc.stub.AbstractAsyncStub<GreetingServiceStub> {

        private GreetingServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreetingServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreetingServiceStub(channel, callOptions);
        }

        /**
         * <pre>
         * unary
         * </pre>
         */
        public void sayHello(HelloRequest request, io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(getChannel().newCall(getSayHelloMethod(), getCallOptions()),
                    request, responseObserver);
        }

        /**
         * <pre>
         * server-streaming
         * </pre>
         */
        public void sayHei(HelloRequest request, io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncServerStreamingCall(getChannel().newCall(getSayHeiMethod(), getCallOptions()),
                    request, responseObserver);
        }

        /**
         * <pre>
         * bidirectional-streaming
         * </pre>
         */
        public io.grpc.stub.StreamObserver<HelloRequest> sayHi(
                io.grpc.stub.StreamObserver<HelloResponse> responseObserver) {
            return io.grpc.stub.ClientCalls
                .asyncBidiStreamingCall(getChannel().newCall(getSayHiMethod(), getCallOptions()), responseObserver);
        }

    }

    /**
     * A stub to allow clients to do synchronous rpc calls to service GreetingService.
     */
    public static final class GreetingServiceBlockingStub
            extends io.grpc.stub.AbstractBlockingStub<GreetingServiceBlockingStub> {

        private GreetingServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreetingServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreetingServiceBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * unary
         * </pre>
         */
        public HelloResponse sayHello(HelloRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(getChannel(), getSayHelloMethod(), getCallOptions(),
                    request);
        }

        /**
         * <pre>
         * server-streaming
         * </pre>
         */
        public java.util.Iterator<HelloResponse> sayHei(HelloRequest request) {
            return io.grpc.stub.ClientCalls.blockingServerStreamingCall(getChannel(), getSayHeiMethod(),
                    getCallOptions(), request);
        }

    }

    /**
     * A stub to allow clients to do ListenableFuture-style rpc calls to service
     * GreetingService.
     */
    public static final class GreetingServiceFutureStub
            extends io.grpc.stub.AbstractFutureStub<GreetingServiceFutureStub> {

        private GreetingServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreetingServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreetingServiceFutureStub(channel, callOptions);
        }

        /**
         * <pre>
         * unary
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<HelloResponse> sayHello(HelloRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(getChannel().newCall(getSayHelloMethod(), getCallOptions()),
                    request);
        }

    }

    private static final int METHODID_SAY_HELLO = 0;

    private static final int METHODID_SAY_HEI = 1;

    private static final int METHODID_SAY_HI = 2;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final AsyncService serviceImpl;

        private final int methodId;

        MethodHandlers(AsyncService serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_SAY_HELLO:
                    serviceImpl.sayHello((HelloRequest) request,
                            (io.grpc.stub.StreamObserver<HelloResponse>) responseObserver);
                    break;
                case METHODID_SAY_HEI:
                    serviceImpl.sayHei((HelloRequest) request,
                            (io.grpc.stub.StreamObserver<HelloResponse>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_SAY_HI:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl
                        .sayHi((io.grpc.stub.StreamObserver<HelloResponse>) responseObserver);
                default:
                    throw new AssertionError();
            }
        }

    }

    public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
        return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
            .addMethod(getSayHelloMethod(),
                    io.grpc.stub.ServerCalls
                        .asyncUnaryCall(new MethodHandlers<HelloRequest, HelloResponse>(service, METHODID_SAY_HELLO)))
            .addMethod(getSayHeiMethod(),
                    io.grpc.stub.ServerCalls.asyncServerStreamingCall(
                            new MethodHandlers<HelloRequest, HelloResponse>(service, METHODID_SAY_HEI)))
            .addMethod(getSayHiMethod(),
                    io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
                            new MethodHandlers<HelloRequest, HelloResponse>(service, METHODID_SAY_HI)))
            .build();
    }

    private static abstract class GreetingServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {

        GreetingServiceBaseDescriptorSupplier() {
        }

        @Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return GreetingServiceProto.getDescriptor();
        }

        @Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("GreetingService");
        }

    }

    private static final class GreetingServiceFileDescriptorSupplier extends GreetingServiceBaseDescriptorSupplier {

        GreetingServiceFileDescriptorSupplier() {
        }

    }

    private static final class GreetingServiceMethodDescriptorSupplier extends GreetingServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

        private final String methodName;

        GreetingServiceMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }

    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (GreetingServiceGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                        .setSchemaDescriptor(new GreetingServiceFileDescriptorSupplier())
                        .addMethod(getSayHelloMethod())
                        .addMethod(getSayHeiMethod())
                        .addMethod(getSayHiMethod())
                        .build();
                }
            }
        }
        return result;
    }

}
