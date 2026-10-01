# 接口契约速查

> 完整可调试文档：启动后端后访问 <http://localhost:8080/api/doc.html>（Knife4j）。
> 机器可读版本：<http://localhost:8080/api/v3/api-docs>（OpenAPI 3）。
> 除注册/登录/找回密码外，所有接口需在请求头携带 `Authorization: Bearer {token}`。
> 所有响应统一为 `{ code, message, data }`，`code = 20000` 表示成功。
>
> 本文件按业务域分组，共 **83 个接口**。改动 Controller 后建议对照 `/v3/api-docs` 同步，
> 避免文档与实际路径漂移。

## 状态码

| code | 含义 |
|---|---|
| 20000 | 成功 |
| 40001 | 参数校验失败 |
| 40100 | 未认证 / Token 失效 |
| 40300 | 无权限 |
| 40400 | 资源不存在 |
| 40900 | 资源冲突 |
| 50000 | 服务器内部错误 |

> HTTP 状态码与业务 `code` 是分开的两层：命中 Controller 的业务失败返回 **HTTP 200 +
> 业务 code**；未命中任何 Controller 的路径由全局异常处理器直接返回 **HTTP 404 + code 40400**。

## 认证管理 `/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 注册（账号 3-50 位字母数字下划线、密码 6-32 位） |
| POST | `/auth/login` | 登录，返回 `{token, userId, username, nickname, role}` |
| GET | `/auth/me` | 当前登录用户 |
| GET | `/auth/mail-reset-enabled` | 是否已配置邮件服务（前端据此决定是否展示邮箱找回入口） |
| POST | `/auth/forgot-password` | 找回密码第一步：校验账号与邮箱匹配后发送 6 位验证码（10 分钟有效，60 秒内不可重发） |
| POST | `/auth/reset-password` | 找回密码第二步：凭验证码重置密码 |

> `forgot-password` / `reset-password` 需要 `spring.mail.host` 配置；未配置时前者返回
> 「邮件服务未配置」，此时应走管理员重置：`PUT /admin/users/{id}/reset-password`。
> 账号不存在与邮箱不匹配返回同一句提示，避免被用来探测有效账号。

## 用户管理 `/users`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/users/{id}` | 用户公开信息（手机号脱敏为 `138****8888`） |
| PUT | `/users/{id}` | 修改账号信息（仅本人或管理员） |
| PUT | `/users/{id}/password` | 修改密码（需校验原密码） |
| GET | `/users/{id}/profile` | 查看个人主页资料 |
| PUT | `/users/{id}/profile` | 编辑个人主页资料 |

## 班级管理 `/classes`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/classes` | 创建班级（创建者成为 OWNER，**并自动创建班级群与群会话**） |
| GET | `/classes` | 分页列表，`?keyword=` |
| GET | `/classes/my` | 我加入的班级 |
| POST | `/classes/join` | 凭邀请码加入（**入班即入群**） |
| GET | `/classes/{id}` | 班级详情（含成员数、当前用户角色、距毕业天数） |
| PUT | `/classes/{id}` | 修改班级（创建者/管理员） |
| DELETE | `/classes/{id}` | 解散班级（创建者/管理员，**班级群一并取消**） |
| GET | `/classes/{id}/members` | 成员列表 |
| DELETE | `/classes/{id}/members/{userId}` | 移出成员（不能移除创建者） |
| GET | `/classes/{id}/birthday-reminders` | 生日提醒：未来 N 天内过生日的成员 |

## 班级留言 `/classes/{classId}/messages`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{classId}/messages` | 留言分页（按时间倒序） |
| POST | `/classes/{classId}/messages` | 发布留言（需为班级成员） |
| DELETE | `/messages/{id}` | 删除留言（本人或管理员） |

