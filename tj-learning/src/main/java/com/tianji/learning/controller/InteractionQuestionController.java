package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-03-28
 */
@RestController
@RequestMapping("/questions")
@Api(tags = "问题相关的接口")
@RequiredArgsConstructor
public class InteractionQuestionController {


    private final IInteractionQuestionService questionService;

    /**
     * 新增问题
     * @param questionDTO
     */
    @PostMapping
    @ApiOperation("新增问题")
    public void saveQuestion(@RequestBody @Validated QuestionFormDTO questionDTO){
        questionService.saveQuestion(questionDTO);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询问题(用户端)")
    public PageDTO<QuestionVO> queryQuestionPage(@Validated QuestionPageQuery pageQuery){
        return questionService.queryQuestionPage(pageQuery);
    }

    /**
     * 根据id查询问题详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询问题详情")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id){
        return questionService.queryQuestionById(id);
    }
}
