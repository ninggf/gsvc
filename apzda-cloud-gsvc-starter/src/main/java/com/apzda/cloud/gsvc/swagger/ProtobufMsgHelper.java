/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.swagger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.hubspot.jackson.datatype.protobuf.ExtensionRegistryWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class ProtobufMsgHelper {

    private static final Random r = new Random();

    public static Object create(Class<?> messageType) {
        return create(messageType, ExtensionRegistry.getEmptyRegistry());
    }

    public static Object create(Class<?> messageType, ExtensionRegistry extensionRegistry) {
        return new Creator().create(messageType, extensionRegistry);
    }

    private final static class Creator {

        private final Map<Class<?>, Message.Builder> partiallyBuilt = new HashMap<>();

        private Object create(Class<?> messageType, ExtensionRegistry extensionRegistry) {
            return create(messageType, ExtensionRegistryWrapper.wrap(extensionRegistry));
        }

        private Object create(Class<?> messageType, ExtensionRegistryWrapper extensionRegistry) {
            Message.Builder builder = newBuilder(messageType);
            partiallyBuilt.put(messageType, builder);
            populate(builder, extensionRegistry);
            return builder.build();
        }

        private static Message.Builder newBuilder(Class<?> messageType) {
            try {
                return (Message.Builder) messageType.getMethod("newBuilder").invoke(null);
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private void populate(Message.Builder builder, ExtensionRegistryWrapper extensionRegistry) {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();

            for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
                if (field.isRepeated()) {
                    builder.addRepeatedField(field, getValue(builder, field, null, extensionRegistry));
                }
                else {
                    builder.setField(field, getValue(builder, field, null, extensionRegistry));
                }
            }

            for (ExtensionRegistry.ExtensionInfo extensionInfo : extensionRegistry
                .getExtensionsByDescriptor(descriptor)) {
                Descriptors.FieldDescriptor extension = extensionInfo.descriptor;
                Message defaultInstance = extensionInfo.defaultInstance;
                if (extension.isRepeated()) {
                    builder.addRepeatedField(extension,
                            getValue(builder, extension, defaultInstance, extensionRegistry));
                }
                else {
                    builder.setField(extension, getValue(builder, extension, defaultInstance, extensionRegistry));
                }
            }
        }

        private Object getValue(Message.Builder builder, Descriptors.FieldDescriptor field, Message defaultInstance,
                ExtensionRegistryWrapper extensionRegistry) {
            switch (field.getJavaType()) {
                case INT:
                    return r.nextInt();
                case LONG:
                    return r.nextLong();
                case FLOAT:
                    return r.nextFloat();
                case DOUBLE:
                    return r.nextDouble();
                case BOOLEAN:
                    return r.nextBoolean();
                case STRING:
                    String available = "abcdefghijklmnopqrstuvwxyz0123456789";
                    int length = r.nextInt(20) + 1;
                    StringBuilder value = new StringBuilder();
                    for (int i = 0; i < length; i++) {
                        value.append(available.charAt(r.nextInt(available.length())));
                    }
                    return value.toString();
                case BYTE_STRING:
                    byte[] bytes = new byte[r.nextInt(20) + 1];
                    r.nextBytes(bytes);
                    return ByteString.copyFrom(bytes);
                case ENUM:
                    List<Descriptors.EnumValueDescriptor> values = field.getEnumType().getValues();
                    return values.get(r.nextInt(values.size()));
                case MESSAGE:
                    final Class<? extends Message> subMessageType;
                    if (field.isExtension()) {
                        subMessageType = defaultInstance.getClass();
                    }
                    else {
                        subMessageType = builder.newBuilderForField(field).getDefaultInstanceForType().getClass();
                    }

                    // Handle recursive relationships by returning a partially populated
                    // proto (better than an infinite loop)
                    if (partiallyBuilt.containsKey(subMessageType)) {
                        return partiallyBuilt.get(subMessageType).build();
                    }
                    else {
                        return create(subMessageType, extensionRegistry);
                    }
                default:
                    throw new IllegalArgumentException("Unrecognized field type: " + field.getJavaType());
            }
        }

    }

}
