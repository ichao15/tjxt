package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(value = "remark-service", fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {

    /**
     * 根据用户id和ids查询点赞的状态
     * @param bizIds
     * @return
     */
    @GetMapping("/likes/list")
    Set<Long> queryListByUserIdAndIds(@RequestParam("bizIds") List<Long> bizIds);
}
