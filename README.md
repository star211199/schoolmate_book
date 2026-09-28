# 大学同学录（schoolmate_book）

一个前后端分离的「大学同学录」全栈项目：记录班级成员、留言、相册与班级动态，支持后台用户管理与内容审核。

后端采用 **Spring Boot 3 + MyBatis-Plus + JWT** 的严格三层架构与 RESTful API；前端采用 **Vue 3 + Vite + Pinia + Element Plus**。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3.5、MyBatis-Plus 3.5.7、JWT(jjwt 0.12.6)、BCrypt、Knife4j(OpenAPI3)、Hutool、Lombok |
| 前端 | Vue 3.5、Vite 5、Vue Router 4、Pinia、Element Plus 2.8、Axios |
| 数据库 | MySQL 8.0（utf8mb4），11 张表 |
| 运行环境 | JDK 17、Maven 3.9+、Node 18+ |

架构要点：

- **三层架构**：Controller（参数校验、鉴权入口）→ Service（业务逻辑、事务、权限校验）→ Mapper（数据访问），DTO 入参 / VO 出参，实体不对外暴露。
- **统一契约**：`{code, message, data}` 响应体 + 全局异常处理 + 统一错误码；分页统一 `PageResult`。
- **安全**：BCrypt 加盐存储密码；JWT 无状态鉴权（拦截器 + ThreadLocal 用户上下文，请求结束清理）；越权操作统一返回 40300。
- **可维护性**：逻辑删除、公共字段自动填充、审核状态机（PENDING → PASSED / REJECTED）。

---

## 二、功能模块

1. **认证**：注册、登录（JWT）、当前用户
2. **用户**：个人主页资料（学号/生日/籍贯/现居/签名）、头像上传、修改密码、手机号脱敏
3. **班级**：创建班级（生成邀请码）、邀请码加入、成员管理、班级信息维护
4. **留言板**：班级留言发布与分页
5. **相册**：相册创建、照片上传（白名单 + 10MB 限制）、封面自动回填
6. **班级动态（时间轴）**：发布动态、评论（预留二级回复）
7. **后台管理**：用户启停、内容审核、数据概览

---

## 三、快速开始

### 1. 数据库

```bash
mysql -uroot -p1234 < sql/init.sql
```

脚本会创建数据库 `schoolmate_book`、11 张表，并插入种子数据：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `zhangsan` | `123456` | 普通用户 |

示例班级邀请码：`CLASS01`。

### 2. 启动后端

```bash
cd schoolmate-server
mvn spring-boot:run
```

- 服务地址：http://localhost:8080/api
- 接口文档：http://localhost:8080/api/doc.html

> 端口已在 `pom.xml` 的 `spring-boot-maven-plugin` 中通过
> `<jvmArguments>-Dserver.port=8080</jvmArguments>` 固定（JVM 系统属性优先级高于操作系统
> 环境变量），因此环境中若存在 `SERVER__PORT` 也不会被覆盖，无需在命令行追加参数。
>
> 注意：在 **PowerShell** 中执行 `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`
> 会被拆词，报 `Unknown lifecycle phase ".run.arguments=..."`，请勿使用这种写法。

### 3. 启动前端

```bash
cd schoolmate-web
npm install
npm run dev
```

访问 http://localhost:5173 （已配置 Vite 代理 `/api` → `http://localhost:8080`）。

> 已启用 `strictPort: true`：若 5173 被占用会直接报错而不是漂移到其他端口。
> 此时先结束占用进程（或关闭多余的 dev server 实例）再启动。

---

## 四、目录结构

```
schoolmate_book/
├── schoolmate-server/                 # 后端（Spring Boot 3）
│   └── src/main/java/com/schoolmate/
│       ├── common/                    # Result / ResultCode / PageResult
│       ├── config/                    # MVC、MyBatis-Plus、Knife4j、安全
│       ├── interceptor/               # JWT 拦截器
│       ├── context/                   # 用户上下文（ThreadLocal）
│       ├── controller/                # 控制器层（含 admin 子包）
│       ├── service/ + service/impl/   # 服务层（接口 + 实现）
│       ├── mapper/                    # 数据访问层
│       ├── entity/ dto/ vo/           # 实体 / 入参 / 出参
│       ├── exception/ utils/          # 异常体系 / 工具类
│       └── resources/                 # application.yml 等配置
├── schoolmate-web/                    # 前端（Vue 3）
│   └── src/
│       ├── api/                       # 按资源拆分的接口定义
│       ├── router/ stores/ utils/     # 路由守卫 / Pinia / Axios 封装
│       └── views/                     # 页面（auth / class / profile / admin）
├── sql/init.sql                       # 建库建表 + 种子数据
└── docs/                              # 数据库设计、接口契约
```

---

## 五、文档

- [docs/db-design.md](docs/db-design.md)：参考项目分析结论、表结构设计与审核状态机
- [docs/api.md](docs/api.md)：接口契约速查（也可直接用 Knife4j 在线调试）

---

## 六、开发约定

- 提交信息遵循 Conventional Commits（`feat(server): xxx`、`fix(web): xxx`）
- 每个模块端到端闭环（建表 → entity → mapper → service → controller → 文档自测）后再进入下一个
- 接口设计遵循「契约先行」：先定 DTO/VO 与错误码，再写实现