## 班级相册

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{classId}/albums` | 班级相册列表 |
| POST | `/classes/{classId}/albums` | 创建相册 |
| GET | `/albums/{id}` | 相册详情 |
| DELETE | `/albums/{id}` | 删除相册（创建者/管理员） |
| GET | `/albums/{id}/photos` | 照片列表 |
| POST | `/albums/{id}/photos` | 上传照片（multipart，字段 `file`） |
| DELETE | `/photos/{id}` | 删除照片（上传者/管理员） |

## 班级动态

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{classId}/moments` | 动态分页（时间轴数据源） |
| POST | `/classes/{classId}/moments` | 发布动态（需为班级成员） |
| PUT | `/moments/{id}` | 修改动态 |
| DELETE | `/moments/{id}` | 删除动态 |
| GET | `/moments/{id}/comments` | 评论列表 |
| POST | `/moments/{id}/comments` | 发表评论（`parentId` 非空表示回复某条评论） |
| DELETE | `/comments/{id}` | 删除评论（评论人或管理员） |
| POST | `/moments/{id}/like` | 点赞 / 取消点赞（**切换语义**：已赞则取消） |

## 好友

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/friends` | 我的好友列表（含在线状态与私聊会话ID） |
| PUT | `/friends/{friendUserId}` | 设置好友备注名与分组 |
| DELETE | `/friends/{friendUserId}` | 删除好友（聊天记录保留，重新加回后仍可见） |
| GET | `/friends/requests` | 收到的好友申请，`?status=PENDING\|ACCEPTED\|REJECTED` |
| GET | `/friends/requests/sent` | 我发出的好友申请 |
| GET | `/friends/requests/pending-count` | 待我处理的申请数（「新朋友」红点） |
| POST | `/friends/requests` | 发起好友申请 `{toUserId, message}` |
| PUT | `/friends/requests/{requestId}/accept` | 同意申请（**同时建立私聊会话**） |
| PUT | `/friends/requests/{requestId}/reject` | 拒绝申请 |
| GET | `/friends/search` | 搜索用户，`?keyword=`（账号/昵称/真实姓名） |
| GET | `/friends/relation/{targetUserId}` | 我与某用户的关系：`SELF/NONE/IS_FRIEND/PENDING_SENT/PENDING_RECEIVED` |

## 群聊

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/groups` | 我加入的群列表 |
| POST | `/groups` | 创建群聊（自己自动成为群主） |
| GET | `/groups/{groupId}` | 群资料（含我在群内的角色） |
| PUT | `/groups/{groupId}` | 修改群名/公告/头像（群主或管理员） |
| DELETE | `/groups/{groupId}` | 解散群（仅群主，且仅自建群） |
| GET | `/groups/{groupId}/members` | 群成员分页列表（群主在前，返回 PageResult） |
| POST | `/groups/{groupId}/members` | 批量拉人入群（群主或管理员） |
| DELETE | `/groups/{groupId}/members/{userId}` | 移出成员（**传自己的 ID 等同于退群**） |
| POST | `/groups/{groupId}/quit` | 退出群聊（班级群不可退，需先退出班级） |

## 会话与消息

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/chat/sessions` | 我的会话列表（含未读数与最后一条消息摘要，按置顶+时间排序） |
| GET | `/chat/sessions/{sessionId}` | 会话详情 |
| POST | `/chat/sessions/private/{targetUserId}` | 获取或创建与某用户的私聊会话，返回会话ID |
| GET | `/chat/sessions/{sessionId}/messages` | 历史消息，`?beforeId=&size=`（向上翻页，**返回 List 非 PageResult**，按时间正序） |
| GET | `/chat/sessions/{sessionId}/messages/after` | 增量拉取（断线重连补消息），`?afterId=` |
| PUT | `/chat/sessions/{sessionId}/read` | 上报已读位点，`?lastReadMessageId=`（**查询参数，不是 body**） |
| POST | `/chat/messages` | 发送消息（WebSocket 不可用时的 REST 兜底通道） |
| PUT | `/chat/messages/{messageId}/recall` | 撤回消息（仅发送者本人，限 2 分钟内） |

> 实时通道为 WebSocket：`ws://host:8080/api/ws/chat?token={jwt}`。
> 鉴权走 URL 查询参数（浏览器原生 WebSocket 无法携带自定义头），在握手阶段验签。
> 未读数不落库，由 `COUNT(*) WHERE session_id=? AND id > last_read_message_id` 实时计算。

