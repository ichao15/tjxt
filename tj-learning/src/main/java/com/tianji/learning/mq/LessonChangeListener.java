package com.tianji.learning.mq;

import cn.hutool.core.util.ObjectUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/03/25  14:34
 */

@RequiredArgsConstructor
@Component
@Slf4j
public class LessonChangeListener {

    private final ILearningLessonService lessonService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "learning.lesson.pay.queue", durable = "true"),
                    exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
                    key = MqConstants.Key.ORDER_PAY_KEY
            )
    )
    public void listenLessonPay(OrderBasicDTO dto) {
        // 1. 非空判断
        if (ObjectUtil.isEmpty(dto) || ObjectUtil.hasEmpty(dto.getOrderId(), dto.getCourseIds())) {
            log.error("接收到MQ的消息有误,订单数据为空");
            return;
        }
        // 2. 调用业务
        lessonService.addLesson(dto);
    }
}
