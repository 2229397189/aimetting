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
 * 用户表 {@code t_user} 持久化对象。
 *
 * <p>{@code deleted} 使用 MyBatis-Plus {@code @TableLogic}：
 * 查询自动追加 {@code deleted = 0}，删除自动转为 UPDATE 置位。
 */
@Data
@TableName("t_user")
public class UserDO {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一。 */
    private String username;

    /** BCrypt(10) 密码摘要。 */
    private String passwordHash;

    /** 邮箱。 */
    private String email;

    /** 昵称。 */
    private String nickname;

    /** 头像 URL。 */
    private String avatar;

    /** 角色：USER / ADMIN。 */
    private String role;

    /** 状态：1 正常，0 禁用。 */
    private Integer status;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginAt;

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
