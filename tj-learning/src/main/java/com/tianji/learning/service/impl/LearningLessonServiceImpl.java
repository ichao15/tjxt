package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-03-25
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final LearningRecordMapper learningRecordMapper;

    @Override
    public void addLesson(OrderBasicDTO dto) {
        // 1.远程调用tj-course微服务，根据课程id查询出课程信息(课程有效期)
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(dto.getCourseIds());
        if (CollUtils.isEmpty(courseList)) {
            throw new BadRequestException("课程不存在");
        }
        // 2.遍历课程信息，封装LearningLesson(用户id，课程id，过期时间)
        List<LearningLesson> lessonList = new ArrayList<>();
        for (CourseSimpleInfoDTO course : courseList) {
            LearningLesson lesson = new LearningLesson()
                    .setCourseId(course.getId())
                    // 此处因为是MQ异步调用，不在同一个线程上，所以要从dto中获取
                    .setUserId(dto.getUserId())
                    // 算出过期时间
                    .setExpireTime(LocalDateTime.now().plusMonths(course.getValidDuration()));
            lessonList.add(lesson);
        }
        // 3.批量保存
        saveBatch(lessonList);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLesson(PageQuery query) {
        // 1. 分页查询出当前用户的课表信息
        Long userId = UserContext.getUser();
        // 根据userId分页查询课表
        Page<LearningLesson> pageResult = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<LearningLesson> lessonList = pageResult.getRecords();
        if (CollUtils.isEmpty(lessonList)) {
            return PageDTO.empty(pageResult);
        }
        // 2. 根据courseId查询出课程信息
        List<Long> cIds = lessonList.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(courseList)) {
            throw new BadRequestException("课程信息不存在");
        }
        Map<Long, CourseSimpleInfoDTO> courseMap = courseList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, course -> course));

        // 3. 遍历课表List,封装LearningLessonVO
        List<LearningLessonVO> voList = new ArrayList<>();
        for (LearningLesson lesson : lessonList) {
            LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
            //封装课程信息
            CourseSimpleInfoDTO course = courseMap.get(lesson.getCourseId());
            if (course!=null){
                vo.setCourseName(course.getName());
                vo.setCourseCoverUrl(course.getCoverUrl());
                vo.setSections(course.getSectionNum());
            }
            voList.add(vo);
        }
        // 封装结果
        return PageDTO.of(pageResult,voList);
    }

    @Override
    public LearningLessonVO queryCurrent() {
        //1.获得当前用户Id
        Long userId = UserContext.getUser();
        //2.查询课表,当前用户正在学习的课程 SELECT * FROM   learning_lesson WHERE user_id =2 AND status  = 1 ORDER BY latest_learn_time  DESC  limit 0,1;

//        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<LearningLesson>()
//                .eq(LearningLesson::getUserId, userId)
//                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
//                .orderByDesc(LearningLesson::getLatestLearnTime)
//                .last("limit 0,1");
//        LearningLesson lesson = this.baseMapper.selectOne(queryWrapper);

        LearningLesson lesson = getBaseMapper().queryCurrent(userId);
        if (ObjectUtils.isEmpty(lesson)) {
            return null;
        }

        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        //3.根据课程id查询出课程信息
        CourseFullInfoDTO course = this.courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (ObjectUtil.isEmpty(course)) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(course.getName());
        vo.setCourseCoverUrl(course.getCoverUrl());
        vo.setSections(course.getSectionNum());

        //4.统计课程中的课程
        Integer courseAmount = lambdaQuery().eq(LearningLesson::getUserId, userId).count();
        vo.setCourseAmount(courseAmount);

        List<CataSimpleInfoDTO> catalogueList = catalogueClient.batchQueryCatalogue(List.of(lesson.getLatestSectionId()));
        // 有可能没有学习 当数据库只有一条数据时，LatestLearnTime为NULL也会被查询出来
        if (!CollUtils.isEmpty(catalogueList)) {
            CataSimpleInfoDTO cata = catalogueList.get(0);
            vo.setLatestSectionIndex(cata.getCIndex());
            vo.setLatestSectionName(cata.getName());
        }
        return vo;
    }

    @Override
    public LearningLessonVO queryByCourseId(Long courseId) {
        Long userId = UserContext.getUser();
        // 1. 根据用户id和课程id查询出课表LearningLesson
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId)
                .one();
        if (ObjectUtils.isEmpty(lesson)) {
            return null;
        }
        // 2. 根据课程id查询出课程信息
        CourseFullInfoDTO course = this.courseClient.getCourseInfoById(courseId, false, false);
        if (ObjectUtils.isEmpty(course)) {
            throw new BadRequestException("课程不存在");
        }
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        vo.setCourseName(course.getName());
        vo.setCourseCoverUrl(course.getCoverUrl());
        vo.setSections(course.getSectionNum());
        return vo;
    }

    @Override
    public Long isLessonValid(Long courseId) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            return null;
        }
        // 2.查询课程
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            return null;
        }
        return lesson.getId();
    }

    @Override
    public void createLessonPlan(LearningPlanDTO dto) {
        Long userId = UserContext.getUser();
        //1. 根据userId和courseId查询出课表信息
        LearningLesson old = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, dto.getCourseId()).one();

        if(old==null){
            throw new DbException("课表信息不存在");
        }

        //2. 更新weekFreq, plan_status
        LearningLesson lesson = new LearningLesson();
        lesson.setId(old.getId());
        lesson.setWeekFreq(dto.getFreq());
        if(old.getPlanStatus() == PlanStatus.NO_PLAN){
            lesson.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(lesson);
    }

    @Override
    public LearningPlanPageVO queryMyPlan(PageQuery query) {
        LearningPlanPageVO result = new LearningPlanPageVO();

        //1. 获取当前用户
        Long userId = UserContext.getUser();
        //2. 获得本周的起始和结束时间
        LocalDate now = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(now);
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(now);
        //3.当前用户本周计划统计
        //3.1 当前用户本周已经学习小节总数  SELECT COUNT(*) FROM learning_record  WHERE finished = 1 AND finish_time > '2022-10-10 0:00:00' AND finish_time < '2022-12-30 0:00:00' AND user_id  = 2
        int weekFinished = learningRecordMapper.selectCount(
                new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, weekBeginTime)
                .lt(LearningRecord::getFinishTime, weekEndTime)
                .eq(LearningRecord::getUserId, userId)
        );
        result.setWeekFinished(weekFinished);

        //3.2 当前用户本周计划学习小节总数  SELECT SUM(week_freq) FROM  learning_lesson WHERE  user_id =2 AND plan_status =1 AND status IN(0,1)
        int weekTotalPlan = this.baseMapper.queryWeekTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);

        //4.当前用户计划列表
        //4.1. 分页查询当前用户的课表  SELECT * FROM learning_lesson WHERE user_id  =2 AND plan_status =1 AND status IN (0,1) ORDER BY  latest_learn_time DESC limit a,b;
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> lessonList = page.getRecords();
        if(CollUtils.isEmpty(lessonList)){
            return result.emptyPage(page);
        }

        //4.2. 根据courseId查询课程信息
        Set<Long> courseIds = lessonList.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(courseList)){
            throw new DbException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> courseMap = courseList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        //4.3. 查询某个课程本周的学习小节数  SELECT lesson_id,COUNT(*) FROM learning_record  WHERE finished = 1 AND finish_time > '2022-10-10 0:00:00' AND finish_time < '2023-12-30 0:00:00' AND user_id  = 2 GROUP BY lesson_id
        List<IdAndNumDTO> idAndNumList =   learningRecordMapper.countLearnedSections(userId,weekBeginTime,weekEndTime);
        Map<Long, Integer> idAndNumMap = IdAndNumDTO.toMap(idAndNumList);

        //4.4. 遍历课表List, 封装成voList
        List<LearningPlanVO> planVOList = new ArrayList<>();
        for (LearningLesson lesson : lessonList) {
            //每遍历一次 就要封装成一个LearningPlanVO
            LearningPlanVO learningPlanVO = BeanUtils.copyBean(lesson, LearningPlanVO.class);
            //封装课程信息
            CourseSimpleInfoDTO course = courseMap.get(lesson.getCourseId());
            if(course != null){
                learningPlanVO.setCourseName(course.getName());
                learningPlanVO.setSections(course.getSectionNum());
            }
            //封装这门课程本周学习的小节数
            learningPlanVO.setWeekLearnedSections(idAndNumMap.getOrDefault(lesson.getId(),0));
            planVOList.add(learningPlanVO);
        }
        return result.pageInfo(page.getTotal(),page.getPages(),planVOList);
    }
}
