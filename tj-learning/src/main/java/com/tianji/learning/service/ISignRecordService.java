package com.tianji.learning.service;


import com.tianji.learning.domain.vo.SignResultVO;

/**
 * <p>
 * 签到打卡 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface ISignRecordService {

    /**
     * 签到
     * @return
     */
    SignResultVO addSignRecord();

    /**
     * 查询签到记录
     * @return
     */
    Byte[] querySignRecords();
}
