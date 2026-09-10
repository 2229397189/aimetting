package com.aimeeting.interview.question.dao.mapper;

import com.aimeeting.interview.question.dao.entity.QuestionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 题库 Mapper（MyBatis-Plus + 注解 SQL）。
 */
@Mapper
public interface QuestionMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<QuestionDO> {

    @Select("<script>"
            + "SELECT * FROM t_question WHERE deleted = 0 "
            + "<if test='direction != null and direction != \"\"'> AND direction = #{direction}</if>"
            + "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND title LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}"
            + "</script>")
    List<QuestionDO> selectPageList(@Param("direction") String direction,
                                    @Param("difficulty") String difficulty,
                                    @Param("keyword") String keyword,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    @Select("<script>"
            + "SELECT COUNT(*) FROM t_question WHERE deleted = 0 "
            + "<if test='direction != null and direction != \"\"'> AND direction = #{direction}</if>"
            + "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND title LIKE CONCAT('%', #{keyword}, '%')</if>"
            + "</script>")
    long countPageList(@Param("direction") String direction,
                        @Param("difficulty") String difficulty,
                        @Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT * FROM t_question WHERE deleted = 0 AND status = 1 "
            + "<if test='direction != null and direction != \"\"'> AND direction = #{direction}</if>"
            + "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty}</if>"
            + "<if test='excludeIds != null and excludeIds.size() > 0'> AND id NOT IN "
            + "<foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>"
            + " ORDER BY RAND() LIMIT #{limit}"
            + "</script>")
    List<QuestionDO> selectRandom(@Param("direction") String direction,
                                  @Param("difficulty") String difficulty,
                                  @Param("excludeIds") List<Long> excludeIds,
                                  @Param("limit") int limit);

    @Select("SELECT direction AS k, COUNT(*) AS cnt FROM t_question WHERE deleted = 0 AND status = 1 GROUP BY direction")
    List<CountRow> countByDirection();

    @Select("SELECT difficulty AS k, COUNT(*) AS cnt FROM t_question WHERE deleted = 0 AND status = 1 GROUP BY difficulty")
    List<CountRow> countByDifficulty();

    @Select("SELECT title FROM t_question WHERE deleted = 0")
    List<String> selectAllTitles();
}
