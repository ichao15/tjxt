package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-03-26
 */

@RestController
@RequestMapping("/learning-record")
@RequiredArgsConstructor
@Api(tags = "学习记录相关的接口")
public class LearningRecordController {

    private final ILearningRecordService recordService;

    @GetMapping("/course/{courseId}")
    @ApiOperation("根据课程id查询学习记录")
    public LearningLessonDTO queryByCourseId(@PathVariable("courseId") Long courseId){
        return recordService.queryByCourseId(courseId);
    }

    /**
     * 提交学习记录
     * @param dto
     */
    @PostMapping
    @ApiOperation("提交学习记录")
    public void addLearningRecord(@RequestBody LearningRecordFormDTO dto){
        recordService.addLearningRecord(dto);
    }

}
