FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
RUN mkdir arktouros
COPY build/arktouros-apm-api/* /arktouros/
EXPOSE 50050
EXPOSE 50051
ENV TZ Asia/Shanghai
WORKDIR /arktouros
RUN pwd
RUN ls -l
ENTRYPOINT java -jar -Dloader.path=lib arktouros-apm-api-0.0.1.jar
