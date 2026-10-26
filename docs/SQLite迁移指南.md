# Arktouros SQLite 迁移指南

新版 APM 使用 SQLite 替代 Elasticsearch 的遥测存储，通过既有的四个工厂扩展接入。旧版镜像里的 Elasticsearch HTTP 证书已于 2026 年 10 月 4 日到期，原 systemd 服务因启动前的 HTTPS 健康检查无法通过而停留在 `activating (start-pre)`。新版干净镜像不安装 Elasticsearch，通过 Supervisor 管理 Java 与 nginx，无需 systemd 或 privileged 模式。

**旧 tar 和旧容器不会因修改源码而更新。需要使用本次编译的后端发行包，并加载新版配置及容器启动配置。SQLite 从空库开始，不自动迁移 Elasticsearch 历史索引。**

## 代码链路与变更范围

| 环节 | 原有链路 | SQLite 接入位置 |
| --- | --- | --- |
| 采集 | collector / simple-otel → Arktouros TCP、gRPC、离线 JSON；OTel gRPC → H2 队列 → Analyzer | 保留接收协议和分析器，仍调用 `SinkService.sink` |
| 写入 | `SinkServiceFactoryBean` → Elasticsearch sink | 新增 `SqliteSinkService`，服务和遥测记录在同一事务中写入 |
| 存储 | Elasticsearch 的 service、log、span、四类 metric 索引 | `SqliteStore` 保存完整 JSON，以生成列和索引支持 SQL 查询；Span 和 Service 按 ID 更新，Log 和 Metric 追加 |
| 查询 | Controller → `SearchServiceImpl` → `SearchMapperFactory` | 新增 `SqliteSearchMapper`，覆盖全部 20 个抽象方法；复用原拓扑、Span 树和异常耗时计算 |
| 定时统计 | `ScheduledJobFactoryBean` | 新增 `SqliteScheduledJob`，通过现有 SearchService/SinkService 计算吞吐量、响应时间、错误率 |
| 数据清理 | Controller → `DataOperationServiceFactoryBean` | 新增 `SqliteOperationService`，支持清理全部数据、日志、Span、指标 |
| 启动部署 | systemd 等待 Elasticsearch；镜像安装 Elasticsearch | 新镜像直接启动 APM，数据库目录挂载到宿主机 |

H2 的 `spring.datasource` 仍服务于接收队列，不是遥测查询存储。SQLite 使用独立连接池，不会替换 Spring 的 H2 DataSource。原 H2 遥测扩展是占位实现，不能选择 `h2` 作为四个存储扩展。

ES Java 依赖仍被原有模型映射和可选 ES 实现引用，因此保留该依赖；SQLite 路径不会创建 ES 客户端或连接 ES 服务。

## 配置和文件

两个默认配置已切换为 SQLite：

- `arktouros-apm/arktouros-apm-api/src/main/resources/application.yaml`：本地发行包默认配置。
- `arktouros-image/arktouros-apm-api/config/application.yaml`：一体化镜像的 Linux 配置。

四个选项必须一致；选择 SQLite 时会在启动阶段检查，避免写入和查询使用不同数据库：

```yaml
instance:
  active:
    sinker: sqlite
    searchMapper: sqlite
    scheduledJob: sqlite
    operation: sqlite
sqlite:
  path: ./db/arktouros.sqlite
```

接收器保持原来的选择，离线用户继续使用 `dataReceiver: jsonFile`，并核对 `receiver.file.json.type`、`logDir` 和 `indexFilePath`。本地默认目录为 `./input_logs`，首次启动会创建目录。

`sqlite.path` 相对于 Java 进程的工作目录解析，也可设置绝对路径或通过 `SQLITE_PATH` 环境变量覆盖。默认文件：

- `db/arktouros.sqlite`：SQLite 遥测数据。
- `db/arktouros.sqlite-wal`、`db/arktouros.sqlite-shm`：运行期间可能存在的 WAL 文件。
- `db/apm.mv.db`：原 H2 接收队列。

