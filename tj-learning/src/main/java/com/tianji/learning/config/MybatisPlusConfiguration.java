package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.bouncycastle.jcajce.provider.digest.SHA1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ichao15
 * @version 1.0
 * @Description
 * @date 2024/04/03  13:34
 */

@Configuration
public class MybatisPlusConfiguration {

    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        Map<String, TableNameHandler> tableMap = new HashMap<>();
        tableMap.put("points_board", new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                return TableInfoContext.getInfo();
            }
        });
        return new DynamicTableNameInnerInterceptor(tableMap);
    }
}
