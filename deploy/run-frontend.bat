@echo off

REM Pull the Docker image
docker pull wangminan/arktouros-ui:latest

REM Create directory if it doesn't exist
if not exist "C:\arktouros" (
    mkdir "C:\arktouros"
)

REM Navigate to the directory
cd /d "C:\arktouros" || exit /b

REM Stop and remove the container
docker stop arktouros-ui
docker rm arktouros-ui

REM Start the container using Docker Compose
docker compose up arktouros-ui -d

REM Clean up dangling images
docker image prune -a -f
