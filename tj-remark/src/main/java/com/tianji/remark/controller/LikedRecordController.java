package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-03-29
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Api(tags = "点赞业务相关的接口")
public class LikedRecordController {

    private final ILikedRecordService recordService;


    @PostMapping
    @ApiOperation("点赞或者取消点赞")
    public void addLikeRecord(@Validated @RequestBody LikeRecordFormDTO dto){
        recordService.addLikeRecord(dto);
    }

    @GetMapping("/list")
    @ApiOperation("根据用户id,业务类型和ids查询点赞的状态")
    public Set<Long> queryListByUserIdAndIds(@RequestParam("bizType") String bizType , @RequestParam("bizIds") List<Long> bizIds){
        return recordService.queryListByUserIdAndIds(bizType,bizIds);
    }
}
