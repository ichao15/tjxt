package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.ObjectUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.task.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-03-26
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService lessonService;
    private final CourseClient courseClient;
    private final LearningRecordDelayTaskHandler taskHandler;

    @Override
    public LearningLessonDTO queryByCourseId(Long courseId) {
        //1. 根据courseId和userId查询出课表
        LearningLesson lesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .one();
        if (ObjectUtils.isEmpty(lesson)) {
            return null;
        }
        //2. 根据课表id查询出学习记录
        List<LearningRecord> recordList = lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        if (CollUtils.isEmpty(recordList)) {
            return null;
        }
        //3. 封装LearningLessonDTO
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        List<LearningRecordDTO> records = BeanUtils.copyList(recordList, LearningRecordDTO.class);
        dto.setRecords(records);
        return dto;
    }

    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO dto) {
        //1.获得当前用户
        Long userId = UserContext.getUser();
        //2.处理学习记录
        boolean finished = false;
        //2.1 判断提交的类型
        if (dto.getSectionType().intValue() == SectionType.EXAM.getValue()) {
            //2.2 处理考试
            finished = handleExam(userId, dto);
        } else {
            //2.3 处理视频
            finished = handleVideo(userId, dto);
        }
        // 如果没有小结学完, 课表数据不应该在handleLesson()里面更新, 因为已经在延迟任务里面更新了
        if (!finished) {
            return;
        }

        //3.处理课表
        handleLesson(dto);
    }

    //处理课表
    private void handleLesson(LearningRecordFormDTO dto) {
        //1. 根据课表id查询课表
        LearningLesson old = lessonService.getById(dto.getLessonId());
        if (old == null) {
            throw new DbException("课表信息不存在");
        }
        //2. 判断是否全部学完；如果有小节学完了，才去判断是否全部学完；
        boolean allLearned = false;

        //2.1 根据课程id查询出课程信息 获得课程的总小节数
        CourseFullInfoDTO course = courseClient.getCourseInfoById(old.getCourseId(), false, false);
        if (course == null) {
            throw new DbException("课程信息不存在");
        }
        int sectionNum = course.getSectionNum();
        //2.2 判断 已经学习的小节数+1>=课程的总小节数
        allLearned = old.getLearnedSections() + 1 >= sectionNum;

        //3. 更新课表(课表学习小节数,课表状态,课表最近学习小节,课程最新学习时间)
        boolean success = lessonService.lambdaUpdate()
                .set(LearningLesson::getLatestSectionId, dto.getSectionId()) //最近学习小节
                .set(LearningLesson::getLatestLearnTime, LocalDateTime.now()) //最新学习时间
                .set(LearningLesson::getLearnedSections, old.getLearnedSections() + 1) //课表学习小节数
                .set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED) //如果全部学完了 更新课表状态为已经学完
                .set(old.getStatus() == LessonStatus.NOT_BEGIN, LearningLesson::getStatus, LessonStatus.LEARNING) //如果整个课程是第一次学,  更新课表状态为学习中
                .eq(LearningLesson::getId, old.getId())
                .update();
        if (!success) {
            throw new DbException("课表更新失败");
        }
    }

    //处理考试
    private boolean handleExam(Long userId, LearningRecordFormDTO dto) {
        //1. 新增记录
        LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
        //2. 填充用户id， finished， 完成时间
        record.setUserId(userId);
        record.setFinished(true);
        record.setFinishTime(dto.getCommitTime());
        boolean success = save(record);
        if (!success) {
            throw new DbException("考试提交失败");
        }

        return true;
    }

    //处理视频
    private boolean handleVideo(Long userId, LearningRecordFormDTO dto) {
        //1. 根据课表id和小节id查询学习记录
        LearningRecord old = queryLearningRecord(dto);

        //2. 不存在, 新增学习记录
        if (old == null) {
            LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
            record.setUserId(userId);

            boolean success = save(record);
            if (!success) {
                throw new DbException("视频进度提交失败");
            }
            return false;
        }

        //3. 判断是否第一次学完
        boolean finished = !old.getFinished() && dto.getMoment() * 2 > dto.getDuration();

        //4. 如果不是第一次学完
        if (!finished) {
            LearningRecord record = new LearningRecord();
            record.setLessonId(dto.getLessonId());
            record.setSectionId(dto.getSectionId());
            record.setMoment(dto.getMoment());  //必须是前端新提交的学习moment, 不能是old(数据库)的
            record.setId(old.getId());
            record.setFinished(old.getFinished());

            //4.1 存到Redis, 提交延迟任务
            taskHandler.addLearningRecordTask(record);
            //4.2结束
            return false;
        }

        //5. 更新学习记录
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, dto.getMoment())
                .set(finished, LearningRecord::getFinished, true) //如果finished是true, 更新是否学习完
                .set(finished, LearningRecord::getFinishTime, LocalDateTime.now()) //如果finished是true, 更新是否学习完
                .eq(LearningRecord::getId, old.getId())
                .update();

        if (!success) {
            throw new DbException("视频进度更新失败");
        }

        //6.清除redis的数据
        taskHandler.cleanRecordCache(dto.getLessonId(), dto.getSectionId());

        return finished;
    }

    //先从Redis里面获得, 有直接返回; 没有从mysql里面查询, 再存到Redis
    private LearningRecord queryLearningRecord(LearningRecordFormDTO dto) {
        //1先从Redis里面获得, 有直接返回
        LearningRecord record = taskHandler.readRecordCache(dto.getLessonId(), dto.getSectionId());
        if (record != null) {
            return record;
        }
        //2.没有从mysql里面查询,如果有, 再存到Redis  50分钟 25分钟 数据库学完的, redis清除了  25分钟 15s的   25分钟 30s的  1min
        record = lambdaQuery()
                .eq(LearningRecord::getLessonId, dto.getLessonId())
                .eq(LearningRecord::getSectionId, dto.getSectionId())
                .one();
        if (record != null) {
            taskHandler.writeRecordCache(record);
        }

        return record;
    }
}
