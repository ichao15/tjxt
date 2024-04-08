package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.PointsMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/02  8:42
 */


@Component
@Slf4j
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService pointsRecordService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "sign.points.queue", durable = "true"),
                    exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
                    key = MqConstants.Key.SIGN_IN
            )
    )
    public void listenSignMessage(PointsMessage message) {
        if (message == null) {
            return;
        }
        pointsRecordService.addPointRecord(message, PointsRecordType.SIGN);
    }

    /*   @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "learning.points.queue", durable = "true"),
                    exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
                    key = MqConstants.Key.LEARN_SECTION
            )
    )
    public void listenSignMessage(PointsMessage message) {
        if (message == null) {
            return;
        }
        pointsRecordService.addPointRecord(message, PointsRecordType.LEARNING);
    }*/
}
