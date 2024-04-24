FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
RUN mkdir arktouros
COPY build/arktouros-apm-api/* /arktouros/
EXPOSE 63060
ENV TZ Asia/Shanghai
ENTRYPOINT java -jar /arktouros/arktouros-apm-api-0.0.1.jar
