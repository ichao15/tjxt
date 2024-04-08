package com.tianji.learning.service;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-03-25
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 添加课表信息
     * @param dto
     */
    void addLesson(OrderBasicDTO dto);

    /**
     * 分页查询我的课表
     * @param query
     * @return
     */
    PageDTO<LearningLessonVO> queryMyLesson(PageQuery query);

    /**
     * 查询当前用户正在学习的课程
     * @return
     */
    LearningLessonVO queryCurrent();

    /**
     * 根据课程id查询课程状态
     * @param courseId
     * @return
     */
    LearningLessonVO queryByCourseId(Long courseId);

    /**
     * 校验当前课程是否已经报名
     * @param courseId
     * @return
     */
    Long isLessonValid(Long courseId);

    /**
     * 创建我的学习计划
     * @param dto
     */
    void createLessonPlan(LearningPlanDTO dto);

    /**
     * 查询我的学习计划
     * @param pageQuery
     * @return
     */
    LearningPlanPageVO queryMyPlan(PageQuery pageQuery);
}
