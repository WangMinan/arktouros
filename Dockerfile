FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
RUN mkdir arktouros
# https://www.iszy.cc/posts/13/
WORKDIR /arktouros
COPY build/arktouros-apm-api/config ./config
COPY build/arktouros-apm-api/lib ./lib
COPY build/arktouros-apm-api/arktouros-apm-api-0.0.1.jar ./arktouros-apm-api-0.0.1.jar
RUN ls -l
EXPOSE 50050
EXPOSE 50051
ENV TZ Asia/Shanghai
ENTRYPOINT java -jar -Dloader.path=lib arktouros-apm-api-0.0.1.jar
