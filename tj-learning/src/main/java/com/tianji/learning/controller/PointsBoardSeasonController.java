package com.tianji.learning.controller;


import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class PointsBoardSeasonController {

    private final IPointsBoardSeasonService seasonService;

    @GetMapping("/seasons/list")
    public List<PointsBoardSeasonVO> queryPointsBoardSeasons() {
        // 1.获取时间
        LocalDateTime now = LocalDateTime.now();
        // 2.查询赛季列表，必须是当前赛季之前的（开始时间小于等于当前时间）
        List<PointsBoardSeason> list =  seasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, now).list();
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 3.返回VO
        return BeanUtils.copyToList(list, PointsBoardSeasonVO.class);
    }

}
