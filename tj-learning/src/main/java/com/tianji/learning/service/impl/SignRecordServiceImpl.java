package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.PointsMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    /**
     * 签到
     *
     * @return
     */
    @Override
    public SignResultVO addSignRecord() {
        //1.签到
        //1.1获得当前登录的用户
        Long userId = UserContext.getUser();

        //1.2获得当前的日期
        LocalDate now = LocalDate.now();
        //1.3拼接key
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + DateUtils.format(now, DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //1.4计算offset
        int offset = now.getDayOfMonth() - 1;
        //1.5保存 SETBIT命令会返回指定偏移量原来储存的值。
        Boolean result = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(result)) {
                throw new BizIllegalException("不允许重复签到！");
        }
        //2.统计连续签到的天数
        int signDays = countSignDays(key, now.getDayOfMonth());
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }

        //3.TODO 积分功能
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,MqConstants.Key.SIGN_IN, PointsMessage.of(userId,rewardPoints+1));

        //4.封装vo,返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    @Override
    public Byte[] querySignRecords() {
        //1.获取当前用户
        Long userId = UserContext.getUser();
        //2.获得当前日期
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        //3.执行bitfield sign:uid:110:202404 get  u29  0, 获得截止到当前日期的签到数(10进制)
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + DateUtils.format(now, DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        List<Long> result = redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();
        //4.循环
        Byte[] bytes = new Byte[dayOfMonth];
        int pos = dayOfMonth - 1;
        while (pos >= 0) {
            bytes[pos] = (byte) (num & 1);
            pos--;
            num >>>= 1;
        }
        return bytes;
    }

    //计算连续签到的天数
    private int countSignDays(String key, int len) {
        //1.执行bitfield
        List<Long> result = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return 0;
        }

        //2.取出结果
        int num = result.get(0).intValue(); //10进度数

        int count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            //4.计算器+1
            count++;
            //5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            num >>>=1; //右移一位
        }
        return count;
    }
}
