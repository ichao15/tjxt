package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author xyc
 * @since 2024-03-29
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或者取消点赞
     * @param dto
     */
    void addLikeRecord(LikeRecordFormDTO dto);

    /**
     * 根据用户id,业务类型和ids查询点赞的状态
     * @param bizType
     * @param bizIds
     * @return
     */
    Set<Long> queryListByUserIdAndIds(String bizType, List<Long> bizIds);

    /**
     * 批量读取点赞总数,并且通过MQ通知
     * @param bizType
     * @param maxBizSize
     */
    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}
