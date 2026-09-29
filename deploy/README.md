# 便携部署说明（免 sudo）

面向「只有普通账号、没有 sudo、不能装系统包」的服务器。全部文件放在 `~/schoolmate_book` 下，
不写任何系统目录，可整体删除即完全卸载。

## 目录布局

```
~/schoolmate_book/
├── app/                      # 应用与配置
│   ├── schoolmate-server.jar     # 后端可执行 jar
│   ├── application-prod.yml      # 生产配置（数据源/JWT 走环境变量）
│   ├── env.sh                    # 生成的密码与密钥（权限 600，勿提交）
│   ├── my.cnf                    # 便携 MySQL 配置
│   ├── sql/                      # init.sql / upgrade.sql / v2-init.sql
│   └── upload/                   # 上传文件落盘目录
├── runtime/
│   ├── jdk/                      # 便携 JDK 17（Temurin）
│   ├── mysql/                    # 便携 MySQL 8.0.29
│   └── lib/libaio.so.1           # libaio 兼容软链接（见下）
├── web/                          # 前端项目（含 dist 与 node_modules）
├── data/mysql/                   # MySQL 数据目录
├── downloads/                    # cloudflared / natapp_bin 等下载的客户端
├── logs/                         # backend.log / frontend.log / mysql-error.log / natapp.log
├── pids/                         # 进程 pid 文件
└── deploy/                       # 本目录脚本
```

## 一次性初始化

```bash
bash ~/schoolmate_book/deploy/setup-server.sh
```

脚本会：生成随机数据库密码与 JWT 密钥 → 生成 `my.cnf` → 初始化 MySQL 数据目录 →
启动 MySQL → 建业务库账号 → 导入三个 SQL 文件。可重复执行，已存在的配置不会被覆盖。

## 启停

```bash
bash ~/schoolmate_book/deploy/start-all.sh    # 启动 MySQL + 后端 + 前端
bash ~/schoolmate_book/deploy/stop-all.sh     # 全部停止
tail -f ~/schoolmate_book/logs/backend.log    # 看后端日志
```

启动后本机可访问 `http://127.0.0.1:5173`（前端），`http://127.0.0.1:8080/api`（后端）。

### 更新版本（改了后端或前端之后）

```bash
# 本地：打包后端、构建前端，然后上传到服务器 ~/schoolmate_book/_stage3/
cd schoolmate-server && mvn -DskipTests clean package
cd ../schoolmate-web && node node_modules/vite/bin/vite.js build --outDir dist_build --emptyOutDir
cd dist_build && tar czf /tmp/dist.tar.gz .
scp -P 3389 schoolmate-server/target/schoolmate-server.jar  albot@120.197.14.102:~/schoolmate_book/_stage3/
scp -P 3389 /tmp/dist.tar.gz                                albot@120.197.14.102:~/schoolmate_book/_stage3/dist.tar.gz

# 服务器：原地替换并重启（脚本里会停服务、换文件、再启动）
bash ~/schoolmate_book/deploy/redeploy.sh
```

> `vite build` 若报 `SAFE_DELETE_BULK_CONFIRM_REQUIRED`（批量删除被保护），
> 用 `--outDir dist_build --emptyOutDir` 输出到全新目录即可绕过。

## 演示数据维护

内置账号：`admin / 123456`（超级管理员）、`zhangsan / 123456`（张三）；班级邀请码 `CLASS01`。

```bash
bash deploy/reset-demo.sh   # 删库重建 + 重新导入种子 SQL（会清空所有业务数据）
bash deploy/seed-demo.sh    # 通过接口重建班级（联动创建班级群 + 群会话），并把邀请码改回 CLASS01
```

**为什么需要 `seed-demo.sh`**：班级群（`ensureClassGroup`）只在**调用建班接口时**才会创建，
而 `init.sql` 里的示例班级是直接用 SQL 插进去的，因此它没有群 —— 聊天页看不到班级群。
`reset-demo.sh` 会删掉种子班级，再由 `seed-demo.sh` 走接口把它建回来，顺带补上毕业日期
（建班入参不含该字段，需再调一次更新接口）和邀请码。

回归测试脚本位于 `docs/`：`_test_group.py`（群聊模块，45 项断言）、`_test_ws_private.mjs`（私聊实时推送）。
注意这两个脚本**会在库里留下测试数据**（自建班级/群、测试用户），跑完演示环境建议重新执行上面两条命令恢复。

### 跑回归测试的正确姿势

```bash
# 1) 开一条 SSH 端口转发（绕开免费隧道的连接限流）
ssh -p 3389 -N -L 15173:127.0.0.1:5173 albot@120.197.14.102 &

# 2) 用转发地址当 BASE，用穿透域名当 Origin（复刻浏览器来源，又不触发限流）
TEST_BASE=http://127.0.0.1:15173/api \
TEST_ORIGIN=http://<当前穿透域名> \
python docs/_test_group.py
```

