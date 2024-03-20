create table if not exists APM_METRICS_QUEUE
(
    ID   bigint primary key auto_increment(1), -- 标注1是因为我这儿试出来默认自增是32 太逆天了
    DATA longtext
);

create table if not exists APM_LOG_QUEUE
(
    ID   bigint primary key auto_increment(1),
    DATA longtext
);

create table if not exists APM_TRACE_QUEUE
(
    ID   bigint primary key auto_increment(1),
    DATA longtext
);
