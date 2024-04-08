package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/01  15:04
 */

/**
 * fallbackFactory 推荐：可以捕获异常信息并返回默认降级结果。可以打印堆栈信息。
 * fallback 不推荐:不能捕获异常打印堆栈信息，不利于问题排查。
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("查询remark-service服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> queryListByUserIdAndIds(List<Long> bizIds) {
                return CollUtils.emptySet();
            }
        };
    }
}
