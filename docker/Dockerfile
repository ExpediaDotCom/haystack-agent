FROM openjdk:8-jre
MAINTAINER Haystack <haystack@expedia.com>

ENV APP_NAME haystack-agent
ENV APP_HOME /app/bin

RUN mkdir -p ${APP_HOME}

RUN GRPC_HEALTH_PROBE_VERSION=v0.2.0 && \
    wget -qO/bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64 && \
    chmod +x /bin/grpc_health_probe

COPY bundlers/haystack-agent/target/${APP_NAME}.jar ${APP_HOME}/

COPY docker/default.conf ${APP_HOME}/
COPY docker/start-app.sh ${APP_HOME}/

RUN chmod +x ${APP_HOME}/start-app.sh

WORKDIR ${APP_HOME}

ENTRYPOINT ["./start-app.sh"]

CMD ["--config-provider", "file", "--file-path", "/app/bin/default.conf"]