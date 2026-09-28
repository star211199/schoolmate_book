# 接口契约速查

> 完整可调试文档：启动后端后访问 <http://localhost:8080/api/doc.html>（Knife4j）。
> 除注册/登录外，所有接口需在请求头携带 `Authorization: Bearer {token}`。
> 所有响应统一为 `{ code, message, data }`，`code = 20000` 表示成功。

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

## 认证 `/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 注册（账号 3-50 位字母数字下划线、密码 6-32 位） |
| POST | `/auth/login` | 登录，返回 `{token, userId, username, nickname, role}` |
| GET | `/auth/me` | 当前登录用户 |

## 用户 `/users`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/users/{id}` | 用户公开信息（手机号脱敏） |
| PUT | `/users/{id}` | 修改账号信息（仅本人或管理员） |
| PUT | `/users/{id}/password` | 修改密码 |
| GET | `/users/{id}/profile` | 查看个人主页资料 |
| PUT | `/users/{id}/profile` | 编辑个人主页资料 |

## 班级 `/classes`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/classes` | 创建班级（创建者成为 OWNER） |
| GET | `/classes` | 分页列表，`?keyword=` |
| GET | `/classes/my` | 我加入的班级 |
| POST | `/classes/join` | 凭邀请码加入 |
| GET | `/classes/{id}` | 班级详情（含成员数、当前用户角色） |
| PUT | `/classes/{id}` | 修改班级（创建者/管理员） |
| DELETE | `/classes/{id}` | 解散班级（创建者/管理员） |
| GET | `/classes/{id}/members` | 成员列表 |
| DELETE | `/classes/{id}/members/{userId}` | 移出成员（不能移除创建者） |

## 留言 `/classes/{classId}/messages`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{classId}/messages` | 留言分页（按时间倒序） |
| POST | `/classes/{classId}/messages` | 发布留言（需为班级成员） |
| DELETE | `/messages/{id}` | 删除留言（本人/管理员） |

## 相册与照片

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/classes/{classId}/albums` | 创建相册 |
| GET | `/classes/{classId}/albums` | 班级相册列表 |
| GET | `/albums/{id}` | 相册详情 |
| DELETE | `/albums/{id}` | 删除相册（创建者/管理员） |
| GET | `/albums/{id}/photos` | 照片列表 |
| POST | `/albums/{id}/photos` | 上传照片（multipart，字段 `file`） |
| DELETE | `/photos/{id}` | 删除照片（上传者/管理员） |

## 动态与评论

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{classId}/moments` | 动态分页（时间轴数据源） |
| POST | `/classes/{classId}/moments` | 发布动态（需为班级成员） |
| PUT | `/moments/{id}` | 修改动态 |
| DELETE | `/moments/{id}` | 删除动态 |
| GET | `/moments/{id}/comments` | 评论列表 |
| POST | `/moments/{id}/comments` | 发表评论 |
| DELETE | `/comments/{id}` | 删除评论 |

## 文件

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/files/upload` | 上传图片，返回 `{url}`，白名单 jpg/jpeg/png/gif/webp/bmp，单文件 ≤ 10MB |

## 后台管理 `/admin`（仅 ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/users` | 用户分页 |
| PUT | `/admin/users/{id}/status?status=NORMAL\|DISABLED` | 启停用户 |
| GET | `/admin/stats` | 仪表盘统计 |
| GET | `/admin/audit/pending?type=` | 待审核内容（MESSAGE/MOMENT/PHOTO） |
| PUT | `/admin/audit/{type}/{id}?passed=true\|false` | 审核通过/驳回 |

## 数据约定

- 时间格式：`yyyy-MM-dd HH:mm:ss`（日期 `yyyy-MM-dd`）
- 分页返回：`{ total, pageNum, pageSize, totalPages, records }`
- 实体不直接出参，统一经 VO 转换；手机号对外脱敏为 `138****8888`