## 通知中心 `/notifications`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/notifications` | 通知分页，`?pageNum=&pageSize=&unreadOnly=true` |
| GET | `/notifications/unread-count` | 未读通知数（导航栏铃铛角标） |
| PUT | `/notifications/{id}/read` | 标记单条已读 |
| PUT | `/notifications/read-all` | 全部标记已读 |

通知类型与跳转目标（`bizType` 决定前端去向，`bizId` 是**业务页面 ID 而非事件 ID**）：

| type | 触发时机 | bizType | bizId | 前端跳转 |
|---|---|---|---|---|
| `FRIEND_REQUEST` | 收到好友申请 | `FRIEND` | 申请ID | `/contacts` |
| `FRIEND_ACCEPTED` | 对方同意申请 | `FRIEND` | 申请ID | `/contacts` |
| `MOMENT_LIKE` | 动态被点赞（不含自赞、不含取消赞） | `MOMENT` | **classId** | `/classes/{classId}?tab=moments` |
| `MOMENT_COMMENT` | 动态被评论（不含自评；二级评论通知被回复人） | `MOMENT` | **classId** | `/classes/{classId}?tab=moments` |
| `CAPSULE_OPENED` | 时光胶囊到期，由定时任务产生 | `CAPSULE` | 胶囊ID | `/capsules` |

> 新通知会通过 WebSocket 推送 `{type:"NOTIFICATION"}` 帧，前端铃铛角标实时 +1；
> 列表数据不推送，由通知页按需拉取。

## 时光胶囊 `/capsules`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/capsules` | 写一封信并封存 `{title, content, openTime, openType, classId?}` |
| GET | `/capsules/my` | 我的胶囊列表（含倒计时，**未到点不返回 content**） |
| GET | `/capsules/class/{classId}` | 班级公开胶囊墙（需为班级成员） |
| GET | `/capsules/{id}` | 胶囊详情（未到开启时间不返回 content） |
| DELETE | `/capsules/{id}` | 删除胶囊（仅本人且封存中，已开启不允许删除） |

可见性规则（三条都必须记住）：

1. **内容可见性只看时间**：`now >= openTime` 才返回 `content`，否则为 `null`。
   不依赖 `status` 字段 —— 所以定时任务还没跑到时，到点的信也能立即打开。
2. **SELF 仅写信人可见**：他人访问返回 `40400`（连存在性都不暴露）。
3. **PUBLIC 需班级成员**：非成员访问详情或胶囊墙返回 `40300`；向未加入的班级投信同样被拒。

`status` 字段只服务于「到期通知」：定时任务（`@Scheduled`，60 秒一轮）把刚跨过开启时间的信
置为 `OPENED` 并给写信人发通知 —— 通知需要一个「从未知到已知」的状态跃迁作触发点。

## 文件管理

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/files/upload` | 上传图片，返回 `{url}`，白名单 jpg/jpeg/png/gif/webp/bmp，单文件 ≤ 10MB |

## 后台管理 `/admin`（仅 ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/users` | 用户分页 |
| PUT | `/admin/users/{id}/status?status=NORMAL\|DISABLED` | 启停用户 |
| PUT | `/admin/users/{id}/reset-password` | 重置密码，**返回随机新密码，仅显示一次**，需线下告知用户 |
| GET | `/admin/stats` | 仪表盘统计 |
| GET | `/admin/audit/pending?type=` | 待审核内容（MESSAGE/MOMENT/PHOTO） |
| PUT | `/admin/audit/{type}/{id}?passed=true\|false` | 审核通过/驳回 |

## 数据约定

- 时间格式：`yyyy-MM-dd HH:mm:ss`（日期 `yyyy-MM-dd`）
- 分页返回：`{ total, pageNum, pageSize, totalPages, records }`
- 实体不直接出参，统一经 VO 转换；手机号对外脱敏为 `138****8888`
- **VO 里的主键字段名不统一**：`ChatSessionVO.id`、`ChatGroupVO.id`（都不是 `sessionId`/`groupId`）
- **输入内容一律转义**：留言/评论/动态/消息落库前经 `HtmlUtils.htmlEscape`，前端用 `{{ }}` 插值，
  严禁 `v-html`
- 未知路径统一返回 HTTP 404 + `code 40400`，不会落到 500 兜底
