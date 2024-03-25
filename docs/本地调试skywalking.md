```shell
docker create network es-net
docker run -d \
	--name elasticsearch7 \
    -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
    -e "discovery.type=single-node" \
    -v es-data:/usr/share/elasticsearch/data \
    -v es-plugins:/usr/share/elasticsearch/plugins \
    --privileged \
    --network es-net \
    -p 9200:9200 \
    -p 9300:9300 \
elasticsearch:7.12.1
```

通过上面的命令在本地启动elasticsearch环境

![image-20240325113349418](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240325113349418.png)

OAP服务启动类`oap-server/server-starter/src/main/java/org/apache/skywalking/oap/server/starter/OAPServerStartUp.java`

```sh
# 查看index列表
curl -XGET 'http://localhost:9200/_cat/indices?v'
```

![image-20240325113334608](https://cdn.jsdelivr.net/gh/WangMinan/Pics/image-20240325113334608.png)

```sh
# 查看某个index的mapping结构
curl -XGET 'http://localhost:9200/<index_name>/_mapping?pretty'
```

