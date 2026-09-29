# schoolmate_book 项目长期记忆

大学同学录（Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + JWT / Vue 3 + Vite + Pinia + Element Plus）。
仓库结构：`schoolmate-server`（后端）、`schoolmate-web`（前端）、`sql/`（建表脚本）、`docs/`（设计文档与回归测试脚本）。

## 环境与启动
- 本地 MySQL：`root` / `1234`，库名 `schoolmate_book`。mysql 客户端在 `/d/MySQL/MySQL Server 8.0/bin/mysql`。
- 后端：`cd schoolmate-server && mvn spring-boot:run` → `:8080`，**context-path 为 `/api`**（所以 REST 前缀 `/api`，WebSocket 为 `ws://host:8080/api/ws/chat`）。
- 前端：`cd schoolmate-web && npm run dev` → `:5173`，通过 Vite proxy 转发 `/api` 到 8080（含 `ws:true`）。
- 测试账号（密码均 `123456`）：`admin` / `zhangsan` / `lisi` / `wangwu`。后两个用于演示群聊（群聊需 ≥3 人）。

## 易踩的坑
- ⚠️ **仓库是公开的**（`star211199/schoolmate_book`）。提交前必须扫密钥/authtoken，别把凭据推进去。
- **返回码是 `20000` 而非 `200`**（`ResultCode.SUCCESS(20000,"操作成功")`）。写测试断言时用 20000。
- 用户表叫 **`user`**（不是 `sys_user`）；班级表是 `class_info` / `class_member`；v2 聊天表 `chat_session` / `chat_session_user` / `chat_message` / `chat_group` / `chat_group_member` / `friendship` / `friend_request` / `time_capsule`。
- VO 里的主键字段名不统一：`ChatSessionVO.id`、`ChatGroupVO.id`（都不是 `sessionId`/`groupId`）。
- `ChatController.pageMessages` 返回 **List**（不是 PageResult），参数是 `beforeId` + `size`；`PUT /chat/sessions/{id}/read` 的 `lastReadMessageId` 是**查询参数**不是 body。
- `GroupController` 的成员列表返回 PageResult（取 `.records`）。
- 用 Python/Node 写测试脚本时注意：JS 的 `%` 是取模不是字符串格式化（要用 `console.log('a=%s', x)`）。

## 关键工程约定
- 建表统一带 `deleted`（`@TableLogic` 逻辑删除）与 `create_time/update_time`（`@TableField(fill=...)` 自动填充）。
- 唯一索引都要把 `deleted` 纳入，例如 `uk_pair(user_a_id,user_b_id,deleted)`、`uk_biz(group_type,class_id,deleted)`；插入时捕获 `DuplicateKeyException` 实现幂等。
- **聊天内容严禁 `v-html`**，服务端已用 `HtmlUtils.htmlEscape` 转义；前端用 `{{ }}` 插值。
- 消息「先落库再推送」，推送放 `TransactionUtil.afterCommit()`，避免幽灵消息。
- 未读数不落字段，用 `COUNT(*) WHERE session_id=? AND id > last_read_message_id` 实时计算。
- WebSocket 鉴权走 URL `?token=`（浏览器原生 WS 不能带自定义头），在 `WsHandshakeInterceptor` 验签。
- **未知路径统一 404**：`GlobalExceptionHandler` 单独处理 `NoResourceFoundException`（Spring 6.1 起
  未命中 Controller 会抛这个）与 `NoHandlerFoundException`，返回 HTTP 404 + `code 40400`，
  日志只记一行 WARN。别让它们落到 `Exception` 兜底去（那样会变成 500 并打完整堆栈）。
- **CORS 放开来源**：穿透域名是动态的，白名单无法枚举，故用 `allowedOriginPatterns("*")` +
  `allowCredentials(false)`（全项目不用 Cookie/Session，鉴权只靠 `Authorization` 头）。

## 待办 / 注意
- 会话注册表在 JVM 内存中，**仅支持单节点**；集群化需把 `WsSessionRegistry.pushToUser` 改为 Redis Pub/Sub（已预留说明）。
- 回归脚本：`docs/_test_group.py`（群聊 REST，45 项）、`docs/_test_ws.mjs`（实时链路，25 项）。改完聊天相关代码建议两个都跑一遍。
- 写测试时 `clientMsgId` 必须每轮唯一，否则会命中幂等分支导致「推送时有时无」的假故障。

## 远程部署（2026-09-29 完成）
服务器：`ssh -p 3389 albot@120.197.14.102`（Ubuntu 24.04，多人共用的实验室机器）。
**全服务器无 sudo 权限、无 docker 组权限，因此走「免 sudo 便携部署」**，所有文件在 `~/schoolmate_book/` 下。

