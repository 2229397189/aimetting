package com.aimeeting.interview.interview.dao.mapper;

import com.aimeeting.interview.interview.dao.entity.SessionAnswerDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 答题记录 Mapper。
 */
@Mapper
public interface SessionAnswerMapper extends BaseMapper<SessionAnswerDO> {

    /**
     * 按会话加载全部答题（按 id 升序）。
     *
     * @param sessionId 会话 ID
     * @return 答题列表
     */
    @Select("SELECT * FROM t_session_answer WHERE deleted = 0 AND session_id = #{sessionId} ORDER BY id ASC")
    java.util.List<SessionAnswerDO> selectBySession(@Param("sessionId") Long sessionId);

    /**
     * 按会话 + 会话题目加载答题（原题 + 追问链）。
     *
     * @param sessionId         会话 ID
     * @param sessionQuestionId 会话题目 ID
     * @return 答题列表
     */
    @Select("SELECT * FROM t_session_answer WHERE deleted = 0 AND session_id = #{sessionId} "
            + "AND session_question_id = #{sessionQuestionId} ORDER BY id ASC")
    java.util.List<SessionAnswerDO> selectByQuestion(@Param("sessionId") Long sessionId,
                                                    @Param("sessionQuestionId") Long sessionQuestionId);
}
