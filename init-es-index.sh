#!/bin/bash

# 等待Elasticsearch启动
echo "等待Elasticsearch启动..."
sleep 10

# 检查Elasticsearch是否可用
echo "检查Elasticsearch连接..."
until curl -s http://localhost:9200/_cluster/health; do
  echo "等待Elasticsearch可用..."
  sleep 5
done

echo "创建rag_chunks索引..."
curl -X PUT "http://localhost:9200/rag_chunks" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "chunkId": {
        "type": "keyword"
      },
      "fileMd5": {
        "type": "keyword"
      },
      "source": {
        "type": "text"
      },
      "chunkIndex": {
        "type": "integer"
      },
      "content": {
        "type": "text"
      },
      "charCount": {
        "type": "integer"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 1024,
        "index": true,
        "similarity": "cosine"
      },
      "embeddingDim": {
        "type": "integer"
      },
      "model": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date"
      }
    }
  }
}'

echo "索引创建完成"
