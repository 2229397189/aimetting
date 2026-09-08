package com.aimeeting.interview.resume.dao.mapper;

import com.aimeeting.interview.resume.dao.entity.ResumeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 简历 Mapper（MyBatis-Plus）。
 */
@Mapper
public interface ResumeMapper extends BaseMapper<ResumeDO> {

    /**
     * 将某用户除指定简历外的其余简历置为非默认。
     *
     * @param userId     用户 ID
     * @param excludeId  保持默认的那条简历 ID
     */
    @Update("UPDATE t_resume SET is_default = 0 WHERE user_id = #{userId} AND deleted = 0 AND id != #{excludeId}")
    void resetDefault(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}
