#!/bin/bash

# Elasticsearch 地址和认证配置
ES_HOST="https://localhost:9200"
CERT_PATH="/etc/elasticsearch/certs/http_ca.crt"

# 获取所有以 arktouros 开头的索引
indices=$(curl -u elastic:elasticsearch@631 -s --cacert "$CERT_PATH" -X GET "$ES_HOST/_cat/indices/arktouros*" | awk '{print $3}')

# 检查是否存在符合条件的索引
if [ -z "$indices" ]; then
  echo "没有找到以 'arktouros' 开头的索引。"
  exit 0
fi

# 删除符合条件的索引
for index in $indices; do
  echo "正在删除索引: $index"
  curl -u elastic:elasticsearch@631 -s --cacert "$CERT_PATH" -X DELETE "$ES_HOST/$index"
done

echo "所有以 'arktouros' 开头的索引已删除。"
