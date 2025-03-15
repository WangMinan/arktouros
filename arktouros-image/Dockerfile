FROM almalinux:9
LABEL authors="wangminan"
ENV TZ Asia/Shanghai

# 在Dockerfile中使用shell一定要注意 上下文变量不互通 和github actions一样

# --------安装基础工具--------
RUN dnf install -y vim mlocate net-tools perl-Digest-SHA wget tar procps

# --------安装nginx arktouros-ui arktouros-bigscreen--------
RUN dnf install -y nginx
# 允许nginx开机自启
RUN systemctl enable nginx
# 拷贝bigscreen项目
RUN mkdir -p /etc/nginx/html/arktouros-bigscreen
COPY arktouros-bigscreen /etc/nginx/html/arktouros-bigscreen
RUN mkdir -p /etc/nginx/html/arktouros-ui
# 把arktouros-ui的dist文件夹下的东西拷贝到/etc/nginx/html/arktouros-ui
COPY arktouros-ui/dist /etc/nginx/html/arktouros-ui
# 拷贝当前目录下nginx.conf到/etc/nginx/nginx.conf
COPY arktouros-image/nginx.conf /etc/nginx/nginx.conf

# --------配置java--------
RUN dnf install -y wget tar
WORKDIR /usr/local
RUN wget https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz
RUN tar -zxvf graalvm-jdk-21_linux-x64_bin.tar.gz
RUN rm -rf graalvm-jdk-21_linux-x64_bin.tar.gz
RUN mv /usr/local/graalvm-jdk-21.0.6+8.1 /usr/local/jdk-21
# 环境变量
ENV JAVA_HOME=/usr/local/jdk-21
ENV PATH=$JAVA_HOME/bin:$PATH

# --------安装elasticsearch--------
RUN rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch
RUN wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.15.5-x86_64.rpm
RUN wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.15.5-x86_64.rpm.sha512
RUN shasum -a 512 -c elasticsearch-8.15.5-x86_64.rpm.sha512
# 使用 rpm --install elasticsearch-8.15.5-x86_64.rpm 会输出类似于 The generated password for the elastic built-in superuser is : HC6=QDjDIy-nffDdNaqK 写入文件 /tmp/elastic_password
# 安装elasticsearch并提取生成的密码
RUN ELASTIC_PASSWORD=$(rpm --install elasticsearch-8.15.5-x86_64.rpm | grep "The generated password for the elastic built-in superuser is" | awk '{print $11}') && \
    echo $ELASTIC_PASSWORD > /tmp/elastic_password
RUN systemctl enable elasticsearch
# 修改 /etc/passwd最后一行的最末尾的/sbin/nologin为/bin/bash
RUN sed -i '$s|/sbin/nologin|/bin/bash|' /etc/passwd

USER elasticsearch
# 启动elasticsearch 从/tmp/elastic_password读取初始密码，然后通过接口修改密码
RUN /usr/share/elasticsearch/bin/elasticsearch -d -p /tmp/elasticsearch.pid && \
    curl --cacert /etc/elasticsearch/certs/http_ca.crt -u elastic:$(cat /tmp/elastic_password) -X POST "https://localhost:9200/_security/user/elastic/_password" -H "Content-Type: application/json" -d '{"password":"elasticsearch@631"}'
# 在 /etc/elasticsearch/jvm.options里追加写 -Xms4g -Xmx4g
RUN echo "-Xms8g" >> /etc/elasticsearch/jvm.options && \
	echo "-Xmx8g" >> /etc/elasticsearch/jvm.options

USER root
# 删除.rpm和.sha文件 删除elasticsearch产生的锁
RUN rm -rf elasticsearch-8.15.5-x86_64.rpm elasticsearch-8.15.5-x86_64.rpm.sha512 && \
	rm -rf /var/lib/elasticsearch/node.lock && \
    rm -rf /var/lib/elasticsearch/snapshot_cache/write.lock

# --------安装arktouros--------
WORKDIR /usr/local
COPY build/arktouros-apm-api-leaves.tar.gz /usr/local/arktouros-apm-api-leaves.tar.gz
RUN tar -zxvf arktouros-apm-api-leaves.tar.gz
COPY arktouros-image/arktouros.service /usr/lib/systemd/system/arktouros.service

# --------image配置 包括文件映射 entrypoint--------
EXPOSE 50049 50050 50051 50052 50053

ENTRYPOINT /usr/sbin/init
