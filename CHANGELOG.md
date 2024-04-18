# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.19] - 2024-04-18

### Added

- do validation for server-streaming call

### Changed

- use application name as default service name

### Fixed

- convert Wildcard MediaType to a specified type

### Improved

- File upload

## [1.2.18] - 2024-04-17

### Added

- Add gsvc.serviceName option
- apzda-cloud-gsvc-test module
    - @GsvcTest
    - @AutoConfigureGsvcTest
- xxl-job-adapter module
- Configuration properties
    - apzda.cloud.config.real-ip-header: GsvcContextHolder.getRemoteIp use to get remote Ip.
    - apzda.cloud.config.real-ip-from: only accept real-ip-header form which the request comes.
    - apzda.cloud.config.flat-response: do not wrap the response.
- GsvcContextHolder.current
- IForwardPlugin interface
- I18nUtils now supports message as key

### Changed

- Spring boot upgraded to 3.2.4
- Spring Cloud upgraded to 2023.0.1
- I18nHelper renamed to I18nUtils
- Dependencies upgraded to their latest version

### Improved

- Service Name Resolve mechanism
- Gateway, forwarding request to downstream instead of RPC.
- RPC Mechanism, using Stub instead of Reflection.
- Tracing
- Sentinel
