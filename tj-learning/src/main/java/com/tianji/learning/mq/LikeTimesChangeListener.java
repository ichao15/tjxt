package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/03/29  11:52
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class LikeTimesChangeListener {

    private final IInteractionReplyService replyService;

    /**
     * 监听点赞的Listener
     * @param dto
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "qa.liked.times.queue", durable = "true"),
                    exchange = @Exchange(name = LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
                    key = QA_LIKED_TIMES_KEY
            )
    )
    public void listenReplyLikedTimesChange(List<LikedTimesDTO> dtoList) {
        if(CollUtils.isEmpty(dtoList)){
            return;
        }

        List<InteractionReply> list = new ArrayList<>(dtoList.size());
        for (LikedTimesDTO dto : dtoList) {
            InteractionReply r = new InteractionReply();
            r.setId(dto.getBizId());
            r.setLikedTimes(dto.getLikedTimes());
            list.add(r);
        }

        replyService.updateBatchById(list);
    }
}
