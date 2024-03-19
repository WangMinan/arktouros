create schema if not exists "apm";

create table if not exists "apm"."apm_metrics_queue"
(
    id   bigint primary key auto_increment,
    data longtext
);

create table if not exists "apm"."apm_log_queue"
(
    id   bigint primary key auto_increment,
    data longtext
);

create table if not exists "apm"."apm_trace_queue"
(
    id   bigint primary key auto_increment,
    data longtext
);
