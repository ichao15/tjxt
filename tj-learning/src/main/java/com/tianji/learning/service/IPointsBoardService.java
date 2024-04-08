package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    /**
     * 根据赛季查询积分排行榜
     * @param query
     * @return
     */
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    /**
     * 根据season创建表
     * @param season
     */
    void createPointsBoardTableBySeason(PointsBoardSeason season);
}
