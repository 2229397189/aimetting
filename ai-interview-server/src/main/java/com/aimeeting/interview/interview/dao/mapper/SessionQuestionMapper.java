package com.aimeeting.interview.interview.dao.mapper;

import com.aimeeting.interview.interview.dao.entity.SessionQuestionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会话题目 Mapper。
 */
@Mapper
public interface SessionQuestionMapper extends BaseMapper<SessionQuestionDO> {

    /**
     * 按会话加载全部题目（按题号升序）。
     *
     * @param sessionId 会话 ID
     * @return 题目列表
     */
    @Select("SELECT * FROM t_session_question WHERE deleted = 0 AND session_id = #{sessionId} "
            + "ORDER BY question_no ASC")
    java.util.List<SessionQuestionDO> selectBySession(@Param("sessionId") Long sessionId);
}
