#!/bin/bash
docker pull wangminan/arktouros-apm-server:latest
docker pull wangminan/arktouros-ui:latest
mkdir -p /root/arktouros
cd /root/arktouros || exit
docker-compose stop arktouros-ui
docker-compose rm arktouros-ui
docker-compose stop arktouros-apm-server
docker-compose rm arktouros-apm-server
docker-compose up -d
