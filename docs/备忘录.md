## 本地起elasticsearch8环境

### docker

参考[Install Kibana with Docker | Kibana Guide [8.12\] | Elastic](https://www.elastic.co/guide/en/kibana/current/docker.html)

```bash
docker network create elastic
docker run --name elasticsearch --net elastic -p 9200:9200 -p 9300:9300 -it -m 1GB ne
```

![image-20240318155015301](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240318155015301.png)

记录信息，然后拉kibana

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Elasticsearch security features have been automatically configured!
✅ Authentication is enabled and cluster connections are encrypted.

ℹ️  Password for the elastic user (reset with `bin/elasticsearch-reset-password -u elastic`):
  -yM1zrDUY4gZptHR*j4j

ℹ️  HTTP CA certificate SHA-256 fingerprint:
  5c1a8b2ecdf648da8b8a5d89033c44a56eaf1046a9b5df6932189539f6d8a720

ℹ️  Configure Kibana to use this cluster:
• Run Kibana and click the configuration link in the terminal when Kibana starts.
• Copy the following enrollment token and paste it into Kibana in your browser (valid for the next 30 minutes):
  eyJ2ZXIiOiI4LjEyLjIiLCJhZHIiOlsiMTcyLjE4LjAuMjo5MjAwIl0sImZnciI6IjVjMWE4YjJlY2RmNjQ4ZGE4YjhhNWQ4OTAzM2M0NGE1NmVhZjEwNDZhOWI1ZGY2OTMyMTg5NTM5ZjZkOGE3MjAiLCJrZXkiOiJuNjJKVUk0QmIzUVBMMEtWZzZHUTplZkNFSUkyUlFJdXdkMjdYNk5YcDlRIn0=

ℹ️ Configure other nodes to join this cluster:
• Copy the following enrollment token and start new Elasticsearch nodes with `bin/elasticsearch --enrollment-token <token>` (valid for the next 30 minutes):
  eyJ2ZXIiOiI4LjEyLjIiLCJhZHIiOlsiMTcyLjE4LjAuMjo5MjAwIl0sImZnciI6IjVjMWE4YjJlY2RmNjQ4ZGE4YjhhNWQ4OTAzM2M0NGE1NmVhZjEwNDZhOWI1ZGY2OTMyMTg5NTM5ZjZkOGE3MjAiLCJrZXkiOiJvYTJKVUk0QmIzUVBMMEtWZzZHVjpiSTFzUFpVV1NUV3ZBRXhweVlNeHB3In0=

  If you're running in Docker, copy the enrollment token and run:
  `docker run -e "ENROLLMENT_TOKEN=<token>" docker.elastic.co/elasticsearch/elasticsearch:8.12.2`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
docker run --name kibana --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:8.12.2
```

![image-20240318155413286](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240318155413286.png)

使用给出的链接进入kibana控制台，粘贴kibana的token，配置完成后使用密码登录。

如果您使用[docker-compose文件](./docker-compose-es8.yaml)启动，则需要用以下命令生成token。然后到kibana的logs里找到对应的带code的启动url。

```sh
docker exec -it elasticsearch /usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic
docker exec -it elasticsearch /usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s kibana
```

kibana进去之后可以从右上角点头像改密码

安装成功

![image-20240318155834615](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240318155834615.png)

重启，退出命令行

在使用第三方软件连接时需要用到SSL文件

```sh
docker cp elasticsearch:/usr/share/elasticsearch/config/certs/http_ca.crt .
```



### APIkey的获取

APIkey使用如下请求生成

```shell
POST /_security/api_key
```

可以加参数和过期时间

https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-create-api-key.html

![image-20240321150348317](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240321150348317.png)

```json
{
  "id": "KAbSX44B4XVu6U60Z4G1",
  "name": "arktouros-apm-api",
  "api_key": "gkAetb_uR3-ePhhP5saPPw",
  "encoded": "S0FiU1g0NEI0WFZ1NlU2MFo0RzE6Z2tBZXRiX3VSMy1lUGhoUDVzYVBQdw=="
}
```