- 部署脚本在仓库 `deploy/`（也已上传到服务器同名目录）：
  `setup-server.sh`（一次性初始化，幂等）/ `start-all.sh` / `stop-all.sh` / `start-tunnel.sh`
  / `reset-demo.sh` / `seed-demo.sh` / `redeploy.sh`（原地更新 jar+dist 并重启）/ `README.md`
- 服务器布局：`app/`（jar+配置+env.sh+my.cnf+sql+upload）、`runtime/`（jdk/mysql/lib）、`web/`（前端含 dist）、`data/`、`logs/`、`pids/`
- 密钥在 `app/env.sh`（权限 600，随机生成，勿提交）
- 单端口设计：`vite preview` 托管 dist 并把 `/api`（含 WS）代理到后端，因此**只需暴露 5173**。
  注意 Vite 的 `preview.proxy` **默认继承** `server.proxy`，不用重复配。
- 后端生产配置 `server.address: 127.0.0.1`（只听回环），MySQL 也只听 127.0.0.1:3306。

### 该服务器上的两个硬约束（换环境也值得记）
1. **没有 sudo、不在 docker 组**：`apt` 与 `docker` 都用不了，只能便携化下载解压到 home 目录运行。
2. **公网只有 3389 通**：`120.197.14.102` 前面有一台 Windows 机器做端口转发（502 响应体是
   Windows `os error 10061`），大片端口被转走，8080/8443 被拦。要做公网访问只能：改网关转发规则、
   或在服务器上跑穿透客户端。用「临时监听 + 外部 curl」可快速判定哪些端口真能通。

### Ubuntu 24.04 跑 MySQL 8.0 预编译版的动态库兼容（通用坑）
- `mysqld` 需要 `libaio.so.1`，系统只有 `libaio.so.1t64`（包名 libaio1t64）
- `mysql` 客户端需要 `libtinfo.so.5`，系统只有 `libtinfo.so.6`
- 解法：`mkdir -p runtime/lib && ln -sf <系统库> runtime/lib/<要找的名字>`，再 `export LD_LIBRARY_PATH=runtime/lib`
  用 `ldd | grep "not found"` 逐个确认。

### 其他踩坑
- `application-prod.yml` 内**不能**声明 `spring.profiles.active`（profile 专属文件里禁写），会抛 `InvalidConfigDataPropertyException`。
- **natapp Linux 客户端下载 403 的真正原因**：CDN 要求 URL 带 `?key=<key>&authtoken=<token>` 两个参数
  （key 当前是 `6561`），自己拼 `download.natapp.cn/assets/downloads/clients/...` 必然 403。
  正解是用官方一键脚本（免 sudo，装到指定目录）：
  `NATAPP_INSTALL_DIR=~/xx curl -fsSL "https://natapp.cn/get.sh?authtoken=<token>" | sh`
  装完得到 `natapp` + `run_natapp.sh`。**仍不要**用第三方镜像（该二进制持有 authtoken）。
- **natapp v3 没有 `-localport` 参数**，本地端口在云端「隧道列表」里配；启动后看日志
  `forwarding=http://xxx.natappfree.cc local=127.0.0.1:5173` 确认。它还会在 `0.0.0.0:4040`
  起 Web 面板（该版本无法改绑回环）。
- cloudflared 快速隧道：早前实测 HTTP 可用、**HTTPS 在 TLS 层被拦**，但重测时 HTTPS 又完全正常
  （TLS 握手 0.33s，wss 也通）—— 属**网络/时段的偶发拦截，不是配置问题**。用 `curl` 手搓 WS 握手会被
  Cloudflare 拒成 `400 Can "Upgrade" only to "WebSocket"`，这是 curl 的问题，真 WebSocket 客户端正常。
- **`pkill -f "xxx"` 会匹配到自己所在的 shell**（命令行里含同样字符串），把自己杀掉导致后续命令不执行。
  远程执行时用方括号写法 `pkill -f "[x]xx"`。
  但**方括号写法也会失效**：如果同一条 ssh 命令里别处出现了该字符串的明文（例如
  `ssh host 'pkill -f "[s]choolmate-server.jar"; cp x schoolmate-server.jar'`），进程命令行仍会命中。
  凡是要 kill 的服务名会出现在命令里，就把整个流程写成 `deploy/*.sh` 脚本文件再 `bash 脚本` 执行
  （脚本进程命令行只有 "bash deploy/xxx.sh"，不会自伤）—— `redeploy.sh` 就是这么来的。

