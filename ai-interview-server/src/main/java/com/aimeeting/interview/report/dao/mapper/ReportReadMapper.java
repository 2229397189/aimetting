package com.aimeeting.interview.report.dao.mapper;

import com.aimeeting.interview.report.dao.entity.SessionAnswerReadDO;
import com.aimeeting.interview.report.dao.entity.SessionMetaReadDO;
import com.aimeeting.interview.report.dao.entity.SessionQuestionReadDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报告聚合只读 Mapper（跨 t_session_question / t_session_answer / t_interview_session）。
 *
 * <p>只读不写；与 M4（interview 包）解耦，避免依赖其尚未完成的实体定义。
 */
@Mapper
public interface ReportReadMapper {

    @Select("SELECT * FROM t_session_question WHERE deleted = 0 AND session_id = #{sessionId} ORDER BY question_no ASC")
    List<SessionQuestionReadDO> listQuestions(@Param("sessionId") Long sessionId);

    @Select("SELECT * FROM t_session_answer WHERE deleted = 0 AND session_id = #{sessionId} ORDER BY id ASC")
    List<SessionAnswerReadDO> listAnswers(@Param("sessionId") Long sessionId);

    @Select("SELECT id, session_no AS sessionNo, directions, difficulty, user_id AS userId "
            + "FROM t_interview_session WHERE deleted = 0 AND id = #{sessionId}")
    SessionMetaReadDO selectSessionMeta(@Param("sessionId") Long sessionId);
}
