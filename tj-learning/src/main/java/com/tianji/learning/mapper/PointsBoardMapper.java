package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author xyc
 * @since 2024-04-02
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    @Insert("CREATE TABLE `${tableName}`\n" +
            "(\n" +
            "\t`id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '榜单id',\n" +
            "\t`user_id` BIGINT NOT NULL COMMENT '学生id',\n" +
            "\t`points`  INT    NOT NULL COMMENT '积分值',\n" +
            "\tPRIMARY KEY (`id`) USING BTREE,\n" +
            "\tINDEX `idx_user_id` (`user_id`) USING BTREE\n" +
            ")\n" +
            "\tCOMMENT ='学霸天梯榜'\n" +
            "\tCOLLATE = 'utf8mb4_0900_ai_ci'\n" +
            "\tENGINE = InnoDB\n" +
            "\tROW_FORMAT = DYNAMIC")
    void createPointsBoardTableBySeason(@Param("tableName") String tabName);
}
