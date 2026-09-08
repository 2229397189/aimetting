package com.aimeeting.interview.interview.dao.mapper;

import com.aimeeting.interview.interview.dao.entity.InterviewSessionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 面试会话 Mapper。
 *
 * <p>列表查询走 MyBatis-Plus {@code LambdaQueryWrapper} + 分页插件，
 * 此处仅补充按用户 / 状态 / 方向过滤的自定义 SQL 与状态统计。
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSessionDO> {

    /**
     * 按用户 + 状态 + 方向过滤（方向为逗号分隔列，按包含匹配）。
     *
     * @param userId   用户 ID
     * @param status   状态（可空）
     * @param direction 方向（可空）
     * @return 会话列表（按 id 倒序）
     */
    @Select("<script>SELECT * FROM t_interview_session WHERE deleted = 0 AND user_id = #{userId} "
            + "<if test='status != null and status != \"\"'> AND status = #{status}</if>"
            + "<if test='direction != null and direction != \"\"'> AND CONCAT(',', directions, ',') "
            + "LIKE CONCAT('%,', #{direction}, ',%')</if>"
            + " ORDER BY id DESC</script>")
    java.util.List<InterviewSessionDO> selectByUser(@Param("userId") Long userId,
                                                    @Param("status") String status,
                                                    @Param("direction") String direction);

    /**
     * 按状态统计会话数（管理端概览用）。
     *
     * @return 分组计数（k=状态，cnt=条数）
     */
    @Select("SELECT status AS k, COUNT(*) AS cnt FROM t_interview_session WHERE deleted = 0 GROUP BY status")
    java.util.List<com.aimeeting.interview.interview.dao.mapper.CountRow> countByStatus();
}
