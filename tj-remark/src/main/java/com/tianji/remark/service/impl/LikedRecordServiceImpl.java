package com.tianji.remark.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author xyc
 * @since 2024-03-29
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper mqHelper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        // 1.判断点赞的类型, 点赞, 取消点赞
        boolean success = dto.getLiked() ? like(dto) : unlike(dto);
        // 2.判断是否操作成功
        if (success) {
            // 3.成功,统计当前业务的点赞数量
            String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + dto.getBizType() + ":" + dto.getBizId();
            Long likeTimes = redisTemplate.opsForSet().size(key);
            if (likeTimes == null) {
                return;
            }
            //4.缓存到Redis(ZSet)
            redisTemplate.opsForZSet().add(RedisConstants.LIKES_TIMES_KEY_PREFIX + dto.getBizType(), dto.getBizId().toString(), likeTimes);
        }
    }

    @Override
    public Set<Long> queryListByUserIdAndIds(String bizType, List<Long> bizIds) {
        Set<Long> set = new HashSet<>();

        //1.根据用户id和业务id, 业务类型查询
        Long userId = UserContext.getUser();

        //bizIds:   1      10       9       4
        //objects: true  false   true   false
        //返回值objects: 就是管道里面多条命令(多个操作)的结果的集合
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection conn) throws DataAccessException {
                StringRedisConnection connection = (StringRedisConnection) conn;
                for (Long bizId : bizIds) {
                    String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizType + ":" + bizId;
                    connection.sIsMember(key, userId.toString());
                }
                return null;
            }
        });

        for (int i = 0; i < objects.size(); i++) {
            Boolean result = (Boolean) objects.get(i);
            if (result) {
                set.add(bizIds.get(i));
            }
        }

        //2.返回点赞了的业务id
        return set;
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //1.读取
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType, maxBizSize);
        if(CollUtils.isEmpty(tuples)){
            return;
        }

        List<LikedTimesDTO> likedTimesDTOList = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId = tuple.getValue();
            Double likedTimes = tuple.getScore();
            if (bizId == null || likedTimes == null) {
                continue;
            }
            LikedTimesDTO dto = new LikedTimesDTO();
            dto.setBizId(Long.valueOf(bizId));
            dto.setLikedTimes(likedTimes.intValue());
            likedTimesDTOList.add(dto);
        }
        //2.发送MQ
        mqHelper.send(MqConstants.Exchange.LIKE_RECORD_EXCHANGE,StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType),likedTimesDTOList);
    }

    //点赞
    private boolean like(LikeRecordFormDTO dto) {
        //1.拼接key  likes:set:biz:QA:bizId
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + dto.getBizType() + ":" + dto.getBizId();
        //2.向Set存
        Long userId = UserContext.getUser();
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        if (result != null) {
            return result > 0;
        }
        return false;
    }

    //取消点赞
    private boolean unlike(LikeRecordFormDTO dto) {
        //1.拼接key  likes:set:biz:QA:bizId
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + dto.getBizType() + ":" + dto.getBizId();
        //2.从Set移除
        Long userId = UserContext.getUser();
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        if (result != null) {
            return result > 0;
        }
        return false;
    }
}
