package com.aimeeting.interview.auth.dao.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 个人数据概览（U-08）只读聚合查询。
 *
 * <p>跨面试域与简历域做计数/均值聚合，仅读不写。由启动类
 * {@code @MapperScan("com.aimeeting.interview.**.dao.mapper")} 统一扫描注册。
 */
@Mapper
public interface StatsMapper {

    /** 累计面试场次。 */
    @Select("SELECT COUNT(*) FROM t_interview_session WHERE user_id = #{userId} AND deleted = 0")
    Long countSessions(@Param("userId") Long userId);

    /** 已完成场次（COMPLETED）。 */
    @Select("SELECT COUNT(*) FROM t_interview_session WHERE user_id = #{userId} AND status = 'COMPLETED' AND deleted = 0")
    Long countCompletedSessions(@Param("userId") Long userId);

    /** 会话平均分（无有效分时返回 0）。 */
    @Select("SELECT COALESCE(AVG(score), 0) FROM t_interview_session "
            + "WHERE user_id = #{userId} AND score IS NOT NULL AND deleted = 0")
    BigDecimal averageScore(@Param("userId") Long userId);

    /** 累计答题数。 */
    @Select("SELECT COUNT(*) FROM t_session_answer WHERE user_id = #{userId} AND deleted = 0")
    Long countAnswers(@Param("userId") Long userId);

    /** 简历数。 */
    @Select("SELECT COUNT(*) FROM t_resume WHERE user_id = #{userId} AND deleted = 0")
    Long countResumes(@Param("userId") Long userId);

    /** 近期每日场次与平均分趋势。 */
    @Select("SELECT DATE(create_time) AS `day`, COUNT(*) AS cnt, COALESCE(AVG(score), 0) AS avg_score "
            + "FROM t_interview_session "
            + "WHERE user_id = #{userId} AND create_time >= #{start} AND deleted = 0 "
            + "GROUP BY DATE(create_time) ORDER BY `day`")
    List<TrendRow> dailyTrend(@Param("userId") Long userId, @Param("start") LocalDateTime start);

    /** 单日趋势行。 */
    class TrendRow {

        public String day;
        public Long cnt;
        public BigDecimal avgScore;
    }
}
