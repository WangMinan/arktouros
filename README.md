# Arktouros

"Άρκτουρος"（Arktouros/玉衡）。在希腊神话中，Άρκτουρος是大熊座中最亮的星星，代表着守护和指引的意义。

Arktouros项目是我的本科毕业设计，这是一个轻量级的APM系统，适配中航工业西安计算所的天脉3操作系统。

如果对任何内容有疑问，欢迎通过Github Profile中的联系方式与我取得联系。

毕业论文已经获得西北工业大学首届百篇优秀本科毕业设计（论文）

如果不需要ui项目，可以直接clone；如果需要ui项目，请使用--recursive参数进行clone。

```shell
git clone --recursive https://github.com/wangminan/arktouros
# 或者
git submodule update --init --recursive
```

本地编译至少需要JDK21的基础环境，推荐选用Graal-JDK-21

执行命令

```shell
mvn -Dmaven.test.skip=true clean package
```

如需使用native编译，请使用
```shell
mvn -Dmaven.test.skip=true clean package -Pnative
```

编译产出工件将在${project.basedir}/build/下生成，分别是arktouros-collector，arktouros-webapp与arktouros-apm-api的tar包。

APM 默认使用 SQLite 保存服务、日志、Span 和指标，无需启动 Elasticsearch 或配置其证书。H2 仍作为采集接收队列，两个数据库文件都保存在工作目录的 `db/` 下。

从旧版一体化镜像升级请阅读 [SQLite 迁移指南](docs/SQLite迁移指南.md)。更改配置不能使旧版 JAR 自动获得 SQLite 支持，必须替换后端发行包或重建镜像。

离线交付可手动运行 GitHub Actions 的 **Build SQLite offline installer** 工作流，下载 `install-arktouros-image-linux-amd64` artifact。其中包含前端、大屏、SQLite 后端的完整镜像、无需 Compose 的一键启动脚本和可手动复制导入的 Arktouros TXT 示例，部署步骤见 [安装与排障指南](arktouros-image/安装与排障指南.md)。

在解压出的 `arktouros-apm-api` 目录运行：
```shell
java -Dfile.encoding=UTF-8 -jar arktouros-apm-api-0.0.1.jar
```

如有需要可自行改动config目录下的对应配置文件。

如果需要使用docker运行请自行修改deploy目录下的[docker-compose](deploy/docker-compose.yaml)文件，注意config文件夹映射。

目标端机采集软件请见[https://github.com/wangminan/simple-otel](https://github.com/wangminan/simple-otel)