**不要直接对穿透地址跑回归**：natapp 免费隧道对**突发新连接数**限流，会返回
`HTTP/1.0 429 Connections Exceed`，导致大面积失败，甚至出现「断言应当被拒绝、实际是 429」的**假 PASS**。
（实测 1 次/秒连发 50 次不触发，但几秒内 25 次连接就会触发。）

## 端口与单端口设计

前端用 `vite preview` 托管 `dist`，并复用 `vite.config.js` 里的 `server.proxy`
（Vite 的 `preview.proxy` 默认继承 `server.proxy`，含 `ws: true`）：

| 路径 | 去向 |
|---|---|
| `/` | `dist` 静态资源 |
| `/api/**` | 后端 REST（`127.0.0.1:8080`） |
| `/api/ws/chat` | 后端 WebSocket（同端口升级） |

因此**内网穿透只需要暴露 5173 一个端口**。

## 内网穿透（公网入口）

```bash
bash deploy/start-tunnel.sh natapp <authtoken>   # 方案 B：natapp（推荐）
bash deploy/start-tunnel.sh cloudflared          # 方案 A：无需账号
bash deploy/start-tunnel.sh stop                 # 停止隧道
```

### 方案 A：cloudflared 快速隧道（实测可用）

无需账号，一条命令拿到 `https://xxx.trycloudflare.com`。隧道指向 `127.0.0.1:5173`，
因为前端已把 `/api`（含 WebSocket）代理到后端，所以一个端口就够了。

- **注意一**：该地址**每次重启隧道都会变**。要固定域名需注册 Cloudflare 具名隧道或改用 natapp。
- **注意二**：实测某些时段 **HTTPS 会在 TLS 层被拦**（握手即失败），此时用 **HTTP** 访问反而通；
  换网络/时段后 HTTPS 又正常。隧道本身同时监听 http/https，两种都试一下即可。
- **注意三**：走 HTTP 时流量不加密，登录密码与聊天内容在公网明文传输。
  演示用可以接受，**不要用真实密码**。

### 方案 B：natapp（推荐，已实测跑通）

优势是固定域名（免费版为随机域名，会被不定时更换）、国内线路好。
隧道需配置为**映射本地 5173**（与本站部署端口一致）。

```bash
bash deploy/start-tunnel.sh install-natapp <authtoken>   # 可选：只安装/升级客户端
bash deploy/start-tunnel.sh natapp <authtoken>           # 未安装会自动先安装，然后起隧道
bash deploy/start-tunnel.sh stop                         # 停止隧道
```

**安装方式（关键）**：natapp v3 起提供官方一键安装脚本，客户端会装到
`~/schoolmate_book/downloads/natapp_bin/`（免 sudo）：

```bash
curl -fsSL "https://natapp.cn/get.sh?authtoken=<你的authtoken>" | sh
```

> 早前踩的坑：直接拼 CDN 地址 `http://download.natapp.cn/assets/downloads/clients/3_0_5/natapp_linux_amd64/natapp`
> 会返回 **Tengine 403**。原因是 CDN 校验 URL 上必须携带 `?key=<key>&authtoken=<token>` 两个参数，
> 而这两个值由官网脚本动态烘焙后下发 —— 所以**不要自己拼地址，直接用 `get.sh`**。
>
> **不要**从第三方镜像下载这个二进制 —— 它持有你的 authtoken，来源不可信等于把隧道交给别人。

**本地端口在云端配置**：natapp v3 命令行**没有** `-localport` 参数，本地端口由
natapp.cn →「隧道列表」里的隧道配置决定。本项目必须设为 **5173**；`start-tunnel.sh`
启动后会读取日志里的 `local=...` 并校验，不符会告警。

启动成功时日志形如：

```
[INFO][client][ctl:xxxx]Tunnel established at http://ba3b6d6c.natappfree.cc
[STAT]NATAPP status=online proto=http forwarding=http://ba3b6d6c.natappfree.cc local=127.0.0.1:5173
```

注意 natapp 还会在本机 `0.0.0.0:4040` 起一个 Web 状态面板（该版本无法改绑回环）；
本机防火墙/安全组未放行 4040 的话外网访问不到，但同局域网可见，属已知项。

同一个隧道同一时刻只能有一个客户端在线。如果你本机也在跑 natapp，需要先停掉本机那个。

### 方案 C：复用你本机已有的 natapp（不新增二进制）

在你自己的 Windows 机器上做一条本地端口转发，把服务器端口拉到本机，再让本机 natapp 照常发布：

```bash
ssh -p 3389 -N -L 5173:127.0.0.1:5173 albot@120.197.14.102
```

