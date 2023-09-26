# APZDA CLOUD GSVC

A Toy Based on `Spring Boot` and `Grpc-Java`.

> 它不是轮子，更不是新的框架！只是作者觉得（自以为是）OpenFeign的限制使得有些需求无法实现（可单体部署、又可以微服务部署），
> 然后整合`Spring Boot`, `Spring Cloud`与`Grpc-Java`基于[Protocol Buffers](https://protobuf.dev/)
> 搞的一个看上去解决了`OpenFeign`这些限制的小玩具, 顺带实现了一个小小的网关来隔离南北与东西流量。

## Feature

1. 单、微随你便: 打包在一起部署就是单体应用，分开部署就是微服务架构应用，不需要修改代码即可实现。
2. 流量隔离: 通过内置网关实现南北流量与东西流量隔离，让内部的流量仅内部可见。
3. 观测、流控与安全: 微调Spring Security提供安全保证、基于Spring Actuator提供可观性、整合Sentinel实现流控。

## 使用

### 1. 编写`portobuf`接口协议

请阅读**apzda-cloud-gsvc-demo/demo-protos**下的工程源码。

> 1. 建议使用独立的`maven`工程来定义接口与实现工程分离。
> 2. 只支持`proto3`。

> 特别说明:
> 1. 网关与基于WebClient客户端(类似OpenFeign的客户端)的调用只支持: **UNARY**和**SERVER-STREAMING**。
> 2. 网关会将**SERVER-STREAMING**方法转成**SSE(Server-Sent Events)**, 如果该方法是南北流量，请保证客户端可以处理。
> 3. 东西流量使用grpc调用时，支持所有种类(**UNARY**, **SERVER-STREAMING**, **CLIENT-STREAMING**, **BIDI-STREAMING**)的方法。

### 2. 实现接口协议

请阅读**apzda-cloud-gsvc-demo**下的工程源码。

### 3. 远程调用（微服务架构）

请阅读**apzda-cloud-gsvc-demo**下的工程源码。

调用关系如下:
<pre>
      foo 
    ↗  ↓
demo → bar → math
</pre>

### 4. 单体应用

请阅读**apzda-cloud-gsvc-demo/demo-allinone-app**工程的源码。

### 运行DEMO

1. 将工程导入到**IntelliJ IDEA**。
2. 将`gsvc-compiler` 安装(或`mvn install`)到本地。
3. 执行编译(或`mvn compile`)。
4. 在本地启用一个redis服务器（或改配置连远程redis）。
5. 启动服务: math,foo,bar,demo
    * 打开`bar-test.http`，`foo-test.http`，`demo-test.http`。 环境选`dev`，然后执行。
6. 启动: math,allinone
    * 打开`bar-test.http`，`foo-test.http`。 环境选`sit`,然后执行。

## 文档

文档[传送门](https://gsvc.apzda.com)。

## 参与

1. 欢迎`PR`。
2. 欢迎提`ISSUE`。
3. 欢迎提建议(意见请保留)。
