package com.schoolmate.vo.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户搜索结果 VO。
 *
 * <p>relation 字段让前端一次请求就能决定按钮该显示「加好友」「等待验证」还是「发消息」，
 * 不需要自己在多个列表之间拼接判断。
 *
 * @author Albot
 */
@Data
public class UserSearchVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "个性签名")
    private String motto;

    @Schema(description = "关系 SELF/NONE/IS_FRIEND/PENDING_SENT/PENDING_RECEIVED")
    private String relation;
}
