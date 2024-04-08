package com.tianji.learning.handler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/07  23:42
 */

@Component
@Slf4j
public class TestHandler {

    @XxlJob("helloJob")
    public void helloJob(){
        log.info("hello....");
    }
}
