package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.PointsMessage;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface IPointsRecordService extends IService<PointsRecord> {

    /**
     * 保存积分记录
     * @param message
     * @param sign
     */
    void addPointRecord(PointsMessage message, PointsRecordType sign);

    /**
     * 查询今日积分情况
     * @return
     */
    List<PointsStatisticsVO> queryMyPointToday();
}