### 浏览器里「点登录就报网络异常」的真因：CORS（同类项目必看）
症状：**所有 GET 页面/列表都正常，但一点登录/注册/发消息就报错**；前端文案是
「网络异常，请稍后重试」（因为 403 响应体是纯文本没有 message 字段，落到了兜底分支）。

根因链条（三个条件缺一不可，很容易漏掉）：
1. 浏览器在**非 GET/HEAD 请求**上一定会自动带 `Origin` 头（GET 不带）→ 所以只有写操作出问题。
2. `vite.config.js` 代理配了 `changeOrigin: true`，会把 `Host` 改写成 `localhost:8080`；
   Spring 的 `CorsUtils.isSameOrigin` 用 `Origin` 与 `Host` 比对，于是把同源请求误判成跨域。
3. `WebMvcConfig.addCorsMappings` 的 `allowedOrigins` 白名单只写了 localhost，**没含穿透域名**
   → `checkOrigin` 失败 → 403 `Invalid CORS request`。

修复：白名单改为 `allowedOriginPatterns("*")`，并把 `allowCredentials` 从 `true` 改成 `false`
（全项目不用 Cookie/Session，鉴权只靠 `Authorization` 头里的 JWT，关掉凭据没有副作用）。

**最该记住的教训**：`docs/_test_*.py|mjs` 以前用 curl/urllib/fetch 发请求，**都不带 Origin**，
所以 45 项回归全过却完全掩盖了这个缺陷。现已在这两个脚本里补上 `Origin`（按 BASE 推导，模拟浏览器）。
以后写任何 HTTP 回归测试，都要带上 Origin。

### natapp 免费隧道的连接限流（回归测试不能直接打穿透地址）
- 触发时返回 `HTTP/1.0 429 Connections Exceed`，正文 `Too much connections in one mintue,Please try later`。
- 实测：**1 次/秒持续 50 次不触发**；但**几秒内 25 次连接这类突发会触发**，之后一段时间持续 429。
- 后果：密集发请求的回归脚本直接打穿透地址会**大面积误报**，甚至出现
  「非群主解散群被拒 → 期望失败但实际是 429」这种**假 PASS**。
- 正确做法：回归时用 `TEST_BASE` 指向服务器本机 `http://127.0.0.1:5173/api`（走 SSH 端口转发
  `ssh -p 3389 -N -L 15173:127.0.0.1:5173 albot@...`），再用 `TEST_ORIGIN=http://<穿透域名>`
  模拟浏览器来源 —— 既不影响链路保真度，又绕开限流。
- 真实浏览器首屏会并发拉 5~6 个资源，属于会踩限流的量级，这是「时好时坏」的次要原因。

### 演示环境数据（重要业务坑）
- **`ensureClassGroup` 只在「调用建班接口」时触发**（`ClassServiceImpl.create`）。`init.sql` 里
  样本班级是直接 SQL 插入的，所以**没有班级群**，聊天页看不到班级群。
- 因此演示环境维护分两步：`deploy/reset-demo.sh`（删库重导入种子，并删掉种子班级）→
  `deploy/seed-demo.sh`（走接口重建班级，联动生成班级群+会话，再补毕业日期并改回邀请码 `CLASS01`）。
- 建班入参 `ClassCreateDTO` **不含** `graduationDate`，要用 `PUT /classes/{id}` 补；邀请码是随机生成的，
  想让邀请码保持 `CLASS01` 只能改库。
- `docs/_test_group.py` 与 `_test_ws_private.mjs` **会在库里留测试数据**（自建班级/群、注册的测试用户、
  私聊消息）。跑完演示环境记得用上面两条脚本恢复干净。

### 前端性能（1Mbps 隧道下的硬约束，改前端前必读）
- 公网走的是 **nateapp 免费隧道，只有 1Mbps**。**任何新增图片都必须先压缩**，
  原图曾是 2~3MB 的 PNG，直接把登录页拖到 50 秒（axios 超时 15s → 报「网络异常」）。
- 现有图片已全部转 WebP（`scripts/optimize-images.py`，14.9MB→0.85MB）。
  **图片文件名不含 hash**，换图要换文件名（`/images/**` 缓存 7 天）。
- 排查前端「慢」时：一定要用 `curl --compressed`（GET，不是 `-I`）量真实传输体积。
  vite preview **已内置 gzip**，别误判。
- 缓存策略靠 `vite.config.js` 的 `previewCacheHeaders` 插件在 `writeHead` 里改写 ——
  因为 sirv 走 `writeHead(status, headers)`，**传入的 headers 会覆盖 `setHeader`**。
- 主 JS 包 gzip 后 400KB：`main.js` 里 Element Plus 全量引入 + 全量注册图标所致，尚未按需引入。
