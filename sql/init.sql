-- ============================================================
-- 大学同学录（schoolmate_book）数据库初始化脚本
-- MySQL 8.0 / utf8mb4
-- 用法： mysql -uroot -p1234 < sql/init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `schoolmate_book`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `schoolmate_book`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. user 用户账号
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码密文',
  `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/DISABLED',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户账号';

-- ----------------------------
-- 2/3. RBAC 预留：角色表 + 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code`   VARCHAR(50) NOT NULL                COMMENT '角色编码',
  `role_name`   VARCHAR(50) NOT NULL                COMMENT '角色名称',
  `description` VARCHAR(200) DEFAULT NULL           COMMENT '角色描述',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表(RBAC预留)';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT NOT NULL                COMMENT '用户ID',
  `role_id`     BIGINT NOT NULL                COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`, `deleted`),
  KEY `idx_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表(RBAC预留)';

-- ----------------------------
-- 4. class_info 班级
-- ----------------------------
DROP TABLE IF EXISTS `class_info`;
CREATE TABLE `class_info` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_name`  VARCHAR(100) NOT NULL                COMMENT '班级名称',
  `grade`       VARCHAR(20)  DEFAULT NULL            COMMENT '年级',
  `major`       VARCHAR(100) DEFAULT NULL            COMMENT '专业',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '班级简介',
  `invite_code` VARCHAR(20)  NOT NULL                COMMENT '邀请码',
  `owner_id`    BIGINT       NOT NULL                COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`, `deleted`),
  KEY `idx_owner` (`owner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级';

-- ----------------------------
-- 5. class_member 班级成员
-- ----------------------------
DROP TABLE IF EXISTS `class_member`;
CREATE TABLE `class_member` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`    BIGINT      NOT NULL                COMMENT '班级ID',
  `user_id`     BIGINT      NOT NULL                COMMENT '用户ID',
  `member_role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '班级内角色 OWNER/MEMBER',
  `join_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_user` (`class_id`, `user_id`, `deleted`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级成员';

-- ----------------------------
-- 6. user_profile 同学个人主页资料（1:1）
-- ----------------------------
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT      NOT NULL                COMMENT '用户ID',
  `real_name`       VARCHAR(50) DEFAULT NULL            COMMENT '真实姓名',
  `student_no`      VARCHAR(50) DEFAULT NULL            COMMENT '学号',
  `gender`          VARCHAR(10) DEFAULT NULL            COMMENT '性别 MALE/FEMALE/UNKNOWN',
  `birthday`        DATE        DEFAULT NULL            COMMENT '生日',
  `hometown`        VARCHAR(100) DEFAULT NULL           COMMENT '籍贯',
  `current_city`    VARCHAR(100) DEFAULT NULL           COMMENT '现居城市',
  `contact`         VARCHAR(100) DEFAULT NULL           COMMENT '联系方式（微信/QQ）',
  `motto`           VARCHAR(200) DEFAULT NULL           COMMENT '个性签名',
  `enrollment_year` VARCHAR(10) DEFAULT NULL            COMMENT '入学年份',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '同学个人主页资料';

-- ----------------------------
-- 7. message 班级留言
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`     BIGINT      NOT NULL                COMMENT '班级ID',
  `user_id`      BIGINT      NOT NULL                COMMENT '留言人ID',
  `content`      VARCHAR(1000) NOT NULL              COMMENT '留言内容',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PASSED' COMMENT '审核 PENDING/PASSED/REJECTED',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_class_create` (`class_id`, `create_time`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级留言';

-- ----------------------------
-- 8. album 相册
-- ----------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`    BIGINT       NOT NULL                COMMENT '班级ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '创建人ID',
  `name`        VARCHAR(100) NOT NULL                COMMENT '相册名称',
  `cover_url`   VARCHAR(255) DEFAULT NULL            COMMENT '封面URL',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '相册描述',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_class` (`class_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '相册';

-- ----------------------------
-- 9. photo 照片
-- ----------------------------
DROP TABLE IF EXISTS `photo`;
CREATE TABLE `photo` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `album_id`     BIGINT       NOT NULL                COMMENT '相册ID',
  `user_id`      BIGINT       NOT NULL                COMMENT '上传者ID',
  `url`          VARCHAR(255) NOT NULL                COMMENT '图片URL',
  `description`  VARCHAR(200) DEFAULT NULL            COMMENT '图片描述',
  `audit_status` VARCHAR(20)  NOT NULL DEFAULT 'PASSED' COMMENT '审核 PENDING/PASSED/REJECTED',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '照片';

-- ----------------------------
-- 10. moment 班级动态（时间轴数据源）
-- ----------------------------
DROP TABLE IF EXISTS `moment`;
CREATE TABLE `moment` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`     BIGINT       NOT NULL                COMMENT '班级ID',
  `user_id`      BIGINT       NOT NULL                COMMENT '发布人ID',
  `content`      VARCHAR(2000) NOT NULL               COMMENT '动态内容',
  `image_urls`   VARCHAR(2000) DEFAULT NULL           COMMENT '图片URL数组(JSON)',
  `audit_status` VARCHAR(20)  NOT NULL DEFAULT 'PASSED' COMMENT '审核 PENDING/PASSED/REJECTED',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_class_create` (`class_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级动态';

-- ----------------------------
-- 11. comment 动态评论
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `moment_id`  BIGINT      NOT NULL                COMMENT '动态ID',
  `user_id`    BIGINT      NOT NULL                COMMENT '评论人ID',
  `content`    VARCHAR(500) NOT NULL               COMMENT '评论内容',
  `parent_id`  BIGINT      DEFAULT NULL            COMMENT '父评论ID（预留二级回复）',
  `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_moment` (`moment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '动态评论';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 种子数据
-- 说明：
--  - admin / 123456：超级管理员（BCrypt 密文）
--  - zhangsan / 123456：普通用户示例
--  - 示例班级邀请码：CLASS01
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`)
VALUES
  (1, 'admin', '$2b$10$dgzvDOHUqJ4l4LIYumcNXO2mX8hOgS2FfSBJACc6iydyh391iLCpa', '超级管理员', 'ADMIN', 'NORMAL'),
  (2, 'zhangsan', '$2b$10$dgzvDOHUqJ4l4LIYumcNXO2mX8hOgS2FfSBJACc6iydyh391iLCpa', '张三', 'USER', 'NORMAL')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`)
VALUES
  (1, 'ADMIN', '超级管理员', '系统全部权限'),
  (2, 'USER', '普通用户', '同学录常规权限')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`)
VALUES (1, 1, 1), (2, 2, 2)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `class_info` (`id`, `class_name`, `grade`, `major`, `description`, `invite_code`, `owner_id`)
VALUES
  (1, '2024级智能科学与技术1班', '2024级', '智能科学与技术', '示例班级：广东技术师范大学', 'CLASS01', 1)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `class_member` (`id`, `class_id`, `user_id`, `member_role`)
VALUES (1, 1, 1, 'OWNER'), (2, 1, 2, 'MEMBER')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `user_profile` (`id`, `user_id`, `real_name`, `student_no`, `gender`, `hometown`, `current_city`, `motto`, `enrollment_year`)
VALUES
  (1, 1, '系统管理员', 'ADMIN001', 'UNKNOWN', '中国广州', '广州', '让回忆有处可寻', '2024'),
  (2, 2, '张三', '2024100001', 'MALE', '广东汕头', '广州', '保持热爱，奔赴山海', '2024')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;
