package com.aimeeting.interview.auth.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户资料表 {@code t_user_profile} 持久化对象（与 t_user 一对一）。
 */
@Data
@TableName("t_user_profile")
public class UserProfileDO {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 t_user.id，唯一。 */
    private Long userId;

    /** 目标岗位。 */
    private String targetPosition;

    /** 工作年限。 */
    private Integer workYears;

    /** 自我介绍。 */
    private String intro;

    /** 手机号。 */
    private String phone;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;
}
