FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
COPY build/arktouros-apm-api* /
EXPOSE 63060
ENV TZ Asia/Shanghai
ENTRYPOINT java -jar arktouros-apm-api-0.0.1.jar
