@echo off

REM Pull the Docker image
docker pull wangminan/arktouros-apm-server:latest

REM Create directory if it doesn't exist
if not exist "C:\arktouros" (
    mkdir "C:\arktouros"
)

REM Navigate to the directory
cd /d "C:\arktouros" || exit /b

REM Stop and remove the container
docker stop arktouros-apm-server
docker rm arktouros-apm-server

REM Start the container using Docker Compose
docker compose up arktouros-apm-server -d

REM Clean up dangling images
docker image prune -a -f
