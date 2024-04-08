package com.tianji.learning.service.impl;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.message.PointsMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */

@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointRecord(PointsMessage message, PointsRecordType type) {
        //1.判断是否有积分上限
        Integer points = message.getPoints();
        int maxPoints = type.getMaxPoints();

        int realPoints = points;
        //2.有积分上限
        LocalDateTime now = LocalDateTime.now();
        if (maxPoints > 0) {
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            //2.1查询今日已经获得积分 SELECT SUM(points) FROM points_record WHERE user_id  = 2 AND type =2 AND create_time >= '2023-02-14 0:00:00' AND create_time<= '2024-02-14 23:59:59'
            int todayPoint = this.baseMapper.queryUserTodayPoint(message.getUserId(), type, begin, end);

            //2.2是否超过积分上限
            if (todayPoint >= maxPoints) {
                return;
            }
            if (todayPoint + points > maxPoints) {
                realPoints = maxPoints - todayPoint;
            }
        }
        //3.没有超上限或者没有积分上限保存记录
        PointsRecord pointsRecord = new PointsRecord();
        pointsRecord.setUserId(message.getUserId());
        pointsRecord.setPoints(realPoints);
        pointsRecord.setType(type);
        save(pointsRecord);

        //4. 更新总积分到Redis
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,message.getUserId().toString(),realPoints);
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointToday() {
        //1.获得当前用户
        Long userId = UserContext.getUser();
        //2.获得今天的时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        //3.调用Dao查询
        List<PointsRecord> pointsRecordList  = getBaseMapper().queryMyPointToday(userId,begin,end);
        if(CollUtils.isEmpty(pointsRecordList)){
            return CollUtils.emptyList();
        }

        //4.封装VO
        List<PointsStatisticsVO> voList = new ArrayList<>();
        for (PointsRecord pointsRecord : pointsRecordList) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(pointsRecord.getType().getDesc());
            vo.setPoints(pointsRecord.getPoints());
            vo.setMaxPoints(pointsRecord.getType().getMaxPoints());
            voList.add(vo);
        }
        return voList;
    }
}
