FROM openjdk:8-jre
MAINTAINER Haystack <haystack@expedia.com>

ENV APP_NAME haystack-agent
ENV APP_HOME /app/bin

RUN mkdir -p ${APP_HOME}

COPY bundlers/haystack-agent/target/${APP_NAME}.jar ${APP_HOME}/

COPY docker/dev.conf ${APP_HOME}/
COPY docker/start-app.sh ${APP_HOME}/

RUN chmod +x ${APP_HOME}/start-app.sh

WORKDIR ${APP_HOME}

ENTRYPOINT ["./start-app.sh"]

CMD ["--config-provider", "file", "--file-path", "/app/bin/default.conf"]