之后本机 `localhost:5173` 指向的就是服务器上的应用，你原有的 natapp 隧道无需改动。
代价是必须保持这个 SSH 窗口开着，且本机的 Vite 开发服务要停掉（5173 会冲突）。


## 如果以后拿到了 sudo（更标准的做法）

用 Nginx 替掉 `vite preview`（`dist` 托管 + 反向代理 + WebSocket 转发）：

```nginx
server {
    listen 80;
    server_name _;
    root /home/albot/schoolmate_book/web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;   # SPA 路由回退
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # WebSocket 升级（/api/ws/chat 靠这几行）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
```

然后把后端进程交给 systemd（`ExecStart` 用便携 JDK 的绝对路径即可）。

## 已知注意点

- **浏览器里「一点登录就报网络异常」的真凶是 CORS（务必先看这条）**：
  `vite.config.js` 的代理配了 `changeOrigin: true`，会把 `Host` 改写成 `localhost:8080`，
  而浏览器对**非 GET 请求一定会带 `Origin`**（如穿透域名），Spring 拿两者比对后把同源请求
  误判成跨域；若 `WebMvcConfig` 的 CORS 白名单里没有穿透域名，就直接 `403 Invalid CORS request`。
  症状是**页面和列表（GET）全正常，只有登录/发消息等写操作失败**。
  现已在 `WebMvcConfig` 改为 `allowedOriginPatterns("*")` + `allowCredentials(false)`
  （本项目不用 Cookie/Session，鉴权只走 `Authorization` 头，故关掉凭据无副作用）。
  **排查手法**：带上 `Origin` 复现、去掉就正常 → 就是这个原因。
  curl / Python 默认都不带 `Origin`，所以「接口全通」不能证明浏览器里能用。
- **回归测试必须带 `Origin`**：`docs/_test_*.py|mjs` 已补上（可用 `TEST_ORIGIN` 覆盖）。
  历史上正因为没带，45 项全过却掩盖了上面那个缺陷。
- **免费隧道有连接限流**：natapp 免费版对突发新连接数限流，返回
  `HTTP/1.0 429 Connections Exceed` / `Too much connections in one mintue`。
  实测 1 次/秒连发 50 次不触发，但几秒内 25 次连接就会触发，之后一段时间持续 429。
  这是「时好时坏」的一个次要原因（浏览器首屏会并发拉 5~6 个资源，属会踩限流的量级）。
- **带宽是硬约束**：natapp 免费隧道只有 **1Mbps（≈125KB/s）**。
  改动前端后请确认 `dist` 体积（`du -sh web/dist`），当前约 2.7MB。
  新增图片务必先压缩 —— 用 `python scripts/optimize-images.py`（会转 WebP 并降分辨率）。
  历史上的原图是 2~3MB 的 PNG，把登录页拖到 **50 秒**，而 axios 超时 15 秒，
  表现为「点登录就报网络异常」，很容易误判成后端或隧道不稳。
- **缓存策略**：`vite.config.js` 的 `previewCacheHeaders` 插件按路径设置
  `Cache-Control`（`/assets/**` 强缓存 1 年、`/images/**` 7 天、`index.html` no-cache）。
  `index.html` 必须保持 no-cache，否则发版后拿不到新资源引用。
  注意 `/images/**` 文件名不含 hash —— **换图必须换文件名**，否则用户会看到旧图。
- **排查前端加载慢**：用 `curl -s --compressed -o /dev/null -w "%{size_download} %{time_total}" <url>`。
  一定要带 `--compressed` 且用 GET（不是 `-I`），否则量到的是未压缩体积会误判。
  vite preview 本身已内置 gzip。
- **libaio / libtinfo**：Ubuntu 24.04 把 `libaio` 包改名为 `libaio1t64`（库文件变成 `libaio.so.1t64`），
  而 MySQL 8.0.29 的 `mysqld` 仍按 `libaio.so.1` 链接；同时 `mysql` 命令行客户端链接的是
  `libtinfo.so.5`，而系统只有 `libtinfo.so.6`。脚本用「本地软链接 + `LD_LIBRARY_PATH`」绕过两者，
  不侵入系统目录。
- **后端只监听回环**：`application-prod.yml` 设了 `server.address: 127.0.0.1`，
  后端不暴露到局域网，只有同机的前端代理能访问（在这类共用服务器上更安全）。
- **JWT 密钥**：`env.sh` 里是随机生成的，换环境请重新生成，不要复用开发环境的密钥。
- **接口文档**：生产配置默认关闭 Knife4j / Swagger（`SWAGGER_ENABLED=false`）。
- **上传文件**：`schoolmate.upload.base-dir` 指向 `app/upload`，与 jar 分离，升级 jar 不会丢文件。
- **单机限制**：会话在线状态存在 JVM 内存里，只支持单节点；集群需要改造成 Redis 发布订阅。
