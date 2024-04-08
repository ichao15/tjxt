package com.tianji.learning.service.impl;

import com.tianji.common.utils.UserContext;
import com.tianji.learning.service.ILearningLessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/03/25  22:17
 */

@SpringBootTest
public class Test {

    @Autowired
    private ILearningLessonService lessonService;

    @org.junit.jupiter.api.Test
    public void test() {
        UserContext.setUser(2L);
        System.out.println(lessonService.queryCurrent());
    }
}
