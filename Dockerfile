FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
COPY build/arktouros-apm-api/* /
EXPOSE 63060
ENV TZ Asia/Shanghai
ENTRYPOINT java -jar -Dloader.path=/lib -jar /arktouros-apm-api.jar
