FROM ghcr.io/graalvm/jdk-community:21
LABEL authors="wangminan"
RUN mkdir arktouros-ui
RUN mkdir arktouros-ui/config
# https://www.iszy.cc/posts/13/
WORKDIR /arktouros-ui
COPY src/main/resources ./config
COPY target/arktouros-webapp.jar ./arktouros-webapp.jar
# RUN ls -l
EXPOSE 50052
ENV TZ Asia/Shanghai
ENTRYPOINT java -jar arktouros-webapp.jar
