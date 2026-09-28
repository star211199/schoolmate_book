# 数据库设计说明

## 一、参考项目分析结果

分析对象（GitHub，仅参考功能与表设计，不复用其代码）：

| 项目 | 后端 | 前端 | 主要实体 |
|---|---|---|---|
| `domiegg/springboot-vue3650` 海滨学院班级回忆录 | Spring Boot 2.2.2 + MyBatis-Plus 2.3 + Shiro | Vue2 + ElementUI（后台）/ Layui + jQuery（前台） | 班级信息、班委、加入班级、班级相册、活动信息、新闻信息、捐赠、论坛 |
| `domie06/springboot-vue3000` 高校班级同学录 | Spring Boot 2.x + MyBatis-Plus | 同上 | 班级校友、个人信息、班级相册、班级统计、通知、问卷 |

**结论**：两者均为毕设级脚手架代码——实体扁平（仅 id + 业务字段 + addtime）、无 DTO/VO 分层、无软删除、无审核流、字段全拼音缩写命名、前台仍为 Layui + jQuery。
因此本项目的做法是：**吸收其功能模块划分与业务字段，架构层完全重做**（Spring Boot 3 + 三层架构 + DTO/VO 隔离 + 逻辑删除 + 审核状态机 + 规范 RESTful）。

## 二、本项目的表设计（10 张表）

统一约定：
- 库名 `schoolmate_book`，字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`。
- 主键 `id BIGINT AUTO_INCREMENT`。
- 公共字段：`create_time`、`update_time`、`deleted TINYINT(1) DEFAULT 0`（MyBatis-Plus 逻辑删除）。
- **不使用物理外键**（互联网惯例，与逻辑删除冲突），关系由索引 + 应用层保证。
- 枚举字段用 `VARCHAR` 存储，应用层用 `enum` 约束，避免 tinyint 被驱动误转 boolean。

### 1. user（用户账号）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) | 登录账号，唯一 |
| password | VARCHAR(100) | BCrypt 密文 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| role | VARCHAR(20) | USER / ADMIN |
| status | VARCHAR(20) | NORMAL / DISABLED |

索引：`uk_username(username)`

### 2. sys_role / sys_user_role（RBAC 预留）
简化实现阶段仅使用 `user.role` 字段；保留两张表用于后续平滑升级为完整 RBAC（角色/权限/菜单）。
- sys_role：id、role_code（唯一）、role_name、description
- sys_user_role：id、user_id、role_id（联合唯一 `uk_user_role`）

### 3. class_info（班级）
| 字段 | 类型 | 说明 |
|---|---|---|
| class_name | VARCHAR(100) | 班级名称 |
| grade | VARCHAR(20) | 年级 |
| major | VARCHAR(100) | 专业 |
| description | VARCHAR(500) | 班级简介 |
| invite_code | VARCHAR(20) | 邀请码，唯一 |
| owner_id | BIGINT | 创建人（班长/管理员） |

索引：`uk_invite_code(invite_code)`、`idx_owner(owner_id)`

### 4. class_member（班级成员）
class_id、user_id、member_role（OWNER/MEMBER）、join_time。
索引：`uk_class_user(class_id, user_id)` 防止重复加入、`idx_user(user_id)`

### 5. user_profile（同学个人主页资料，1:1）
user_id（唯一）、real_name、student_no、gender、birthday、hometown、contact、motto、enrollment_year、current_city。
索引：`uk_user_id(user_id)`

### 6. message（班级留言板）
class_id、user_id、content、audit_status（PENDING/PASSED/REJECTED）。
索引：`idx_class_create(class_id, create_time)` 覆盖列表倒序查询

### 7. album（相册）/ 8. photo（照片）
- album：class_id、user_id、name、cover_url、description
- photo：album_id、user_id、url、description、audit_status
- 索引：`idx_class(class_id)`、`idx_album(album_id)`

### 9. moment（班级动态 / 时间轴）
class_id、user_id、content、image_urls（JSON 数组）、audit_status。
索引：`idx_class_create(class_id, create_time)`

### 10. comment（动态评论）
moment_id、user_id、content、parent_id（可空，预留二级回复）。
索引：`idx_moment(moment_id)`

## 三、审核状态机

内容类表（message / photo / moment）统一使用 `audit_status`：

```
用户发布 → PENDING ──管理员通过──▶ PASSED（公开展示）
                    └──管理员驳回──▶ REJECTED（仅自己可见）
```

默认策略：可配置为「发布即通过（PASSED）」或「需审核（PENDING）」，由 `schoolmate.content.audit-enabled` 配置项控制，便于开发期调试与生产期管控。
