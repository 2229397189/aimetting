package com.aimeeting.interview.admin.dao.mapper;

import com.aimeeting.interview.admin.dao.entity.TrendRow;
import com.aimeeting.interview.admin.dao.entity.UserReadDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 管理端只读 Mapper（跨 t_user / t_interview_session / t_interview_report / t_session_question）。
 *
 * <p>只读不写（除用户启停）；与 M1/M4/M6 包解耦。
 */
@Mapper
public interface AdminMapper {

    @Select("<script>"
            + "SELECT COUNT(*) FROM t_user WHERE deleted = 0 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (username LIKE CONCAT('%', #{keyword}, '%') "
            + " OR nickname LIKE CONCAT('%', #{keyword}, '%') "
            + " OR email LIKE CONCAT('%', #{keyword}, '%'))</if>"
            + "</script>")
    long countUsers(@Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT id, username, nickname, email, role, status, create_time AS createTime, last_login_at AS lastLoginAt "
            + "FROM t_user WHERE deleted = 0 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (username LIKE CONCAT('%', #{keyword}, '%') "
            + " OR nickname LIKE CONCAT('%', #{keyword}, '%') "
            + " OR email LIKE CONCAT('%', #{keyword}, '%'))</if>"
            + " ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}"
            + "</script>")
    List<UserReadDO> listUsers(@Param("keyword") String keyword,
                              @Param("offset") long offset,
                              @Param("limit") long limit);

    @Update("UPDATE t_user SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM t_user WHERE deleted = 0")
    long countAllUsers();

    @Select("SELECT COUNT(*) FROM t_interview_session WHERE deleted = 0")
    long countAllSessions();

    @Select("SELECT COUNT(*) FROM t_interview_session WHERE deleted = 0 AND status = 'COMPLETED'")
    long countCompletedSessions();

    @Select("SELECT AVG(total_score) FROM t_interview_report WHERE deleted = 0")
    java.math.BigDecimal avgReportScore();

    @Select("SELECT COUNT(*) FROM t_session_question WHERE deleted = 0")
    long countQuestions();

    @Select("SELECT CAST(DATE(create_time) AS CHAR) AS day, COUNT(*) AS cnt, AVG(score) AS avg_score "
            + "FROM t_interview_session "
            + "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) "
            + "GROUP BY DATE(create_time) ORDER BY day")
    List<TrendRow> sessionTrend(@Param("days") int days);

    @Select("SELECT directions FROM t_interview_session "
            + "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    List<String> recentDirections(@Param("days") int days);
}
