-- ============================================================
-- 大学同学录 v2 扩展脚本：即时通讯（好友 / 群聊 / 会话 / 消息）
-- 依赖：必须先执行 init.sql
-- 用法：mysql -uroot -p1234 schoolmate_book < sql/v2-init.sql
-- ============================================================

USE `schoolmate_book`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. friend_request 好友申请
-- ----------------------------
DROP TABLE IF EXISTS `friend_request`;
CREATE TABLE `friend_request` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` BIGINT       NOT NULL                COMMENT '申请人ID',
  `to_user_id`   BIGINT       NOT NULL                COMMENT '被申请人ID',
  `message`      VARCHAR(200) DEFAULT NULL            COMMENT '验证消息',
  `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                 COMMENT '状态 PENDING待处理/ACCEPTED已同意/REJECTED已拒绝/EXPIRED已过期',
  `handle_time`  DATETIME     DEFAULT NULL            COMMENT '处理时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_to_status` (`to_user_id`, `status`),
  KEY `idx_from` (`from_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好友申请';

-- ----------------------------
-- 2. friendship 好友关系
--    单行存储：约定 user_a_id < user_b_id，配合唯一索引避免双向重复
-- ----------------------------
DROP TABLE IF EXISTS `friendship`;
CREATE TABLE `friendship` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_a_id`  BIGINT      NOT NULL                COMMENT '较小的用户ID',
  `user_b_id`  BIGINT      NOT NULL                COMMENT '较大的用户ID',
  `remark_a`   VARCHAR(50) DEFAULT NULL            COMMENT 'A 给 B 设置的备注名',
  `remark_b`   VARCHAR(50) DEFAULT NULL            COMMENT 'B 给 A 设置的备注名',
  `group_a`    VARCHAR(50) DEFAULT NULL            COMMENT 'A 侧好友分组',
  `group_b`    VARCHAR(50) DEFAULT NULL            COMMENT 'B 侧好友分组',
  `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`user_a_id`, `user_b_id`, `deleted`),
  KEY `idx_a` (`user_a_id`),
  KEY `idx_b` (`user_b_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好友关系';

-- ----------------------------
-- 3. chat_group 群聊
--    group_type=CLASS 时一个班级只能有一个班级群（由 uk_biz 保证）
-- ----------------------------
DROP TABLE IF EXISTS `chat_group`;
CREATE TABLE `chat_group` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_name`   VARCHAR(100) NOT NULL                COMMENT '群名称',
  `avatar`       VARCHAR(255) DEFAULT NULL            COMMENT '群头像URL',
  `group_type`   VARCHAR(20)  NOT NULL DEFAULT 'CUSTOM'
                 COMMENT '群类型 CLASS班级群/CUSTOM自建群',
  `class_id`     BIGINT       DEFAULT NULL            COMMENT '班级群关联的班级ID',
  `owner_id`     BIGINT       NOT NULL                COMMENT '群主ID',
  `notice`       VARCHAR(500) DEFAULT NULL            COMMENT '群公告',
  `member_count` INT          NOT NULL DEFAULT 1      COMMENT '成员数（冗余，便于展示）',
  `max_member`   INT          NOT NULL DEFAULT 200    COMMENT '成员上限',
  `join_mode`    VARCHAR(20)  NOT NULL DEFAULT 'INVITE'
                 COMMENT '入群方式 INVITE仅邀请/APPROVAL需审核',
  `status`       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/DISMISSED',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`group_type`, `class_id`, `deleted`),
  KEY `idx_owner` (`owner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '群聊';

-- ----------------------------
-- 4. chat_group_member 群成员
-- ----------------------------
DROP TABLE IF EXISTS `chat_group_member`;
CREATE TABLE `chat_group_member` (
  `id`                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id`             BIGINT      NOT NULL                COMMENT '群ID',
  `user_id`              BIGINT      NOT NULL                COMMENT '用户ID',
  `member_role`          VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                         COMMENT '群内角色 OWNER群主/ADMIN管理员/MEMBER成员',
  `group_nickname`       VARCHAR(50) DEFAULT NULL            COMMENT '群昵称',
  `mute_until`           DATETIME    DEFAULT NULL            COMMENT '禁言截止时间，NULL 表示未禁言',
  `last_read_message_id` BIGINT      NOT NULL DEFAULT 0      COMMENT '已读位点：已读到的最大消息ID',
  `join_time`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`              TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`, `deleted`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '群成员';

-- ----------------------------
-- 5. chat_session 会话（私聊与群聊统一）
--    biz_key：私聊 P_{小id}_{大id}，群聊 G_{groupId}
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_key`           VARCHAR(60) NOT NULL                COMMENT '业务唯一键，保证会话不重复创建',
  `session_type`      VARCHAR(20) NOT NULL                COMMENT '会话类型 PRIVATE私聊/GROUP群聊',
  `group_id`          BIGINT      DEFAULT NULL            COMMENT '群聊时的群ID',
  `target_user_id`    BIGINT      DEFAULT NULL            COMMENT '私聊时的对方用户ID',
  `last_message_id`   BIGINT      NOT NULL DEFAULT 0      COMMENT '最后一条消息ID',
  `last_message_time` DATETIME    DEFAULT NULL            COMMENT '最后一条消息时间，用于会话列表排序',
  `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_key` (`biz_key`, `deleted`),
  KEY `idx_group` (`group_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话（私聊与群聊统一）';

-- ----------------------------
-- 6. chat_session_user 用户会话状态
--    置顶/免打扰/已读位点都是"用户私有"的，必须与会话本身分开存
-- ----------------------------
DROP TABLE IF EXISTS `chat_session_user`;
CREATE TABLE `chat_session_user` (
  `id`                   BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`           BIGINT     NOT NULL                COMMENT '会话ID',
  `user_id`              BIGINT     NOT NULL                COMMENT '用户ID',
  `last_read_message_id` BIGINT     NOT NULL DEFAULT 0      COMMENT '已读位点，未读数由此计算',
  `pinned`               TINYINT(1) NOT NULL DEFAULT 0      COMMENT '是否置顶',
  `muted`                TINYINT(1) NOT NULL DEFAULT 0      COMMENT '是否免打扰',
  `create_time`          DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`              TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user` (`session_id`, `user_id`, `deleted`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户会话状态';

-- ----------------------------
-- 7. chat_message 聊天消息（私聊与群聊共用）
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，单调递增，同时用作已读位点比较依据',
  `session_id`    BIGINT        NOT NULL                COMMENT '会话ID',
  `sender_id`     BIGINT        NOT NULL                COMMENT '发送者ID',
  `msg_type`      VARCHAR(20)   NOT NULL DEFAULT 'TEXT'
                  COMMENT '消息类型 TEXT文本/IMAGE图片/FILE文件/SYSTEM系统提示/RECALL已撤回',
  `content`       VARCHAR(2000) DEFAULT NULL            COMMENT '文本内容（已做 XSS 转义）',
  `extra`         VARCHAR(1000) DEFAULT NULL            COMMENT '扩展信息 JSON：图片URL、文件名、被@的userId列表',
  `reply_to_id`   BIGINT        DEFAULT NULL            COMMENT '引用的消息ID',
  `client_msg_id` VARCHAR(64)   DEFAULT NULL            COMMENT '前端生成的幂等ID，用于防重复发送',
  `send_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_id_id` (`session_id`, `id`),
  KEY `idx_sender` (`sender_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天消息';

-- ----------------------------
-- 8. time_capsule 时光胶囊（P3 亮点功能，先建表）
-- ----------------------------
DROP TABLE IF EXISTS `time_capsule`;
CREATE TABLE `time_capsule` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL                COMMENT '写信人ID',
  `class_id`     BIGINT       DEFAULT NULL            COMMENT '所属班级ID',
  `title`        VARCHAR(100) NOT NULL                COMMENT '标题',
  `content`      VARCHAR(2000) NOT NULL               COMMENT '信件内容',
  `open_time`    DATETIME     NOT NULL                COMMENT '开启时间，到点后才能查看',
  `open_type`    VARCHAR(20)  NOT NULL DEFAULT 'SELF'
                 COMMENT '收件类型 SELF写给未来的自己/PUBLIC写给全班',
  `status`       VARCHAR(20)  NOT NULL DEFAULT 'SEALED' COMMENT '状态 SEALED封存中/OPENED已开启',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_open` (`open_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '时光胶囊';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 说明
--  1. 本脚本只建表，不插入种子数据。好友与群聊请通过界面流程产生，
--     以便验证「加好友 → 自动建会话」「建班级 → 自动建班级群」等联动逻辑。
--  2. 若需重置，先 DROP 这些表再重新执行即可，不影响 init.sql 的原有表。
-- ============================================================
