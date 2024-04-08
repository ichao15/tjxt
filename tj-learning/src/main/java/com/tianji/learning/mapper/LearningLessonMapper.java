package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 学生课程表 Mapper 接口
 * </p>
 *
 * @author xyc
 * @since 2024-03-25
 */
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {

    @Select("SELECT * FROM learning_lesson WHERE user_id = #{userId} AND status=1 ORDER BY latest_learn_time DESC limit 0,1")
    LearningLesson queryCurrent(@Param("userId") Long userId);

    //当前用户本周计划学习小节总数
    @Select("SELECT SUM(week_freq) FROM  learning_lesson WHERE  user_id = #{userId} AND plan_status =1 AND status IN(0,1)")
    int queryWeekTotalPlan(Long userId);
}
