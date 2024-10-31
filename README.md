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

编译产出工件将在${project.basedir}/build/下生成，分别是arktouros-collector与arktouros-apm-api的tar包。

解压后的运行命令，您需要在本地运行与elasticsearch-java这一新client版本对应的elasticsearch，建议使用elasticsearch8.7.0及以上版本，使用较低版本的elasticsearch会出现连接问题。
```shell
java -jar -Djava.library.path=lib/ *.jar
```

如有需要可自行改动config目录下的对应配置文件。

如果需要使用docker运行请自行修改deploy目录下的[docker-compose](deploy/docker-compose.yaml)文件，注意config文件夹映射。

目标端机采集软件请见[https://github.com/wangminan/simple-otel](https://github.com/wangminan/simple-otel)
