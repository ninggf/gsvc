/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.apzda.cloud.gsvc.proto;

public final class GreetingServiceGsvc {

    private static final java.util.Map<String, Object[]> METHOD_META_INFO = new java.util.HashMap<>();

    static {
        METHOD_META_INFO.put("sayHello",
                new Object[] { io.grpc.MethodDescriptor.MethodType.UNARY, HelloRequest.class, HelloResponse.class });
        METHOD_META_INFO.put("sayHei",
                new Object[] { io.grpc.MethodDescriptor.MethodType.UNARY, HelloRequest.class, HelloResponse.class });
        METHOD_META_INFO.put("sayHi",
                new Object[] { io.grpc.MethodDescriptor.MethodType.UNARY, HelloRequest.class, HelloResponse.class });
    }

    private GreetingServiceGsvc() {
    }

    public static Object[] getMetadata(String methodName) throws NoSuchMethodException {
        Object[] objects = METHOD_META_INFO.get(methodName);
        if (objects != null) {
            return objects;
        }
        throw new NoSuchMethodException(methodName);
    }

}