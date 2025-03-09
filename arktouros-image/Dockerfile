FROM almalinux:9
LABEL authors="wangminan"
ENV TZ Asia/Shanghai

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
# 使用 rpm --install elasticsearch-8.15.5-x86_64.rpm
# 会输出类似于 The generated password for the elastic built-in superuser is : HC6=QDjDIy-nffDdNaqK
# 从输出中拿到elasticsearch的密码 存到变量ELASTIC_PASSWORD
RUN ELASTIC_PASSWORD=$(rpm --install elasticsearch-8.15.5-x86_64.rpm | grep "The generated password for the elastic built-in superuser is" | awk '{print $11}')
RUN systemctl enable elasticsearch
# 创建非root用户
RUN useradd -ms /bin/bash es
RUN chown -R es:es /usr/share/elasticsearch /etc/elasticsearch /var/lib/elasticsearch /var/log/elasticsearch
# 切换到非root用户
USER es
RUN /usr/share/elasticsearch/bin/elasticsearch -d -p /tmp/elasticsearch.pid
USER root
# 通过接口修改密码
RUN curl --cacert /etc/elasticsearch/certs/http_ca.crt -u elastic:$ELASTIC_PASSWORD -X POST "https://localhost:9200/_security/user/elastic/_password" -H "Content-Type: application/json" -d '{"password":"elasticsearch@631"}'
# 删除.rpm和.sha文件
RUN rm -rf elasticsearch-8.15.5-x86_64.rpm elasticsearch-8.15.5-x86_64.rpm.sha512

# --------安装arktouros--------
WORKDIR /usr/local
COPY build/arktouros-apm-api-leaves.tar.gz /usr/local/arktouros-apm-api-leaves.tar.gz
RUN tar -zxvf arktouros-apm-api-leaves.tar.gz
COPY arktouros-image/arktouros.service /usr/lib/systemd/system/arktouros.service

# --------都搞好了 关闭所有服务--------
RUN systemctl stop nginx
# 通过jps关闭elasticsearch
RUN jps | grep Elasticsearch | awk '{print $1}' | xargs kill -12

# 把权限还给elasticsearch用户并删除用户es
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch /etc/elasticsearch /var/lib/elasticsearch /var/log/elasticsearch
RUN userdel -r es

# --------image配置 包括文件映射 entrypoint--------
EXPOSE 50049 50050 50051 50052 50053

ENTRYPOINT /usr/sbin/init
