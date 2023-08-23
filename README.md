# APZDA CLOUD GSVC

基于`spring-cloud-gateway`的小玩具。

## 使用

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.apzda.cloud</groupId>
        <artifactId>apzda-cloud-gsvc-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <groupId>com.your.group-id</groupId>
    <artifactId>your.artifact-id</artifactId>
    <properties>
        <!-- your properties -->
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.apzda.cloud</groupId>
            <artifactId>apzda-cloud-gsvc-starter</artifactId>
        </dependency>
        <!-- your dependencies -->
    </dependencies>
    <build>
        <!-- your build -->
    </build>
</project>
```