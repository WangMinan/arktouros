#!/bin/bash
docker pull wangminan/arktouros-apm-server:latest
mkdir -p /root/arktouros
cd /root/arktouros || exit
docker-compose stop arktouros-apm-server
docker-compose rm arktouros-apm-server
docker-compose up -d
