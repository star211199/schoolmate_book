<div align="center">

# 大学同学录 · schoolmate_book

**把「班级」装进浏览器** —— 班级主页 · 留言墙 · 相册 · 动态时间轴 · 实时聊天 · 通知中心 · 时光胶囊

一个前后端分离的校园同学录全栈项目：后端是严格三层的 Spring Boot 3 服务，前端是 5 套主题可切换的 Vue 3 单页应用。

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-E4382B?style=flat-square)](https://baomidou.com/)
[![Vue](https://img.shields.io/badge/Vue-3.5-42B883?style=flat-square&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.8-409EFF?style=flat-square)](https://element-plus.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)

[![GitHub Stars](https://img.shields.io/github/stars/star211199/schoolmate_book?style=flat-square&logo=github&label=Stars)](https://github.com/star211199/schoolmate_book/stargazers)
[![Last Commit](https://img.shields.io/github/last-commit/star211199/schoolmate_book?style=flat-square)](https://github.com/star211199/schoolmate_book/commits/main)
[![Repo Size](https://img.shields.io/github/repo-size/star211199/schoolmate_book?style=flat-square)](https://github.com/star211199/schoolmate_book)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](https://github.com/star211199/schoolmate_book/blob/main/LICENSE)

</div>

---

## 📖 这是什么

大学四年过去，班级群会沉底、群文件会过期、毕业照散落在各人手机里。
这个项目想提供一个**把班级沉淀下来**的地方 —— 一个班，一个主页，把留言、相册、
动态和聊天都收在一起。

除了业务本身，它同时是一份**可运行的工程范式参考**：

- 严格三层架构（Controller / Service / Mapper）+ DTO 入参 / VO 出参，实体不对外暴露
- 统一响应契约 `{ code, message, data }` + 全局异常处理 + 语义化错误码
- JWT 无状态鉴权（拦截器 + ThreadLocal 用户上下文）
- WebSocket 原生协议实现实时私聊 / 群聊，含未读数、已读回执、消息撤回
- **通知中心**：把好友申请、点赞、评论、胶囊到期聚合成一个铃铛入口，未读数走 WebSocket 实时推送
- **时光胶囊**：写给未来的自己或全班的一封信，到点前内容对任何人（含写信人）都不可见
- 5 套主题全部由 CSS 变量驱动，切换主题只改一个 `data-theme` 属性
- 一套**免 sudo 便携部署**脚本：没有 root、装不了系统包也能把整套服务跑起来并穿透到公网

---

## 📷 界面预览

<img src="images/login.webp" alt="登录页：樱花主题插画 + 演示账号提示" width="100%">

<table>
  <tr>
    <td width="50%" align="center">
      <img src="images/class-list.webp" alt="班级广场"><br>
      <sub>班级广场 · 搜索 / 加入 / 创建班级</sub>
    </td>
    <td width="50%" align="center">
      <img src="images/class-detail.webp" alt="班级主页"><br>
      <sub>班级主页 · 毕业倒计时 / 成员 / 生日提醒</sub>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="images/chat.webp" alt="实时聊天"><br>
      <sub>消息 · WebSocket 私聊与群聊</sub>
    </td>
    <td width="50%" align="center">
      <img src="images/themes.webp" alt="主题装扮"><br>
      <sub>主题装扮 · 5 套配色即时切换</sub>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="images/profile.webp" alt="个人中心"><br>
      <sub>个人中心 · 资料 / 星座 / MBTI / 技能标签</sub>
    </td>
    <td width="50%" align="center">
      <img src="images/admin.webp" alt="后台管理"><br>
      <sub>后台管理 · 数据概览 / 用户 / 内容审核</sub>
    </td>
  </tr>
</table>

---

## ✨ 功能特性

### 🏫 班级与内容

| 模块 | 能力 |
|---|---|
| **班级** | 创建班级（自动生成邀请码）、凭邀请码加入、成员列表、移出成员、修改信息、解散班级 |
| **留言墙** | 发布留言、按时间倒序分页、删除（本人或管理员） |
| **相册** | 多相册管理、照片上传（后缀白名单 + 单文件 ≤ 10MB）、首图自动回填为封面 |
| **动态时间轴** | 发布动态、评论、点赞，构成班级时间轴 |
| **个人主页** | 学号 / 生日 / 星座（按生日自动计算）/ MBTI / 籍贯 / 现居 / 兴趣爱好 / 技能标签 / 毕业寄语 / 社交链接 / 封面图 |

### 💬 社交与实时

| 模块 | 能力 |
|---|---|
| **好友体系** | 用户搜索、发送申请、同意 / 拒绝、待处理计数、备注、解除关系、关系查询 |
| **实时聊天** | 私聊 + 群聊，WebSocket 推送新消息、未读数、已读回执、消息撤回、断线增量拉取 |
| **班级群联动** | 创建班级时自动建立班级群与群会话，成员入班即入群 |
| **通知中心** | 好友申请 / 通过、动态被赞、动态被评论、胶囊到期统一聚合；未读数实时角标、单条已读、全部已读、按类型跳转 |
| **时光胶囊** | 写给未来的自己（SELF，仅本人可见）或写给全班（PUBLIC，班级成员可见）；开启时间必须是将来，**到期前 content 一律不下发**，定时任务到点置为已开启并通知写信人 |
| **找回密码** | 邮箱验证码找回（6 位、10 分钟有效、60 秒防重发，未配 SMTP 时自动隐藏入口）+ 管理员后台一键重置 |
| **主题装扮** | 🌸 樱の和风 · 🌌 星海夜航 · 💜 紫夜霓虹 · 🌿 薄荷森屿 · 🌇 暮色橘颂 |
| **H5 移动端** | 独立的手机端同学录页面 |
| **后台管理** | 数据概览、用户启停、密码重置、内容审核（留言 / 动态 / 照片） |

> 🔒 **通知写入都在事务提交后触发**（`TransactionUtil.afterCommit`），避免「推送了但事务回滚」的幽灵通知；
> 「自己赞自己 / 自己评论自己」不会产生通知。

---

## 🧱 技术栈

| 层次 | 选型 |
|---|---|
| **后端框架** | Spring Boot 3.3.5 · Spring Web · Spring WebSocket · Validation · AOP |
| **持久层** | MyBatis-Plus 3.5.7 · MySQL Connector/J |
| **安全** | JWT（jjwt 0.12.6）· Spring Security Crypto（BCrypt） |
| **接口文档** | Knife4j 4.x（OpenAPI 3），在线调试 `/api/doc.html` |
| **工具库** | Hutool · Lombok |
| **前端框架** | Vue 3.5（Composition API）· Vue Router 4 · Pinia |
| **构建 / UI** | Vite 5 · Element Plus 2.8 · `@element-plus/icons-vue` |
| **网络** | Axios（请求 / 响应拦截器统一处理 Token 与错误）· 原生 WebSocket |
| **数据库** | MySQL 8.0（`utf8mb4`），21 张表 |
| **运行环境** | JDK 17 · Maven 3.9+ · Node 18+ |

---

## 🏗 架构设计

### 整体架构

```mermaid
flowchart LR
    subgraph Client["浏览器"]
        SPA["Vue 3 SPA<br/>Pinia / Vue Router / Element Plus"]
        WSC["WebSocket 客户端"]
    end

    Proxy["Vite 服务 :5173<br/>/api/** 与 /api/ws/chat 反向代理"]

    subgraph Server["Spring Boot 3 :8080（context-path = /api）"]
        Interceptor["JWT 拦截器<br/>鉴权 + ThreadLocal 用户上下文"]
        Controller["Controller<br/>参数校验 / Result 包装"]
        Service["Service<br/>业务规则 / 事务 / 越权校验"]
        Mapper["Mapper<br/>MyBatis-Plus"]
        Interceptor --> Controller --> Service --> Mapper
    end

    DB[("MySQL 8.0<br/>21 张表")]
    Disk["本地磁盘<br/>upload/ 图片存储"]
    Sched["@Scheduled 定时任务<br/>时光胶囊到期开启"]

    SPA -->|"REST（JSON）"| Proxy
    WSC -.->|"ws 升级"| Proxy
    Proxy --> Interceptor
    Mapper --> DB
    Service --> Disk
    Service -.->|"推送消息"| WSC
    Sched --> Service
```

> 前端把 `/api`（含 WebSocket 升级）统一代理到后端，因此**内网穿透只需暴露 5173 一个端口**。

### 分层职责

| 层 | 职责 | 明确不做 |
|---|---|---|
| **Controller** | 接收请求、JSR-303 参数校验、调用 Service、包装 `Result` | 写业务逻辑、直接调 Mapper、感知事务 |
| **Service** | 业务规则、事务边界、越权校验、DTO → VO 转换 | 依赖 `HttpServletRequest` 等 Web 对象 |
| **Mapper** | 单表 CRUD 与条件查询 | 跨表业务编排 |

```
controller/           18 个控制器（后台接口集中在 controller/admin/）
service/ + service/impl/   13 个业务接口与实现
mapper/               MyBatis-Plus BaseMapper 扩展
entity/ dto/ vo/      实体 / 入参 / 出参，实体不出现在接口签名上
ws/                   ChatWebSocketHandler · WsSessionRegistry · WsFrame
interceptor/ context/ JwtInterceptor · UserContext
common/                Result · ResultCode · PageResult · BasePageQuery
```

### 统一响应契约

所有接口统一返回：

```json
{
  "code": 20000,
  "message": "success",
  "data": {}
}
```

| code | 含义 | 说明 |
|---|---|---|
| `20000` | 成功 | — |
| `40001` | 参数校验失败 | 由 `@Valid` 与全局异常处理器统一转译 |
| `40100` | 未认证 | Token 缺失、过期或非法 |
| `40300` | 无权限 | 越权操作（如非群主解散群） |
| `40400` | 资源不存在 | 未命中的接口路径或业务资源 |
| `40900` | 资源冲突 | 唯一约束冲突等 |
| `50000` | 服务器内部错误 | 未预期的异常，堆栈只落日志不外泄 |

分页统一返回 `{ total, pageNum, pageSize, totalPages, records }`；手机号等敏感字段对外脱敏为 `138****8888`。

### 内容审核状态机

内容发布可配置为「先审后发」，审核开关由 `schoolmate.content.audit-enabled` 控制。

```mermaid
stateDiagram-v2
    [*] --> PENDING: 用户提交
    PENDING --> PASSED: 管理员通过
    PENDING --> REJECTED: 管理员驳回
    REJECTED --> PENDING: 修改后重新提交
    PASSED --> [*]: 对外可见
```

### 时光胶囊状态机

`status` 字段**不参与可见性判定**（可见性只看 `now >= open_time`），它唯一的职责是给定时任务
提供一个「从未知到已知」的状态跃迁触发点 —— 只有跨越 SEALED → OPENED 那一刻才发一次到期通知。

```mermaid
stateDiagram-v2
    [*] --> SEALED: 写信并封存
    SEALED --> OPENED: 定时任务扫到 open_time 已过
    SEALED --> [*]: 写信人撤回
    OPENED --> [*]: 内容永久可见，不可删除
```

```mermaid
sequenceDiagram
    participant T as @Scheduled（60s 一轮）
    participant S as CapsuleService
    participant N as NotificationService
    participant W as WebSocket
    participant U as 写信人

    T->>S: 扫描 status=SEALED 且 open_time <= now
    S->>S: 置为 OPENED
    S->>N: notify(CAPSULE_OPENED)
    N->>N: 落库 notification
    N->>W: 事务提交后推送 NOTIFICATION 帧
    W-->>U: 铃铛角标 +1
```

---

## 📁 目录结构

```
schoolmate_book/
├── schoolmate-server/                      # 后端 Spring Boot 3
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/schoolmate/
│       │   ├── common/                     # Result / ResultCode / PageResult / BasePageQuery
│       │   ├── config/                     # WebMvc / MyBatis-Plus / Knife4j / Security / WebSocket
│       │   ├── interceptor/                # JwtInterceptor
│       │   ├── context/                    # UserContext（ThreadLocal，请求结束清理）
│       │   ├── controller/                 # 16 个控制器（admin/ 子包为后台接口）
│       │   ├── service/ + service/impl/    # 业务接口与实现
│       │   ├── mapper/                     # 数据访问
│       │   ├── entity/ dto/ vo/            # 实体 / 入参 / 出参
│       │   ├── ws/                         # ChatWebSocketHandler / WsSessionRegistry / WsFrame
│       │   ├── exception/                  # BusinessException / GlobalExceptionHandler
│       │   └── utils/                      # JwtUtil / FileUploadUtil / ConstellationUtil
│       └── resources/                      # application.yml / application-dev.yml
├── schoolmate-web/                         # 前端 Vue 3 + Vite
│   ├── vite.config.js                      # 代理、端口、静态资源缓存策略
│   ├── public/images/                      # 站点图片（WebP）
│   └── src/
│       ├── api/                            # 按资源拆分的接口定义（12 个模块）
│       ├── router/                         # 路由表 + 登录 / 管理员守卫
│       ├── stores/                         # Pinia：user · chat · theme · notification
│       ├── utils/                          # axios 封装 / WebSocket 客户端
│       └── views/                          # 页面：auth · class · chat · contact · notification · capsule · profile · settings · h5 · admin
├── sql/
│   ├── init.sql                            # 建库 + 11 张基础表 + 种子数据
│   ├── upgrade.sql                         # 增量升级：资料扩展字段 / 点赞表 / 毕业日期
│   ├── v2-init.sql                         # v2 模块：好友 + 群聊 + 会话 + 时光胶囊表（8 张表）
│   └── upgrade-v3.sql                      # v3 模块：通知中心表（1 张表）
├── docs/                                   # 数据库设计、接口契约、回归脚本
├── images/                                 # README 界面截图（WebP）
├── scripts/                                # 图片 WebP 化与压缩
└── deploy/                                 # 免 sudo 便携部署脚本与运维文档
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| MySQL | 8.0+ |

### 1️⃣ 初始化数据库

```bash
mysql -uroot -p < sql/init.sql          # 建库 + 11 张基础表 + 种子数据
mysql -uroot -p < sql/upgrade.sql       # 增量：资料扩展字段 / 点赞表 / 毕业日期
mysql -uroot -p < sql/v2-init.sql       # 增量：好友 / 群聊 / 会话 / 时光胶囊（8 张表）
mysql -uroot -p < sql/upgrade-v3.sql    # 增量：通知中心（1 张表）
```

四个脚本按 `init.sql` → `upgrade.sql` → `v2-init.sql` → `upgrade-v3.sql` 的顺序执行。

> ⚠️ **执行前请留意**：`init.sql` 与 `v2-init.sql` 在每张表建表前会先 `DROP TABLE`
> （**会清空既有数据**），仅用于首次初始化或重建环境；`upgrade.sql`、`upgrade-v3.sql`
> 是字段/新表增量脚本，用 `CREATE TABLE IF NOT EXISTS` 保证可重复执行，但也不会清理已有数据。

### 2️⃣ 启动后端

```bash
cd schoolmate-server
mvn spring-boot:run
```

| 地址 | 用途 |
|---|---|
| `http://localhost:8080/api` | 接口根路径 |
| `http://localhost:8080/api/doc.html` | Knife4j 在线接口文档 |

> **端口已被固定**：`pom.xml` 的 `spring-boot-maven-plugin` 中写入了
> `<jvmArguments>-Dserver.port=8080</jvmArguments>`，JVM 系统属性优先级高于操作系统环境变量，
> 因此环境中即便存在 `SERVER_PORT` 也不会被覆盖。
>
> ⚠️ 在 **PowerShell** 中执行 `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`
> 会被拆词，报 `Unknown lifecycle phase ".run.arguments=..."`，请勿使用这种写法。

### 3️⃣ 启动前端

```bash
cd schoolmate-web
npm install
npm run dev
```

访问 <http://localhost:5173>（已配置代理 `/api` → `http://localhost:8080`）。

> 已启用 `strictPort: true`：5173 被占用时会直接报错而不是自动漂移端口，
> 请先结束占用进程再启动。

### 🔑 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员（可进入 `/admin`） |
| `zhangsan` | `123456` | 普通用户 |

示例班级邀请码：`CLASS01`。

---

## 📡 接口一览

共 **83 个 REST 接口**，完整契约见 [`docs/api.md`](docs/api.md)，或在 Knife4j 中在线调试。

| 分组 | 前缀 | 主要能力 |
|---|---|---|
| 认证 | `/auth` | 注册、登录、当前用户、邮箱验证码找回密码 |
| 用户 | `/users` | 账号信息、个人资料、头像上传 |
| 班级 | `/classes` | 创建 / 列表 / 详情 / 加入 / 成员管理 / 生日提醒 / 解散 |
| 留言 | `/classes/{id}/messages` · `/messages` | 发布、分页、删除 |
| 相册 | `/classes/{id}/albums` · `/albums` · `/photos` | 相册与照片管理 |
| 动态 | `/classes/{id}/moments` · `/moments` · `/comments` | 动态、评论、点赞 |
| 好友 | `/friends` | 搜索、申请、审批、备注、解除关系 |
| 聊天 | `/chat` | 会话列表、消息收发、已读、撤回 |
| 群组 | `/groups` | 创建群、我加入的群、群资料、成员管理、退群、解散 |
| 通知中心 | `/notifications` | 分页列表、未读数、单条已读、全部已读 |
| 时光胶囊 | `/capsules` | 写信封存、我的信箱、班级胶囊墙、详情、撤回 |
| 文件 | `/files/upload` | 图片上传（白名单 + 10MB） |
| 后台 | `/admin` | 用户分页、启停、重置密码、统计、内容审核 |
| 实时通道 | `ws://host:8080/api/ws/chat?token=xxx` | WebSocket 消息推送 |

除注册 / 登录 / 找回密码外，所有接口需在请求头携带 `Authorization: Bearer {token}`。

---

## 🌐 部署上线

`deploy/` 目录提供一套面向「**只有普通账号、没有 sudo、不能装系统包**」的服务器的便携部署方案：
JDK 与 MySQL 都用免安装压缩包解到 `~/schoolmate_book/` 下，不写任何系统目录，整体删除即完全卸载。

```bash
bash ~/schoolmate_book/deploy/setup-server.sh   # 一次性初始化（生成随机密码 / JWT 密钥、建库导入）
bash ~/schoolmate_book/deploy/start-all.sh      # 启动 MySQL + 后端 + 前端
bash ~/schoolmate_book/deploy/start-tunnel.sh natapp <authtoken>   # 内网穿透，暴露公网入口
bash ~/schoolmate_book/deploy/stop-all.sh       # 全部停止
```

完整说明（目录布局、更新流程、端口设计、隧道选型与踩坑）见 **[deploy/README.md](deploy/README.md)**。

---

## 📚 文档

| 文档 | 内容 |
|---|---|
| [`docs/db-design.md`](docs/db-design.md) | 表结构设计、参考项目分析结论、审核状态机 |
| [`docs/api.md`](docs/api.md) | 83 个接口的契约速查、错误码、数据格式约定 |
| [`deploy/README.md`](deploy/README.md) | 便携部署与运维（含公网穿透、性能与限流注意事项） |

### 回归脚本

`docs/` 下三个脚本都支持用环境变量覆盖目标环境（`TEST_BASE` / `TEST_ORIGIN` / `TEST_WS`）：

| 脚本 | 覆盖范围 |
|---|---|
| [`docs/_test_v3.py`](docs/_test_v3.py) | 通知中心与时光胶囊，**63 项**（含到期可见性与定时任务，分两阶段执行） |
| [`docs/_test_group.py`](docs/_test_group.py) | 群聊 REST 全流程，45 项 |
| [`docs/_test_ws_private.mjs`](docs/_test_ws_private.mjs) | 私聊实时链路（WebSocket 双向收发 + ACK） |

```bash
python docs/_test_v3.py            # 阶段一：REST 全流程
python docs/_test_v3.py --phase2   # 阶段二：改库把 open_time 设到过去，验证到期可见 + 定时任务
python docs/_test_group.py
node docs/_test_ws_private.mjs
```

> ⚠️ 两个坑：
> 1. 回归脚本**必须带 `Origin` 头**（脚本已内置）。浏览器在非 GET 请求上一定会自动带 `Origin`，
>    不带的测试会漏掉整整一类 CORS 缺陷 —— 曾经 45 项全过，却掩盖了「浏览器里一点登录就报错」。
> 2. 用公网穿透地址跑密集请求会**误报**：natapp 免费隧道对突发连接数限流（返回
>    `429 Too much connections in one mintue`）。回归时请用 `TEST_BASE` 指向服务器本机
>    （经 SSH 端口转发），再用 `TEST_ORIGIN` 模拟穿透域名。

---

## 🤝 开发约定

- **提交信息**遵循 [Conventional Commits](https://www.conventionalcommits.org/)：`feat(server): xxx`、`fix(web): xxx`、`perf(web): xxx`、`chore(deploy): xxx`
- **契约先行**：先确定 DTO / VO 与错误码，再写实现
- **模块闭环**：每个模块走完「建表 → entity → mapper → service → controller → 文档自测」再进入下一个
- **分层纪律**：Controller 不碰 Mapper，Service 不感知 HTTP，实体不做出参

---

## 🧭 路线图与已知限制

**已知限制**

- 在线状态与会话注册表存在 JVM 内存中，**仅支持单节点部署**；多实例需改造为 Redis 发布订阅
- 图片存本地磁盘，未接入对象存储，多实例部署需先解决共享存储
- 前端主包体积偏大（Element Plus 全量引入 + 全量图标注册），仍有按需引入的优化空间
- 时光胶囊到期通知依赖 `@Scheduled` 扫描，**多实例下会重复发通知**，集群化需引入分布式锁（如 ShedLock）
- 找回密码的邮箱验证码存在 JVM 内存 `Map` 中，单节点可用，重启即失效；集群化需落 Redis

**后续计划**

- [ ] 班级公告（班长 / 管理员发置顶公告）
- [ ] 留言与评论支持 `@` 提醒，走通知中心 + WebSocket 推送
- [ ] 毕业去向标记 + ECharts 数据看板（去向分布 / 生源地 / 星座）
- [ ] 通讯录导出 Excel（EasyExcel 流式导出）
- [ ] Element Plus 按需引入，降低首屏体积
- [ ] 上传文件 magic number 校验（当前只查后缀）
- [ ] 接口限流 + Refresh Token（当前 JWT 24 小时过期需重新登录）
- [ ] 图片上传接入对象存储 / CDN
- [ ] 会话在线状态迁移至 Redis，支持多实例
- [ ] 补充 Service 层单元测试与 CI 流水线

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源，可自由用于学习与二次开发。

---

<div align="center">

如果这个项目对你有帮助，欢迎点个 ⭐ **Star** 支持一下

</div>
