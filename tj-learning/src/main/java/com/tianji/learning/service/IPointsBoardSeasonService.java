package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 根据时间查询赛季
     * @param time
     * @return
     */
    PointsBoardSeason querySessionByTime(LocalDateTime time);
}
