// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: greeting_service.proto

package com.apzda.cloud.gsvc.proto;

public interface HelloResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:inventory.proto.HelloResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int32 err_code = 1;</code>
   * @return The errCode.
   */
  int getErrCode();

  /**
   * <code>string err_msg = 2;</code>
   * @return The errMsg.
   */
  String getErrMsg();
  /**
   * <code>string err_msg = 2;</code>
   * @return The bytes for errMsg.
   */
  com.google.protobuf.ByteString
      getErrMsgBytes();

  /**
   * <code>string name = 3;</code>
   * @return The name.
   */
  String getName();
  /**
   * <code>string name = 3;</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();
}
