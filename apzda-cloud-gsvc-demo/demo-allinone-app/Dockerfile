FROM wulaphp/openjdk:17 as builder

ARG SERVICE_JAR

COPY ${SERVICE_JAR} /opt/app/application.jar

RUN java -Djarmode=layertools -jar application.jar extract

FROM wulaphp/openjdk:17

ARG SERVICE_NAME
ARG SERVICE_VER

ENV JAVA_OPTS="-Xms512M -Xmx512M"\
    APP_OPTS="" \
    SERVICE_NAME="${SERVICE_NAME}"\
    SERVICE_VER="${SERVICE_VER}"\
    SERVER_PORT=8080

COPY --from=builder /opt/app/bin/ ./bin/
COPY --from=builder /opt/app/dependencies/ ./${SERVICE_NAME}/
COPY --from=builder /opt/app/company-dependencies/ ./${SERVICE_NAME}/
COPY --from=builder /opt/app/spring-boot-loader/ ./${SERVICE_NAME}/
COPY --from=builder /opt/app/snapshot-dependencies/ ./${SERVICE_NAME}/
COPY --from=builder /opt/app/application/ ./${SERVICE_NAME}/

CMD ["-mode","launcher"]
