#!/bin/bash
docker pull wangminan/arktouros-ui:latest
mkdir -p /root/arktouros
cd /root/arktouros || exit
docker-compose stop arktouros-ui
docker-compose rm arktouros-ui
docker-compose up -d
