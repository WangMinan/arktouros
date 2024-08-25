#!/bin/bash
docker pull wangminan/arktouros-apm-server:latest
mkdir -p /root/arktouros
cd /root/arktouros || exit
docker stop arktouros-apm-server
docker rm arktouros-apm-server
docker compose up arktouros-apm-server -d
docker image prune -a -f
