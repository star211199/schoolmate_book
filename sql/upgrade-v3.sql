-- =====================================================================
-- schoolmate_book v3 增量脚本（在 v2-init.sql 基础上执行）
-- 内容：通知中心表 notification
-- 说明：时光胶囊复用 v2-init.sql 已建好的 time_capsule 表，无需重复建。
-- =====================================================================
USE schoolmate_book;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 通知中心：聚合社交事件（好友申请/通过、点赞、评论、胶囊到期）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notification` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL                COMMENT '接收人ID',
  `type`         VARCHAR(30)  NOT NULL                COMMENT '类型 FRIEND_REQUEST/FRIEND_ACCEPTED/MOMENT_LIKE/MOMENT_COMMENT/CAPSULE_OPENED',
  `title`        VARCHAR(100) NOT NULL                COMMENT '标题，如「张三 赞了你的动态」',
  `content`      VARCHAR(500)  DEFAULT NULL           COMMENT '摘要（评论内容 / 申请留言等，截断存储）',
  `biz_type`     VARCHAR(30)   DEFAULT NULL           COMMENT '关联业务 FRIEND/MOMENT/CAPSULE（前端决定跳转目标）',
  `biz_id`       BIGINT        DEFAULT NULL           COMMENT '关联业务ID（requestId/momentId/capsuleId）',
  `from_user_id` BIGINT        DEFAULT NULL           COMMENT '触发人ID，系统通知为 NULL',
  `read_flag`    TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '已读 0否 1是',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `read_flag`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知中心';

SET FOREIGN_KEY_CHECKS = 1;
