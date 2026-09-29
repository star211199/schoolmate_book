-- =====================================================================
-- schoolmate_book 功能扩展升级脚本（在 init.sql 基础上执行）
-- 内容：个人资料扩展字段 / 班级毕业日期 / 点赞表 / 丰富种子数据
-- =====================================================================
USE schoolmate_book;

-- 1. 个人资料扩展：星座、MBTI、兴趣爱好、技能标签、毕业寄语、社交链接、封面图
ALTER TABLE user_profile
  ADD COLUMN constellation      VARCHAR(20)   NULL COMMENT '星座（根据生日自动计算）'      AFTER birthday,
  ADD COLUMN mbti               VARCHAR(8)    NULL COMMENT 'MBTI 人格类型'                 AFTER constellation,
  ADD COLUMN hobbies            VARCHAR(500)  NULL COMMENT '兴趣爱好（JSON 数组）'          AFTER mbti,
  ADD COLUMN skills             VARCHAR(500)  NULL COMMENT '技能标签（JSON 数组）'          AFTER hobbies,
  ADD COLUMN graduation_message VARCHAR(500)  NULL COMMENT '毕业寄语'                       AFTER skills,
  ADD COLUMN social_links       VARCHAR(1000) NULL COMMENT '社交链接（JSON 对象）'          AFTER graduation_message,
  ADD COLUMN cover_image        VARCHAR(255)  NULL COMMENT '个人主页封面图 URL'             AFTER social_links;

-- 2. 班级增加毕业日期（用于毕业倒计时）
ALTER TABLE class_info
  ADD COLUMN graduation_date DATE NULL COMMENT '毕业日期' AFTER grade;

-- 3. 动态点赞表
CREATE TABLE IF NOT EXISTS moment_like (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  moment_id   BIGINT       NOT NULL COMMENT '动态 ID',
  user_id     BIGINT       NOT NULL COMMENT '点赞用户 ID',
  create_time DATETIME     NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_moment_user (moment_id, user_id),
  KEY idx_moment_id (moment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '动态点赞';

-- 4. 丰富种子数据：示例班级毕业日期
UPDATE class_info SET graduation_date = '2028-06-30' WHERE id = 1;

-- 5. 丰富种子用户资料（含二次元头像、标签、寄语）
-- 注：头像存于 user.avatar；个性签名为 user_profile.motto
UPDATE user_profile SET
  gender = '女',
  birthday = '2006-03-15',
  constellation = '双鱼座',
  mbti = 'INFP',
  hometown = '广东广州',
  current_city = '广州',
  hobbies = '["摄影","二次元","钢琴","手账"]',
  skills = '["Java","Spring Boot","Vue3","MySQL"]',
  motto = '愿我们前程似锦，归来仍是少年',
  graduation_message = '四年的时光像樱花一样落下，感谢遇见的每一个人。未来的路，我们各自发光，顶峰相见！',
  social_links = '{"qq":"2157779530","wechat":"albot_2024","github":"star211199"}',
  cover_image = '/images/banner-graduation.webp'
WHERE user_id = 1;

UPDATE user_profile SET
  gender = '男',
  birthday = '2006-08-20',
  constellation = '狮子座',
  mbti = 'ENTP',
  hometown = '广东深圳',
  current_city = '广州',
  hobbies = '["篮球","游戏","说唱","剪辑"]',
  skills = '["Python","AI","剪辑","摄影"]',
  motto = '代码写得好，头发掉得少',
  graduation_message = '兄弟们在网吧五连坐的日子，这辈子都忘不了。祝大家前程似锦！',
  social_links = '{"qq":"123456789","wechat":"zhangsan_nb"}',
  cover_image = '/images/bg-campus.webp'
WHERE user_id = 2;

-- user.avatar 同步（成员列表等场景直接读 user 表）
UPDATE user SET avatar = '/images/avatar-girl.webp' WHERE id = 1;
UPDATE user SET avatar = '/images/avatar-boy.webp' WHERE id = 2;
