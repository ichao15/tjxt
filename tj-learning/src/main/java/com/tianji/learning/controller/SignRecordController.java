package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 签到打卡 前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */

@Api(tags = "签到相关接口")
@RestController
@RequestMapping("sign-records")
@RequiredArgsConstructor
public class SignRecordController {


    private final ISignRecordService signRecordService;

    @PostMapping
    @ApiOperation("签到")
    public SignResultVO addSignRecord() {
        return signRecordService.addSignRecord();
    }

    @GetMapping
    @ApiOperation("查询签到记录")
    public Byte[] querySignRecords(){
        return signRecordService.querySignRecords();
    }
}