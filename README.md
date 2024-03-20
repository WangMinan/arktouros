# Arktouros

"Άρκτουρος"（Arktouros/玉衡）。在希腊神话中，Άρκτουρος是大熊座中最亮的星星，代表着守护和指引的意义。

Arktouros项目将是我的本科毕业设计，这是一个轻量级的APM系统，用于适配中航工业西安计算所的天脉3操作系统。

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

解压后的运行命令
```shell
java -jar -Djava.library.path=lib/ *.jar
```

如有需要可自行改动config目录下的对应配置文件
