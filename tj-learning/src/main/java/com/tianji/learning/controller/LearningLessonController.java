package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-03-25
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课表相关的接口")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLesson(@Validated PageQuery query){
        return lessonService.queryMyLesson(query);
    }

    @GetMapping("/now")
    @ApiOperation("查询当前用户正在学习的课程")
    public LearningLessonVO queryCurrent() {
        return lessonService.queryCurrent();
    }

    @GetMapping("/{courseId}")
    @ApiOperation("根据课程id查询课程状态")
    public LearningLessonVO queryByCourseId(@PathVariable("courseId") Long courseId) {
        return lessonService.queryByCourseId(courseId);
    }

    @ApiOperation("校验当前课程是否已经报名")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(
            @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId){
        return lessonService.isLessonValid(courseId);
    }

    @PostMapping("/plans")
    @ApiOperation("创建我的学习计划")
    public void createLessonPlan(@RequestBody LearningPlanDTO dto){
        lessonService.createLessonPlan(dto);
    }

    @ApiOperation("查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlan(PageQuery pageQuery){
        return lessonService.queryMyPlan(pageQuery);
    }
}
