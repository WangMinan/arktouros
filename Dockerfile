FROM almalinux:9-minimal
LABEL authors="wangminan"
COPY arktouros-apm/arktouros-apm-api/target/arktouros-apm-api /
EXPOSE 63060
ENV TZ Asia/Shanghai
ENTRYPOINT /arktouros-apm-api