Compose 已挂载整个 `db/` 目录。请使用本机磁盘，避免将 WAL 数据库放在网络文件系统上；备份时停止 APM 后复制整个目录，不能在运行中只复制主 SQLite 文件。[SQLite WAL 说明](https://sqlite.org/wal.html)

## 编译与验证

Windows 使用 PowerShell 7，在仓库根目录执行：

```powershell
mvn -pl arktouros-apm/arktouros-apm-api -am test '-Dtest=SqliteStorageTest,SqliteApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DtestFailureIgnore=false'
mvn -pl arktouros-apm/arktouros-apm-api -am package '-DskipUTs=true' '-DtestFailureIgnore=false'
```

后端发行包为 `build/arktouros-apm-api-leaves.tar.gz`，包含 SQLite JDBC 和依赖库。不要只拷贝 API JAR；同包的 `lib/` 必须同步更新。

`SqliteStorageTest` 使用临时文件验证所有查询、四类指标、Span 更新、重启保留、80 次并发写入、失败事务回滚、定时统计及选择性删除。`SqliteApiTest` 启动完整后端，以真实 gRPC 写入日志/Span/指标，再通过 HTTP 验证全部查询及删除接口，并验证删除后仍可接收数据。

仓库 Maven 默认允许忽略测试失败，验证时必须显式指定 `-DtestFailureIgnore=false`。

2026 年 10 月 26 日本次验证：上述 SQLite 测试加既有 ES Mapper、Arktouros Span 接收回归，共 25 项通过，Maven package 成功。另直接解压最终发行包运行默认离线 JSON 接收器，导入日志/Span/指标后强制停止 Java 并重启，两轮查询均通过，日志没有重复增加。最终包内 SQLite JDBC 为 3.53.4.0，包含 Windows/Linux x64 原生库；版本在根 POM 统一管理，避免被 Spring Boot 的传递依赖版本覆盖。

本地验证记录为 `build/sqlite-validation/final-package.log` 和 `build/sqlite-release-smoke/result.json`。这些是构建产物，不纳入 Git。

## 构建纯净一体化镜像与离线交付包

当前主路径是 `arktouros-image/Dockerfile`：Node 构建前端，运行层使用 Temurin 21 JRE、nginx 和 Supervisor，包含本仓库的大屏静态资源。不继承旧镜像，也不安装 Elasticsearch。

先按上节构建后端，在仓库根目录执行：

```powershell
docker buildx build --platform linux/amd64 --load -f arktouros-image/Dockerfile -t arktouros-all-in-one:sqlite .
docker save -o build/arktouros-all-in-one.tar arktouros-all-in-one:sqlite
python arktouros-image/generate-example.py
python arktouros-image/package-install.py --image-tar build/arktouros-all-in-one.tar --output build/install-arktouros-image
```

输出目录必须为空，打包脚本不会覆盖既有安装目录。`package-install.py` 校验镜像标签、SQLite 标记和 Linux amd64 架构，生成 SHA256 校验文件和 release-manifest.json，并只复制交付所需配置、脚本和合成数据；不会带上开发日志、数据库、旧证书或旧输入索引。

本机 Docker Desktop 未加入 PATH 时，可仅在当前 PowerShell 会话中设置：

```powershell
$env:PATH = 'C:\Program Files\Docker\Docker\resources\bin;' + $env:PATH
```

GitHub Actions 的 **Build SQLite offline installer** 工作流位于 `.github/workflows/build-sqlite-installer.yml`，仅手动触发。它测试后端、构建完整镜像、导出 tar、组装安装目录，再运行 `test-installer.py` 验证容器，最后上传 `install-arktouros-image-linux-amd64` artifact；不推送镜像仓库，也不创建 Release。

`test-installer.py` 从 ZIP 解压临时副本，验证仅 Docker 启动、旧容器备份及清理、手动 TXT 导入、查询、重启、清空与重新导入，不会把测试数据库写进交付目录。在 Linux 运行：

```bash
python3 arktouros-image/test-installer.py build/install-arktouros-image.zip
```

本机 Windows 验证使用 Docker Desktop 与 Git Bash，需要将 Git 的 bin 目录放在 PATH 中以优先使用 Git Bash。

## 交付与迁移

交付目录可以整体压缩为 ZIP。甲方解压后按 [安装与排障指南](../arktouros-image/安装与排障指南.md) 启动；首次启动后可手动复制 `examples/arktouros-demo.txt` 验证，再接入自己的数据。

与旧安装包保持相同的 80、50049～50053 端口及五项有效配置/日志/输入/前端挂载，仅移除 ES 端口并增加 `db/` 挂载。旧包中可选的 nginx 挂载仍以注释保留。

旧目录应整体备份到交付目录之外。旧 tar 的标签是 `arktouros-all-in-one:0.1`，包含旧 ES 环境，不能作为新版镜像交付；旧 PDF、日志和已读输入索引也不应放入新包。旧 ES 数据不会自动迁移；原始采集 JSON 可复制到新环境重新导入，备份原件不变。

## 行为与边界

- HTTP 路径和响应结构保持一致，包括服务列表、日志分页、端点、拓扑、Span 名称/耗时、指标、命名空间及时间范围。
- Span 时间范围分别约束 `startTime >= startTimestamp` 和 `endTime <= endTimestamp`；原 ES 代码复用一个 RangeQuery Builder 会覆盖字段，SQLite 明确应用两项约束。吞吐量按 `startTime` 统计，修正了原 ES 查询不存在的 Span `timestamp` 字段的问题。
- 日志关键词按空白拆分后做字面包含匹配：包含词要求全部命中，排除词任一个命中就排除；支持中文子串和字面 `%`、`_`。ASCII 大小写不敏感，不复现 ES 的语言分词、词干或相关性评分。
- 指标同时给出时间上下界且跨度达到一小时后，沿用小时/天/年的分桶规则，每个名称和指标类型取桶内最早的点，按 UTC 桶边界计算；SQL 窗口查询不会受 ES 默认聚合桶数量限制。
- 删除接口以一条事务性 DELETE 清理当前已提交记录。单类型删除保留服务目录；删除全部数据包含服务目录。接收器不停止、不清空 H2 队列，不重读输入文件；调用后到达的数据仍会写入。需要保持空库时，应先停止采集源并排空接收队列。
- 定时统计在当前 `ApmMain` 中原本就没有启动，本次保留这一默认行为。SQLite 定时实现已具备三个统计任务，可由原有 `ScheduledJob.start()` 接入；不启动 ES rollover 或虚构指标。
- SQLite 适用于本机单实例存储；已配置 WAL 和 10 秒忙等待，但没有做目标虚拟机的大规模数据/持续吞吐量测试，也没有新增数据保留期或自动清理任务。[SQLite 并发限制](https://sqlite.org/wal.html)
- Linux Docker/systemd 镜像运行需要在目标主机验证；Windows Java 集成测试不能代替目标主机验收。

回退时停止新版容器，保留其 SQLite 数据目录，再使用旧容器和原配置。旧容器的证书过期问题仍需要续期处理；回退本身不会修复证书。
