#!/bin/bash
docker pull wangminan/arktouros-ui:latest
mkdir -p /root/arktouros
cd /root/arktouros || exit
docker stop arktouros-ui
docker rm arktouros-ui
docker-compose up arktouros-ui -d
