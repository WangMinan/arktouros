---
title: Arktouros
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.23"

---

# Arktouros

Base URLs:

# Authentication

# service

## GET 获取服务列表

GET /api/v0/service

> Body 请求参数

```json
{}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|query|query|string| 否 |none|
|pageNum|query|integer| 是 |none|
|pageSize|query|integer| 是 |none|
|body|body|object| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "object": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» message|string|false|none||none|
|» object|object|false|none||none|

# topology

## GET 获取命名空间下拓扑

GET /api/v0/topology

暂时不需要做深度的内容

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|namespace|query|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "object": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» message|string|false|none||none|
|» object|object|false|none||none|

# log

## GET 获取日志列表

GET /api/v0/trace

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pageNum|query|integer| 是 |none|
|pageSize|query|integer| 是 |none|
|service|query|string| 否 |服务名称|
|traceId|query|string| 否 |追踪ID|
|keyword|query|string| 否 |内容关键词|
|keywordsNotIncluded|query|string| 否 |内容不包含的关键词|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "object": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» message|string|false|none||none|
|» object|object|false|none||none|

## GET 获取日志具体信息

GET /api/v0/trace/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |traceId|

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

# trace

## GET 根据服务名称获取traceId表

GET /api/v0/trace/ids

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|serviceName|query|string| 是 |服务名称|

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

## GET 根据traceId组织span表

GET /api/v0/trace/spans

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|string| 是 |traceId|
|endPoint|query|string| 否 |端点名称|
|startTime|query|string| 否 |起始时间|
|endTime|query|string| 否 |结束时间|

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|成功|Inline|

### 返回数据结构

# 数据模型

<h2 id="tocS_R">R</h2>

<a id="schemar"></a>
<a id="schema_R"></a>
<a id="tocSr"></a>
<a id="tocsr"></a>

```json
{
  "code": 0,
  "message": "string",
  "object": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||none|
|message|string|false|none||none|
|object|object|false|none||none|

